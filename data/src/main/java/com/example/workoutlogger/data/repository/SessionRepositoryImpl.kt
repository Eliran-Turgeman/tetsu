package com.example.workoutlogger.data.repository

import androidx.room.withTransaction
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.dao.SessionDao
import com.example.workoutlogger.data.db.dao.WorkoutDao
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.WorkoutItemType
import com.example.workoutlogger.data.db.entity.WeightUnit as EntityWeightUnit
import com.example.workoutlogger.data.mapper.toDomain
import com.example.workoutlogger.data.mapper.toEntity
import com.example.workoutlogger.domain.model.PreviousPerformance
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.comparisons.compareBy
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val database: WorkoutLoggerDatabase,
    private val sessionDao: SessionDao,
    private val workoutDao: WorkoutDao
) : SessionRepository {

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    private companion object {
        private const val MILLIS_IN_DAY = 24L * 60L * 60L * 1000L
    }


    override fun observeActiveSession(): Flow<WorkoutSession?> {
        return sessionDao.observeSessionByStatus(SessionStatus.ACTIVE).map { it?.toDomain() }
    }

    override fun observeSession(sessionId: Long): Flow<WorkoutSession?> {
        return sessionDao.observeSession(sessionId).map { it?.toDomain() }
    }

    override fun observeSessionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>> {
        val startInstant = startDate.atStartOfDayIn(timeZone)
        val startEpoch = startInstant.toEpochMilliseconds()
        val endOfDayMillis = endDate.atStartOfDayIn(timeZone).toEpochMilliseconds() + MILLIS_IN_DAY - 1
        return sessionDao.observeSessionsBetween(startEpoch, endOfDayMillis).map { sessions ->
            sessions
                .filter { it.session.status != SessionStatus.CANCELLED }
                .map { it.toDomain() }
        }
    }

    override suspend fun startSessionFromWorkout(workoutId: Long, startedAt: Instant): WorkoutSession {
        return database.withTransaction {
            val workoutWithItems = workoutDao.getWorkoutWithItems(workoutId)
                ?: throw IllegalArgumentException("Workout $workoutId not found")

            val sessionId = sessionDao.insertSession(
                com.example.workoutlogger.data.db.entity.WorkoutSessionEntity(
                    workoutId = workoutWithItems.workout.id,
                    workoutNameSnapshot = workoutWithItems.workout.name,
                    startedAt = startedAt,
                    endedAt = null,
                    status = SessionStatus.ACTIVE
                )
            )

            var position = 0
            workoutWithItems.items.sortedBy { it.position }.forEach { item ->
                if (item.type == WorkoutItemType.EXERCISE) {
                    val exerciseId = sessionDao.insertExercise(
                        com.example.workoutlogger.data.db.entity.SessionExerciseEntity(
                            sessionId = sessionId,
                            position = position++,
                            supersetGroupId = item.supersetGroupId,
                            exerciseName = item.exerciseName.orEmpty()
                        )
                    )

                    val targetSets = max(item.sets ?: 0, 0)
                    repeat(targetSets) { index ->
                        sessionDao.insertSetLog(
                            com.example.workoutlogger.data.db.entity.SessionSetLogEntity(
                                sessionExerciseId = exerciseId,
                                setIndex = index,
                                targetRepsMin = item.repsMin,
                                targetRepsMax = item.repsMax,
                                loggedReps = null,
                                loggedWeight = null,
                                unit = EntityWeightUnit.KG,
                                note = null
                            )
                        )
                    }
                }
            }

            sessionDao.getSessionWithExercises(sessionId)?.toDomain()
                ?: throw IllegalStateException("Failed to load session $sessionId")
        }
    }

    override suspend fun startAdHocSession(name: String, startedAt: Instant): WorkoutSession {
        return database.withTransaction {
            val sessionId = sessionDao.insertSession(
                com.example.workoutlogger.data.db.entity.WorkoutSessionEntity(
                    workoutId = null,
                    workoutNameSnapshot = name,
                    startedAt = startedAt,
                    endedAt = null,
                    status = SessionStatus.ACTIVE
                )
            )

            sessionDao.getSessionWithExercises(sessionId)?.toDomain()
                ?: throw IllegalStateException("Failed to load ad-hoc session $sessionId")
        }
    }

    override suspend fun upsertSessionExercise(sessionId: Long, exercise: SessionExercise): Long {
        return database.withTransaction {
            val exerciseId = if (exercise.id == null) {
                val position = if (exercise.position >= 0) {
                    exercise.position
                } else {
                    (sessionDao.getMaxExercisePosition(sessionId) ?: -1) + 1
                }
                val entity = exercise.copy(position = position).toEntity(sessionId)
                val newId = sessionDao.insertExercise(entity)
                if (exercise.sets.isNotEmpty()) {
                    exercise.sets.sortedBy { it.setIndex }.forEach { set ->
                        sessionDao.insertSetLog(set.copy(sessionExerciseId = newId).toEntity(newId))
                    }
                }
                newId
            } else {
                sessionDao.updateExercise(exercise.toEntity(sessionId))
                exercise.id
                    ?: throw IllegalStateException("Exercise id missing after update for session $sessionId")
            }
            exerciseId
        }
    }

    override suspend fun updateSessionExerciseOrder(sessionId: Long, exerciseIdsInOrder: List<Long>) {
        database.withTransaction {
            exerciseIdsInOrder.forEachIndexed { index, exerciseId ->
                sessionDao.updateExercisePosition(exerciseId, index)
            }
        }
    }

    override suspend fun deleteSessionExercise(exerciseId: Long) {
        database.withTransaction {
            val entity = sessionDao.getExerciseById(exerciseId) ?: return@withTransaction
            sessionDao.deleteSetsByExerciseId(exerciseId)
            sessionDao.deleteExercise(entity)
        }
    }

    override suspend fun upsertSetLog(exerciseId: Long, setLog: SessionSetLog): Long {
        return database.withTransaction {
            if (setLog.id == null) {
                val index = if (setLog.setIndex >= 0) {
                    setLog.setIndex
                } else {
                    (sessionDao.getMaxSetIndex(exerciseId) ?: -1) + 1
                }
                sessionDao.insertSetLog(setLog.copy(setIndex = index, sessionExerciseId = exerciseId).toEntity(exerciseId))
            } else {
                sessionDao.updateSetLog(setLog.toEntity(exerciseId))
                setLog.id
                    ?: throw IllegalStateException("SetLog id missing after update for exercise $exerciseId")
            }
        }
    }

    override suspend fun deleteSetLog(setLogId: Long) {
        database.withTransaction {
            val entity = sessionDao.getSetLogById(setLogId) ?: return@withTransaction
            sessionDao.deleteSetLog(entity)
        }
    }

    override suspend fun finishSession(sessionId: Long, endedAt: Instant) {
        database.withTransaction {
            val entity = sessionDao.getSessionById(sessionId) ?: return@withTransaction
            sessionDao.updateSession(
                entity.copy(
                    endedAt = endedAt,
                    status = SessionStatus.COMPLETED
                )
            )
        }
    }

    override suspend fun cancelSession(sessionId: Long) {
        database.withTransaction {
            val entity = sessionDao.getSessionById(sessionId) ?: return@withTransaction
            sessionDao.deleteSetsBySessionId(sessionId)
            sessionDao.deleteExercisesBySessionId(sessionId)
            sessionDao.deleteSession(entity)
        }
    }

    override suspend fun getPreviousPerformance(exerciseName: String, before: Instant): PreviousPerformance? {
        val exerciseWithSets = sessionDao.getPreviousExerciseWithSets(
            exerciseName = exerciseName,
            beforeEpoch = before.toEpochMilliseconds()
        ) ?: return null
        val parentSession = sessionDao.getSessionById(exerciseWithSets.exercise.sessionId) ?: return null
        val domainExercise = exerciseWithSets.toDomain()
        val comparator = compareBy<SessionSetLog> { it.loggedWeight ?: Double.MIN_VALUE }
            .thenBy { it.loggedReps ?: Int.MIN_VALUE }
        val bestSet = domainExercise.sets.filter { it.loggedWeight != null || it.loggedReps != null }
            .maxWithOrNull(comparator)
        return PreviousPerformance(
            sessionId = parentSession.id,
            sessionEndedAt = parentSession.endedAt ?: parentSession.startedAt,
            exerciseName = exerciseName,
            sets = domainExercise.sets,
            bestSet = bestSet
        )
    }
}

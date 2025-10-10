package com.example.workoutlogger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.workoutlogger.data.db.entity.SessionExerciseEntity
import com.example.workoutlogger.data.db.entity.SessionExerciseWithSets
import com.example.workoutlogger.data.db.entity.SessionSetLogEntity
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.SessionWithExercises
import com.example.workoutlogger.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = :status ORDER BY started_at DESC LIMIT 1")
    fun observeSessionByStatus(status: SessionStatus): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionWithExercises(sessionId: Long): SessionWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE started_at BETWEEN :startEpoch AND :endEpoch ORDER BY started_at ASC")
    fun observeSessionsBetween(startEpoch: Long, endEpoch: Long): Flow<List<SessionWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE started_at BETWEEN :startEpoch AND :endEpoch ORDER BY started_at ASC")
    suspend fun getSessionsBetween(startEpoch: Long, endEpoch: Long): List<SessionWithExercises>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY started_at ASC")
    suspend fun getAllSessions(): List<SessionWithExercises>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = :status ORDER BY started_at DESC LIMIT 1")
    suspend fun getLatestSessionByStatus(status: SessionStatus): SessionWithExercises?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: SessionExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: SessionExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: SessionExerciseEntity)

    @Query("DELETE FROM session_set_logs WHERE session_exercise_id = :exerciseId")
    suspend fun deleteSetsByExerciseId(exerciseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(setLog: SessionSetLogEntity): Long

    @Update
    suspend fun updateSetLog(setLog: SessionSetLogEntity)

    @Delete
    suspend fun deleteSetLog(setLog: SessionSetLogEntity)

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): SessionExerciseEntity?

    @Query("SELECT * FROM session_set_logs WHERE id = :id")
    suspend fun getSetLogById(id: Long): SessionSetLogEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    @Transaction
    @Query(
        "SELECT * FROM session_exercises WHERE exercise_name = :exerciseName " +
            "AND session_id IN (SELECT id FROM workout_sessions WHERE ended_at IS NOT NULL AND ended_at < :beforeEpoch) " +
            "ORDER BY (SELECT ended_at FROM workout_sessions WHERE id = session_id) DESC LIMIT 1"
    )
    suspend fun getPreviousExerciseWithSets(
        exerciseName: String,
        beforeEpoch: Long
    ): SessionExerciseWithSets?

    @Query("SELECT COUNT(*) FROM session_set_logs WHERE session_exercise_id = :exerciseId")
    suspend fun countSetsForExercise(exerciseId: Long): Int

    @Query("UPDATE session_exercises SET position = :position WHERE id = :exerciseId")
    suspend fun updateExercisePosition(exerciseId: Long, position: Int)

    @Query("UPDATE session_set_logs SET set_index = :index WHERE id = :setLogId")
    suspend fun updateSetIndex(setLogId: Long, index: Int)

    @Query("SELECT MAX(position) FROM session_exercises WHERE session_id = :sessionId")
    suspend fun getMaxExercisePosition(sessionId: Long): Int?

    @Query("SELECT MAX(set_index) FROM session_set_logs WHERE session_exercise_id = :exerciseId")
    suspend fun getMaxSetIndex(exerciseId: Long): Int?
}

package com.example.workoutlogger.domain.usecase.importexport

import com.example.workoutlogger.domain.importexport.CsvReader
import com.example.workoutlogger.domain.importexport.FitNotesIosCsvProfile
import com.example.workoutlogger.domain.importexport.StrongHevyCsvProfile
import com.example.workoutlogger.domain.importexport.model.ImportResult
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.repository.SessionRepository
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.plus

class ImportWorkoutCsvUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    private val strongHeader = StrongHevyCsvProfile.header.map { it.lowercase(Locale.US) }
    private val fitNotesHeader = FitNotesIosCsvProfile.header.map { it.lowercase(Locale.US) }
    private val strongFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend operator fun invoke(inputStream: InputStream): ImportResult {
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.mark(1024 * 1024)
            val headerLine = reader.readLine() ?: return ImportResult(workouts = 0, sets = 0)
            reader.reset()
            val delimiter = if (headerLine.contains(';')) ';' else ','
            val rows = CsvReader(reader, delimiter).iterator()
            if (!rows.hasNext()) return ImportResult(0, 0)
            val header = rows.next().map { it.trim().lowercase(Locale.US) }
            val sessions = when {
                header == strongHeader -> parseStrongCsv(rows)
                header == fitNotesHeader -> parseFitNotesCsv(rows)
                else -> error("Unknown CSV header: ${header.joinToString()}")
            }
            sessionRepository.importSessions(sessions)
            val setCount = sessions.sumOf { session -> session.exercises.sumOf { it.sets.size } }
            return ImportResult(workouts = sessions.size, sets = setCount)
        }
    }

    private fun parseStrongCsv(rows: Iterator<List<String>>): List<WorkoutSession> {
        data class StrongRow(
            val dateTime: LocalDateTime,
            val workoutName: String,
            val exerciseName: String,
            val setOrder: Int,
            val weight: Double?,
            val unit: WeightUnit,
            val reps: Int?,
            val notes: String?,
            val workoutDuration: String?,
            val workoutNotes: String?
        )

        val timeZone = TimeZone.currentSystemDefault()
        val workouts = linkedMapOf<Pair<String, String>, MutableList<StrongRow>>()
        val supersetRegister = mutableMapOf<String, MutableSet<String>>()
        while (rows.hasNext()) {
            val row = rows.next()
            if (row.all { it.isBlank() }) continue
            val dateString = row.getOrNull(0)?.trim().orEmpty()
            if (dateString.isBlank()) continue
            val dateTime = runCatching { LocalDateTime.parse(dateString, strongFormatter) }.getOrNull()
                ?: continue
            val workoutName = row.getOrNull(1)?.ifBlank { "Imported Workout" } ?: "Imported Workout"
            val exerciseName = row.getOrNull(2)
                ?.takeIf { it.isNotBlank() }
                ?: continue
            val setOrder = row.getOrNull(3)?.toIntOrNull() ?: 1
            val weight = row.getOrNull(4)?.replace(',', '.')?.toDoubleOrNull()
            val weightUnit = row.getOrNull(5)?.toWeightUnit() ?: WeightUnit.KG
            val reps = row.getOrNull(6)?.toIntOrNull()
            val notes = row.getOrNull(11)?.takeIf { it.isNotBlank() }
            val workoutNotes = row.getOrNull(12)?.takeIf { it.isNotBlank() }
            val duration = row.getOrNull(13)?.takeIf { it.isNotBlank() }
            if (notes != null) {
                extractSupersetPartners(notes).forEach { partner ->
                    supersetRegister.getOrPut(exerciseName) { mutableSetOf() }.add(partner)
                    supersetRegister.getOrPut(partner) { mutableSetOf() }.add(exerciseName)
                }
            }
            val key = workoutName to dateString
            val bucket = workouts.getOrPut(key) { mutableListOf() }
            bucket += StrongRow(
                dateTime = dateTime,
                workoutName = workoutName,
                exerciseName = exerciseName,
                setOrder = setOrder,
                weight = weight,
                unit = weightUnit,
                reps = reps,
                notes = notes,
                workoutDuration = duration,
                workoutNotes = workoutNotes
            )
        }

        val supersetGroups = mutableMapOf<String, String>()
        var supersetCounter = 0
        val sessions = mutableListOf<WorkoutSession>()
        workouts.values.forEach { rowsForWorkout ->
            if (rowsForWorkout.isEmpty()) return@forEach
            val sortedRows = rowsForWorkout.sortedWith(compareBy({ it.exerciseName }, { it.setOrder }))
            val startedAt = sortedRows.minOf { it.dateTime }.atZone(timeZone.toJavaZoneId()).toInstant().toKotlinInstant()
            val endedAt = computeEndedAt(startedAt, sortedRows.first().workoutDuration)
            val exercises = linkedMapOf<String, MutableList<StrongRow>>()
            rowsForWorkout.forEach { strongRow ->
                exercises.getOrPut(strongRow.exerciseName) { mutableListOf() }.add(strongRow)
            }
            val exerciseList = exercises.entries.mapIndexed { index, entry ->
                val groupId = assignSupersetGroup(
                    exerciseName = entry.key,
                    register = supersetRegister,
                    groups = supersetGroups
                ) { "import-${supersetCounter++}" }
                SessionExercise(
                    id = null,
                    sessionId = null,
                    position = index,
                    supersetGroupId = groupId,
                    exerciseName = entry.key,
                    sets = entry.value.sortedBy { it.setOrder }.mapIndexed { setIndex, setRow ->
                        SessionSetLog(
                            id = null,
                            sessionExerciseId = null,
                            setIndex = setIndex,
                            targetRepsMin = null,
                            targetRepsMax = null,
                            loggedReps = setRow.reps,
                            loggedWeight = setRow.weight,
                            unit = setRow.unit,
                            note = listOfNotNull(setRow.notes, setRow.workoutNotes).joinToString(separator = " | ").takeIf { it.isNotBlank() }
                        )
                    }
                )
            }
            sessions += WorkoutSession(
                id = null,
                workoutId = null,
                workoutNameSnapshot = rowsForWorkout.first().workoutName,
                startedAt = startedAt,
                endedAt = endedAt,
                status = WorkoutStatus.COMPLETED,
                exercises = exerciseList
            )
        }
        return sessions
    }

    private fun parseFitNotesCsv(rows: Iterator<List<String>>): List<WorkoutSession> {
        data class FitNotesRow(
            val date: LocalDate,
            val exerciseName: String,
            val weight: Double?,
            val unit: WeightUnit,
            val reps: Int?,
            val notes: String?
        )

        val timeZone = TimeZone.currentSystemDefault()
        val workouts = linkedMapOf<LocalDate, MutableList<FitNotesRow>>()
        while (rows.hasNext()) {
            val row = rows.next()
            if (row.all { it.isBlank() }) continue
            val date = row.getOrNull(0)?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() } ?: continue
            val exerciseName = row.getOrNull(1)?.ifBlank { "Exercise" } ?: "Exercise"
            val weightKg = row.getOrNull(3)?.replace(',', '.')?.toDoubleOrNull()
            val weightLb = row.getOrNull(4)?.replace(',', '.')?.toDoubleOrNull()
            val reps = row.getOrNull(5)?.toIntOrNull()
            val distance = row.getOrNull(6)?.takeIf { it.isNotBlank() }
            val distanceUnit = row.getOrNull(7)?.takeIf { it.isNotBlank() }
            val time = row.getOrNull(8)?.takeIf { it.isNotBlank() }
            val notes = row.getOrNull(9)?.takeIf { it.isNotBlank() }

            val (weight, unit) = when {
                weightLb != null -> weightLb to WeightUnit.LB
                weightKg != null -> weightKg to WeightUnit.KG
                else -> null to WeightUnit.KG
            }
            val enrichedNotes = buildList {
                if (notes != null) add(notes)
                if (!distance.isNullOrBlank()) {
                    add("Distance: $distance${distanceUnit?.let { " $it" } ?: ""}".trim())
                }
                if (!time.isNullOrBlank()) {
                    add("Time: $time")
                }
            }.takeIf { it.isNotEmpty() }?.joinToString(" | ")

            workouts.getOrPut(date) { mutableListOf() } += FitNotesRow(
                date = date,
                exerciseName = exerciseName,
                weight = weight,
                unit = unit,
                reps = reps,
                notes = enrichedNotes
            )
        }

        val sessions = mutableListOf<WorkoutSession>()
        workouts.forEach { (date, rowsForDate) ->
            val startedAt = date.atStartOfDay(timeZone.toJavaZoneId()).toInstant().toKotlinInstant()
            val exercises = rowsForDate.groupBy { it.exerciseName }.entries.mapIndexed { index, entry ->
                SessionExercise(
                    id = null,
                    sessionId = null,
                    position = index,
                    supersetGroupId = null,
                    exerciseName = entry.key,
                    sets = entry.value.mapIndexed { setIndex, set ->
                        SessionSetLog(
                            id = null,
                            sessionExerciseId = null,
                            setIndex = setIndex,
                            targetRepsMin = null,
                            targetRepsMax = null,
                            loggedReps = set.reps,
                            loggedWeight = set.weight,
                            unit = set.unit,
                            note = set.notes
                        )
                    }
                )
            }
            sessions += WorkoutSession(
                id = null,
                workoutId = null,
                workoutNameSnapshot = "FitNotes ${date}",
                startedAt = startedAt,
                endedAt = startedAt,
                status = WorkoutStatus.COMPLETED,
                exercises = exercises
            )
        }
        return sessions
    }

    private fun computeEndedAt(startedAt: kotlinx.datetime.Instant, duration: String?): kotlinx.datetime.Instant? {
        if (duration.isNullOrBlank()) return startedAt
        val normalized = duration.lowercase(Locale.US)
        val hours = Regex("(\\d+)h").find(normalized)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val minutes = Regex("(\\d+)m").find(normalized)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val seconds = Regex("(\\d+)s").find(normalized)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        val durationSeconds = if (totalSeconds > 0) totalSeconds else null
        return durationSeconds?.let { startedAt.plus(it, kotlinx.datetime.DateTimeUnit.SECOND) } ?: startedAt
    }

    private fun extractSupersetPartners(notes: String): List<String> {
        val regex = Regex("Superset with:([^|]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(notes) ?: return emptyList()
        return match.groupValues[1]
            .split(',', '&')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun String.toWeightUnit(): WeightUnit = when (lowercase(Locale.US)) {
        "lb", "lbs" -> WeightUnit.LB
        else -> WeightUnit.KG
    }

    private fun assignSupersetGroup(
        exerciseName: String,
        register: Map<String, MutableSet<String>>,
        groups: MutableMap<String, String>,
        idSupplier: () -> String
    ): String? {
        val partners = register[exerciseName] ?: return null
        if (partners.isEmpty()) return null
        val members = (listOf(exerciseName) + partners).map { it.trim() }.filter { it.isNotEmpty() }
        members.forEach { member ->
            groups[member]?.let { return it }
        }
        val newId = idSupplier()
        members.forEach { member -> groups[member] = newId }
        return newId
    }
}

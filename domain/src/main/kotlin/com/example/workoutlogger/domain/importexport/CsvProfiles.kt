package com.example.workoutlogger.domain.importexport

import com.example.workoutlogger.domain.importexport.model.ExportRow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime

/** Describes how to transform an [ExportRow] into a CSV line. */
sealed interface CsvProfile {
    val header: List<String>
    fun toFields(row: ExportRow): List<String>
}

private val decimalFormatter = DecimalFormat("#.########", DecimalFormatSymbols.getInstance(Locale.US)).apply {
    maximumFractionDigits = 8
}

/** Strong/Hevy compatible CSV profile. */
object StrongHevyCsvProfile : CsvProfile {
    override val header: List<String> = listOf(
        "Date",
        "Workout Name",
        "Exercise Name",
        "Set Order",
        "Weight",
        "Weight Unit",
        "Reps",
        "RPE",
        "Distance",
        "Distance Unit",
        "Seconds",
        "Notes",
        "Workout Notes",
        "Workout Duration"
    )

    private val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun toFields(row: ExportRow): List<String> {
        return listOf(
            row.dateTime.toJavaLocalDateTime().format(formatter),
            row.workoutName.orEmpty(),
            row.exerciseName,
            row.setOrder.toString(),
            row.weight?.let(decimalFormatter::format).orEmpty(),
            row.weightUnit.orEmpty(),
            row.reps?.toString().orEmpty(),
            row.rpe?.let(decimalFormatter::format).orEmpty(),
            row.distance?.let(decimalFormatter::format).orEmpty(),
            row.distanceUnit.orEmpty(),
            row.seconds?.toString().orEmpty(),
            row.notes.orEmpty(),
            row.workoutNotes.orEmpty(),
            row.workoutDuration.orEmpty()
        )
    }
}

/** FitNotes iOS compatible CSV profile. */
object FitNotesIosCsvProfile : CsvProfile {
    override val header: List<String> = listOf(
        "Date",
        "Exercise",
        "Category",
        "Weight (kg)",
        "Weight (lbs)",
        "Reps",
        "Distance",
        "Distance Unit",
        "Time",
        "Notes",
        "Kind"
    )

    private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun toFields(row: ExportRow): List<String> {
        val (kg, lbs) = when (row.weightUnit?.lowercase(Locale.US)) {
            "lb", "lbs" -> Pair(row.weight?.times(0.45359237), row.weight)
            null -> Pair(row.weight, row.weight?.times(2.2046226218))
            else -> Pair(row.weight, row.weight?.times(2.2046226218))
        }
        val timeString = row.seconds?.let { seconds ->
            val minutes = seconds / 60
            val remaining = seconds % 60
            String.format(Locale.US, "%02d:%02d", minutes, remaining)
        } ?: ""
        val kind = buildString {
            if (kg != null || lbs != null) append('w')
            if (row.reps != null) append('r')
            if (row.distance != null) append('d')
            if ((row.seconds ?: 0) > 0) append('t')
            if (isEmpty()) append('r')
        }
        return listOf(
            row.dateTime.date.toJavaLocalDate().format(dateFormatter),
            row.exerciseName,
            row.category.orEmpty(),
            kg?.let(decimalFormatter::format).orEmpty(),
            lbs?.let(decimalFormatter::format).orEmpty(),
            row.reps?.toString().orEmpty(),
            row.distance?.let(decimalFormatter::format).orEmpty(),
            row.distanceUnit.orEmpty(),
            timeString,
            row.notes.orEmpty(),
            kind
        )
    }
}

/** Available CSV profiles the UI can offer. */
enum class CsvProfileType(val profile: CsvProfile) {
    STRONG_HEVY(StrongHevyCsvProfile),
    FITNOTES_IOS(FitNotesIosCsvProfile)
}

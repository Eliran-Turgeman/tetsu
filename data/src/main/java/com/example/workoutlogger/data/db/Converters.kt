package com.example.workoutlogger.data.db

import androidx.room.TypeConverter
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.WorkoutItemType
import com.example.workoutlogger.data.db.entity.WeightUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant

class WorkoutLoggerConverters {
    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun toEpochMillis(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun fromWorkoutItemType(value: WorkoutItemType?): String? = value?.name

    @TypeConverter
    fun toWorkoutItemType(value: String?): WorkoutItemType? = value?.let { WorkoutItemType.valueOf(it) }

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus?): String? = value?.name

    @TypeConverter
    fun toSessionStatus(value: String?): SessionStatus? = value?.let { SessionStatus.valueOf(it) }

    @TypeConverter
    fun fromWeightUnit(value: WeightUnit?): String? = value?.name

    @TypeConverter
    fun toWeightUnit(value: String?): WeightUnit? = value?.let { WeightUnit.valueOf(it) }

    @TypeConverter
    fun fromDayOfWeekSet(value: String?): Set<DayOfWeek>? = value?.split(',')
        ?.filter { it.isNotBlank() }
        ?.map { DayOfWeek.valueOf(it) }
        ?.toSet()

    @TypeConverter
    fun toDayOfWeekSet(value: Set<DayOfWeek>?): String? = value?.joinToString(separator = ",") { it.name }
}

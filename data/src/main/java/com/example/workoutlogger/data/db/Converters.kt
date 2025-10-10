package com.example.workoutlogger.data.db

import androidx.room.TypeConverter
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.WorkoutItemType
import com.example.workoutlogger.data.db.entity.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.AchievementType
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
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

    @TypeConverter
    fun fromAchievementType(value: AchievementType?): String? = value?.name

    @TypeConverter
    fun toAchievementType(value: String?): AchievementType? = value?.let { AchievementType.valueOf(it) }

    @TypeConverter
    fun fromMetricType(value: MetricType?): String? = value?.name

    @TypeConverter
    fun toMetricType(value: String?): MetricType? = value?.let { MetricType.valueOf(it) }

    @TypeConverter
    fun fromAchievementStatus(value: AchievementStatus?): String? = value?.name

    @TypeConverter
    fun toAchievementStatus(value: String?): AchievementStatus? = value?.let { AchievementStatus.valueOf(it) }

    @TypeConverter
    fun fromUserGoalKind(value: UserGoalKind?): String? = value?.name

    @TypeConverter
    fun toUserGoalKind(value: String?): UserGoalKind? = value?.let { UserGoalKind.valueOf(it) }
}

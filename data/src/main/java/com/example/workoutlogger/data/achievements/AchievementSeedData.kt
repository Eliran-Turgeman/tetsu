package com.example.workoutlogger.data.achievements

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.workoutlogger.domain.model.achievements.AchievementType
import com.example.workoutlogger.domain.model.achievements.MetricType

internal data class AchievementSeedDefinition(
    val id: String,
    val title: String,
    val description: String,
    val type: AchievementType,
    val metric: MetricType,
    val targetValue: Double,
    val windowDays: Int?,
    val repeatable: Boolean,
    val tier: Int,
    val iconKey: String,
    val sort: Int
)

internal object AchievementSeedData {

    val definitions: List<AchievementSeedDefinition> = listOf(
        AchievementSeedDefinition(
            id = "first_workout",
            title = "First Workout",
            description = "Log your very first workout session.",
            type = AchievementType.FIRSTS,
            metric = MetricType.FIRST_WORKOUT,
            targetValue = 1.0,
            windowDays = null,
            repeatable = false,
            tier = 1,
            iconKey = "achievement_first_workout",
            sort = 0
        ),
        AchievementSeedDefinition(
            id = "consistency_weekly_3",
            title = "Weekly Warrior",
            description = "Complete 3 workouts within any rolling 7 days.",
            type = AchievementType.CONSISTENCY,
            metric = MetricType.WORKOUTS_PER_WEEK,
            targetValue = 3.0,
            windowDays = 7,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_weekly_warrior",
            sort = 10
        ),
        AchievementSeedDefinition(
            id = "consistency_monthly_12",
            title = "Monthly Momentum",
            description = "Complete 12 workouts within 30 days.",
            type = AchievementType.CONSISTENCY,
            metric = MetricType.WORKOUTS_PER_MONTH,
            targetValue = 12.0,
            windowDays = 30,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_monthly_momentum",
            sort = 20
        ),
        AchievementSeedDefinition(
            id = "streak_7",
            title = "Seven Day Streak",
            description = "Stay active 7 days in a row.",
            type = AchievementType.CONSISTENCY,
            metric = MetricType.STREAK_ACTIVE_DAYS,
            targetValue = 7.0,
            windowDays = 7,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_streak_7",
            sort = 30
        ),
        AchievementSeedDefinition(
            id = "streak_30",
            title = "Thirty Day Streak",
            description = "Stay active for 30 consecutive days.",
            type = AchievementType.CONSISTENCY,
            metric = MetricType.STREAK_ACTIVE_DAYS,
            targetValue = 30.0,
            windowDays = 30,
            repeatable = false,
            tier = 2,
            iconKey = "achievement_streak_30",
            sort = 40
        ),
        AchievementSeedDefinition(
            id = "volume_50k",
            title = "Volume 50K",
            description = "Accumulate 50,000 kg of lifted volume.",
            type = AchievementType.VOLUME,
            metric = MetricType.TOTAL_VOLUME,
            targetValue = 50_000.0,
            windowDays = null,
            repeatable = false,
            tier = 1,
            iconKey = "achievement_volume_50k",
            sort = 50
        ),
        AchievementSeedDefinition(
            id = "volume_250k",
            title = "Volume 250K",
            description = "Accumulate 250,000 kg of lifted volume.",
            type = AchievementType.VOLUME,
            metric = MetricType.TOTAL_VOLUME,
            targetValue = 250_000.0,
            windowDays = null,
            repeatable = false,
            tier = 2,
            iconKey = "achievement_volume_250k",
            sort = 60
        ),
        AchievementSeedDefinition(
            id = "volume_1m",
            title = "Volume 1M",
            description = "Accumulate 1,000,000 kg of lifted volume.",
            type = AchievementType.VOLUME,
            metric = MetricType.TOTAL_VOLUME,
            targetValue = 1_000_000.0,
            windowDays = null,
            repeatable = false,
            tier = 3,
            iconKey = "achievement_volume_1m",
            sort = 70
        ),
        AchievementSeedDefinition(
            id = "variety_push_pull_legs_week",
            title = "Balanced Week",
            description = "Hit push, pull, and legs within 7 days.",
            type = AchievementType.VARIETY,
            metric = MetricType.VARIETY_BALANCE,
            targetValue = 3.0,
            windowDays = 7,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_variety_week",
            sort = 80
        ),
        AchievementSeedDefinition(
            id = "early_bird_5",
            title = "Early Bird",
            description = "Finish 5 workouts before 8am.",
            type = AchievementType.TIME_OF_DAY,
            metric = MetricType.EARLY_BIRD,
            targetValue = 5.0,
            windowDays = 30,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_early_bird",
            sort = 90
        ),
        AchievementSeedDefinition(
            id = "comeback_3_after_14_idle",
            title = "Comeback Kid",
            description = "After 2 weeks off, complete 3 workouts within 7 days.",
            type = AchievementType.COMEBACK,
            metric = MetricType.COMEBACK,
            targetValue = 3.0,
            windowDays = 7,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_comeback",
            sort = 100
        ),
        AchievementSeedDefinition(
            id = "schedule_adherence_80",
            title = "Schedule Star",
            description = "Hit at least 80% of scheduled workouts over 4 weeks.",
            type = AchievementType.SCHEDULE,
            metric = MetricType.SCHEDULE_ADHERENCE,
            targetValue = 0.8,
            windowDays = 28,
            repeatable = true,
            tier = 1,
            iconKey = "achievement_schedule_star",
            sort = 110
        )
    )

    fun insertDefinitions(db: SupportSQLiteDatabase) {
        definitions.forEach { def ->
            val values = contentValuesOf(
                "id" to def.id,
                "title" to def.title,
                "description" to def.description,
                "type" to def.type.name,
                "metric" to def.metric.name,
                "targetValue" to def.targetValue,
                "windowDays" to def.windowDays,
                "repeatable" to if (def.repeatable) 1 else 0,
                "tier" to def.tier,
                "iconKey" to def.iconKey,
                "sort" to def.sort
            )
            db.insert("achievement_definitions", SQLiteDatabase.CONFLICT_REPLACE, values)
        }
    }

    fun insertInstancesFromDefinitions(db: SupportSQLiteDatabase, createdAtMillis: Long) {
        definitions.forEach { def ->
            val values = ContentValues().apply {
                put("instanceId", "def_${def.id}")
                put("definitionId", def.id)
                put("createdAt", createdAtMillis)
                put("status", com.example.workoutlogger.domain.model.achievements.AchievementStatus.IN_PROGRESS.name)
                put("progressCurrent", 0.0)
                put("progressTarget", def.targetValue)
                put("progressUnit", when (def.metric) {
                    MetricType.SCHEDULE_ADHERENCE -> "%"
                    MetricType.TOTAL_VOLUME -> "kg"
                    else -> "workouts"
                })
                put("percent", 0.0)
                putNull("completedAt")
                putNull("userNotes")
                putNull("extraJson")
            }
            db.insert("achievement_instances", SQLiteDatabase.CONFLICT_IGNORE, values)
        }
    }
}

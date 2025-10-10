package com.example.workoutlogger.domain.model.achievements

enum class AchievementType {
    FIRSTS,
    CONSISTENCY,
    VOLUME,
    VARIETY,
    HEATMAP,
    TIME_OF_DAY,
    COMEBACK,
    SCHEDULE,
    USER_GOAL
}

enum class MetricType {
    FIRST_WORKOUT,
    WORKOUTS_PER_WEEK,
    WORKOUTS_PER_MONTH,
    STREAK_ACTIVE_DAYS,
    TOTAL_VOLUME,
    TOTAL_SETS,
    VARIETY_BALANCE,
    HEATMAP_DAYS,
    EARLY_BIRD,
    COMEBACK,
    SCHEDULE_ADHERENCE,
    ONE_RM_TARGET,
    FREQUENCY_TARGET,
    REPS_AT_WEIGHT,
    BODY_WEIGHT_RELATION,
    TIME_UNDER_TENSION
}

enum class AchievementStatus {
    LOCKED,
    IN_PROGRESS,
    COMPLETED
}

enum class UserGoalKind {
    LIFT_WEIGHT,
    REPS_AT_WEIGHT,
    FREQUENCY_IN_WINDOW,
    BODY_WEIGHT_RELATION,
    STREAK,
    TIME_UNDER_TENSION
}

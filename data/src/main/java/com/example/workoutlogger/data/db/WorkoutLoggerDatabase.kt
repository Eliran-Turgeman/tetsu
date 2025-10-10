package com.example.workoutlogger.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.workoutlogger.data.db.dao.AchievementsDao
import com.example.workoutlogger.data.db.dao.ScheduleDao
import com.example.workoutlogger.data.db.dao.SessionDao
import com.example.workoutlogger.data.db.dao.WorkoutDao
import com.example.workoutlogger.data.db.entity.AchievementDefinitionEntity
import com.example.workoutlogger.data.db.entity.AchievementInstanceEntity
import com.example.workoutlogger.data.db.entity.SessionExerciseEntity
import com.example.workoutlogger.data.db.entity.SessionSetLogEntity
import com.example.workoutlogger.data.db.entity.UserGoalEntity
import com.example.workoutlogger.data.db.entity.WorkoutDailySummaryEntity
import com.example.workoutlogger.data.db.entity.WorkoutScheduleEntity
import com.example.workoutlogger.data.db.entity.WorkoutSessionEntity
import com.example.workoutlogger.data.db.entity.WorkoutEntity
import com.example.workoutlogger.data.db.entity.WorkoutItemEntity

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutItemEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetLogEntity::class,
        WorkoutScheduleEntity::class,
        AchievementDefinitionEntity::class,
        AchievementInstanceEntity::class,
        UserGoalEntity::class,
        WorkoutDailySummaryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(WorkoutLoggerConverters::class)
abstract class WorkoutLoggerDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun achievementsDao(): AchievementsDao
}

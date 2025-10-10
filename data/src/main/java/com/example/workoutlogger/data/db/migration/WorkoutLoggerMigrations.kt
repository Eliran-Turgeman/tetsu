package com.example.workoutlogger.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.workoutlogger.data.achievements.AchievementSeedData

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `achievement_definitions` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `metric` TEXT NOT NULL,
                `targetValue` REAL NOT NULL,
                `windowDays` INTEGER,
                `repeatable` INTEGER NOT NULL,
                `tier` INTEGER NOT NULL,
                `iconKey` TEXT NOT NULL,
                `sort` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `achievement_instances` (
                `instanceId` TEXT NOT NULL,
                `definitionId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `progressCurrent` REAL NOT NULL,
                `progressTarget` REAL NOT NULL,
                `progressUnit` TEXT NOT NULL,
                `percent` REAL NOT NULL,
                `completedAt` INTEGER,
                `userNotes` TEXT,
                `extraJson` TEXT,
                PRIMARY KEY(`instanceId`)
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_achievement_instances_definitionId` ON `achievement_instances` (`definitionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_achievement_instances_status` ON `achievement_instances` (`status`)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_goals` (
                `goalId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `kind` TEXT NOT NULL,
                `exerciseName` TEXT,
                `targetValue` REAL NOT NULL,
                `secondaryValue` REAL,
                `windowDays` INTEGER,
                `deadlineAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`goalId`)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_daily_summaries` (
                `date_epoch_day` INTEGER NOT NULL,
                `workouts_completed` INTEGER NOT NULL,
                `total_sets` INTEGER NOT NULL,
                `total_volume_kg` REAL NOT NULL,
                `unique_exercises` INTEGER NOT NULL,
                `category_mask` INTEGER NOT NULL,
                `upper_lower_mask` INTEGER NOT NULL,
                `early_sessions` INTEGER NOT NULL,
                `minutes_active` INTEGER NOT NULL,
                PRIMARY KEY(`date_epoch_day`)
            )
            """.trimIndent()
        )

        AchievementSeedData.insertDefinitions(database)
        AchievementSeedData.insertInstancesFromDefinitions(database, System.currentTimeMillis())
    }
}

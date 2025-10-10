package com.example.workoutlogger.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.workoutlogger.data.db.migration.MIGRATION_1_2
import java.io.IOException
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutLoggerMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutLoggerDatabase::class.java
    )

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_seedsAchievements() {
        helper.createDatabase(dbName, 1).apply {
            close()
        }

        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        Room.databaseBuilder(context, WorkoutLoggerDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .build()
            .use { database ->
                val count = database.openHelper.readableDatabase
                    .query("SELECT COUNT(*) FROM achievement_definitions")
                    .use { cursor ->
                        cursor.moveToFirst()
                        cursor.getInt(0)
                    }
                assertTrue(count >= 1)

                val instances = database.openHelper.readableDatabase
                    .query("SELECT COUNT(*) FROM achievement_instances")
                    .use { cursor ->
                        cursor.moveToFirst()
                        cursor.getInt(0)
                    }
                assertTrue(instances >= 1)
            }
    }
}

private inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        this?.close()
    }
}

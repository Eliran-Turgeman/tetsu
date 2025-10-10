package com.example.workoutlogger.domain.usecase.schedule

import com.example.workoutlogger.domain.model.WorkoutSchedule
import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import javax.inject.Inject

/** Persists a schedule for WorkManager reminders. */
class SaveScheduleUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase
) {
    suspend operator fun invoke(schedule: WorkoutSchedule) {
        require(schedule.notifyHour in 0..23) { "Hour must be within 0..23" }
        require(schedule.notifyMinute in 0..59) { "Minute must be within 0..59" }
        workoutRepository.upsertSchedule(schedule)
        evaluateAchievementsUseCase()
    }
}

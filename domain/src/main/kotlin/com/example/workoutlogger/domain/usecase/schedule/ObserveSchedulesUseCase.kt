package com.example.workoutlogger.domain.usecase.schedule

import com.example.workoutlogger.domain.model.WorkoutSchedule
import com.example.workoutlogger.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams all schedules for WorkManager orchestration. */
class ObserveSchedulesUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(): Flow<List<WorkoutSchedule>> = workoutRepository.observeSchedules()
}

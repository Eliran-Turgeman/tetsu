package com.example.workoutlogger.domain.usecase.schedule

import com.example.workoutlogger.domain.model.WorkoutSchedule
import com.example.workoutlogger.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Watches schedule updates for a given workout definition. */
class ObserveScheduleForWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(workoutId: Long): Flow<WorkoutSchedule?> =
        workoutRepository.observeScheduleForWorkout(workoutId)
}

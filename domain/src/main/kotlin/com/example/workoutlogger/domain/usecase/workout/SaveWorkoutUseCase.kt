package com.example.workoutlogger.domain.usecase.workout

import com.example.workoutlogger.domain.model.WorkoutItemType
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import javax.inject.Inject

/**
 * Persists a workout and normalises its items.
 */
class SaveWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase
) {
    suspend operator fun invoke(workout: Workout): Long {
        require(workout.name.isNotBlank()) { "Workout name cannot be blank" }
        val normalisedItems = workout.items
            .sortedBy { it.position }
            .mapIndexed { index, item ->
                when (item.type) {
                    WorkoutItemType.EXERCISE -> {
                        require(!item.exerciseName.isNullOrBlank()) { "Exercise name required" }
                        require((item.sets ?: 0) >= 0) { "Sets cannot be negative" }
                        item.copy(position = index)
                    }
                    WorkoutItemType.SUPERSET_HEADER -> item.copy(position = index)
                }
            }
        val id = workoutRepository.upsertWorkout(workout.copy(items = normalisedItems))
        evaluateAchievementsUseCase()
        return id
    }
}

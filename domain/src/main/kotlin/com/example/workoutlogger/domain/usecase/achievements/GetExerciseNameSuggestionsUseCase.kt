package com.example.workoutlogger.domain.usecase.achievements

import com.example.workoutlogger.domain.repository.WorkoutRepository
import javax.inject.Inject

/**
 * Provides a canonical list of exercise names sourced from the user's saved templates.
 */
class GetExerciseNameSuggestionsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(): List<String> = workoutRepository.getDistinctExerciseNames()
}

package com.example.workoutlogger.feature.share.data

import java.time.LocalDate

fun selectAchievements(
    selection: AchievementSelection,
    prepared: List<PreparedAchievement>,
    range: ClosedRange<LocalDate>
): List<PreparedAchievement> {
    return when (selection) {
        is AchievementSelection.AutoTopN -> autoSelect(selection, prepared, range)
        is AchievementSelection.Manual -> manualSelect(selection, prepared)
    }
}

private fun autoSelect(
    selection: AchievementSelection.AutoTopN,
    prepared: List<PreparedAchievement>,
    range: ClosedRange<LocalDate>
): List<PreparedAchievement> {
    val eligible = prepared.filter { achievement ->
        when {
            achievement.achievedAt == null -> selection.includeInProgress
            else -> achievement.achievedAt in range
        }
    }

    val sorted = eligible.sortedWith(compareByDescending<PreparedAchievement> { it.achievedAt ?: LocalDate.MIN }
        .thenByDescending { it.tier ?: 0 }
        .thenBy { it.name })

    if (sorted.isEmpty()) return emptyList()

    val picked = mutableListOf<PreparedAchievement>()
    val seenCategories = mutableSetOf<String?>()
    sorted.forEach { achievement ->
        if (picked.size >= selection.n) return@forEach
        val category = achievement.category
        if (seenCategories.add(category)) {
            picked.add(achievement)
        }
    }

    if (picked.size < selection.n) {
        sorted.forEach { achievement ->
            if (picked.size >= selection.n) return@forEach
            if (!picked.contains(achievement)) {
                picked.add(achievement)
            }
        }
    }

    return picked
}

private fun manualSelect(
    selection: AchievementSelection.Manual,
    prepared: List<PreparedAchievement>
): List<PreparedAchievement> {
    if (selection.ids.isEmpty()) return emptyList()
    val byId = prepared.associateBy { it.id }
    return selection.ids.mapNotNull { byId[it] }.take(6)
}

fun truncateName(name: String, maxLength: Int = 18): String {
    if (name.length <= maxLength) return name
    if (maxLength <= 1) return name.take(maxLength)
    return name.take(maxLength - 1) + "\u2026"
}


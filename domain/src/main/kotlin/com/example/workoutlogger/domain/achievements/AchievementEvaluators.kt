package com.example.workoutlogger.domain.achievements

import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.Progress
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.min

class FirstWorkoutEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val completed = ctx.workouts.any { it.sessions > 0 }
        val current = if (completed) 1.0 else 0.0
        return Progress(
            current = current,
            target = def.targetValue,
            percent = if (def.targetValue == 0.0) 1.0 else min(1.0, current / def.targetValue),
            unit = "workouts"
        )
    }
}

class WorkoutsPerWindowEvaluator(private val windowDays: Int) : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val today = ctx.toLocalDate(now)
        val start = today.minusDays(windowDays - 1)
        val total = ctx.workouts.filter { it.date in start..today }.sumOf { it.sessions }
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, total.toDouble() / def.targetValue)
        return Progress(
            current = total.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "workouts"
        )
    }
}

class StreakEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val today = ctx.toLocalDate(now)
        val workoutDates = ctx.workouts.filter { it.sessions > 0 }
            .map { it.date }
            .sortedDescending()
        var streak = 0
        var cursor = today
        val dates = workoutDates.toMutableList()
        while (dates.isNotEmpty()) {
            val date = dates.first()
            if (date == cursor) {
                streak += 1
                dates.removeAt(0)
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, streak.toDouble() / def.targetValue)
        return Progress(
            current = streak.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "days"
        )
    }
}

class TotalVolumeEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val volume = ctx.workouts.sumOf { it.totalVolumeKg }
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, volume / def.targetValue)
        return Progress(
            current = volume,
            target = def.targetValue,
            percent = percent,
            unit = "kg"
        )
    }
}

class VarietyBalanceEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val targetMask = WorkoutDailyCategory.PUSH or WorkoutDailyCategory.PULL or WorkoutDailyCategory.LEGS
        val end = ctx.toLocalDate(now)
        val start = end.minusDays((def.windowDays ?: 7) - 1)
        var unionMask = 0
        ctx.workouts.filter { it.date in start..end }.forEach { summary ->
            unionMask = unionMask or summary.categoryMask
        }
        val covered = unionMask and targetMask
        val count = Integer.bitCount(covered)
        val percent = min(1.0, count.toDouble() / def.targetValue)
        return Progress(
            current = count.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "categories"
        )
    }
}

class ScheduleAdherenceEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val windowDays = def.windowDays ?: 28
        val end = ctx.toLocalDate(now)
        val start = end.minusDays(windowDays - 1)
        val expected = ctx.schedules.sumOf { schedule ->
            schedule.expectedDates.count { it in start..end }
        }
        val completed = ctx.schedules.sumOf { schedule ->
            schedule.completedDates.count { it in start..end }
        }
        val ratio = if (expected == 0) 0.0 else completed.toDouble() / expected
        val percent = min(1.0, ratio / def.targetValue)
        return Progress(
            current = ratio,
            target = def.targetValue,
            percent = percent,
            unit = "%"
        )
    }
}

class EarlyBirdEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val windowDays = def.windowDays ?: 30
        val end = ctx.toLocalDate(now)
        val start = end.minusDays(windowDays - 1)
        val early = ctx.workouts.filter { it.date in start..end }.sumOf { it.earlySessions }
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, early.toDouble() / def.targetValue)
        return Progress(
            current = early.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "workouts"
        )
    }
}

class ComebackEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val ordered = ctx.workouts.sortedBy { it.date }
        if (ordered.isEmpty()) {
            return Progress(0.0, def.targetValue, 0.0, "workouts")
        }
        val nowDate = ctx.toLocalDate(now)
        val recent = ordered.filter { it.date <= nowDate }
        if (recent.isEmpty()) {
            return Progress(0.0, def.targetValue, 0.0, "workouts")
        }
        var lastActiveDate: LocalDate? = null
        var comebackCount = 0
        for (summary in recent) {
            if (summary.sessions > 0) {
                if (lastActiveDate != null) {
                    val gap = summary.date.daysSince(lastActiveDate!!)
                    if (gap >= 14) {
                        val end = summary.date.plusDays((def.windowDays ?: 7) - 1)
                        val count = ordered.filter { it.date in summary.date..end }.sumOf { it.sessions }
                        comebackCount = max(comebackCount, count)
                    }
                }
                lastActiveDate = summary.date
            }
        }
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, comebackCount.toDouble() / def.targetValue)
        return Progress(
            current = comebackCount.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "workouts"
        )
    }
}

class OneRmTargetEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val metadata = inst.metadata
        val exerciseName = metadata?.exerciseName ?: return Progress(0.0, def.targetValue, 0.0, "kg")
        val stats = ctx.exerciseStats[exerciseName.lowercase()] ?: return Progress(0.0, def.targetValue, 0.0, "kg")
        val best = stats.bestOneRmKg ?: stats.bestWeightKg ?: 0.0
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, best / def.targetValue)
        return Progress(
            current = best,
            target = def.targetValue,
            percent = percent,
            unit = "kg"
        )
    }
}

class FrequencyGoalEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val metadata = inst.metadata
        val windowDays = metadata?.windowDays ?: def.windowDays ?: 7
        val end = ctx.toLocalDate(now)
        val start = end.minusDays(windowDays - 1)
        val count = ctx.workouts.filter { it.date in start..end }.sumOf { it.sessions }
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, count.toDouble() / def.targetValue)
        return Progress(
            current = count.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "workouts"
        )
    }
}

class RepsAtWeightGoalEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val metadata = inst.metadata
        val exerciseName = metadata?.exerciseName ?: return Progress(0.0, def.targetValue, 0.0, "reps")
        val targetWeight = metadata?.secondaryTarget ?: 0.0
        val stats = ctx.exerciseStats[exerciseName.lowercase()] ?: return Progress(0.0, def.targetValue, 0.0, "reps")
        val performances = stats.performances.filter { it.weightKg >= targetWeight }
        val achievedReps = performances.maxOfOrNull { it.reps } ?: 0
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, achievedReps.toDouble() / def.targetValue)
        return Progress(
            current = achievedReps.toDouble(),
            target = def.targetValue,
            percent = percent,
            unit = "reps"
        )
    }
}

class BodyWeightRelationEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val metadata = inst.metadata
        val exerciseName = metadata?.exerciseName ?: return Progress(0.0, def.targetValue, 0.0, "kg")
        val stats = ctx.exerciseStats[exerciseName.lowercase()] ?: return Progress(0.0, def.targetValue, 0.0, "kg")
        val bodyWeight = ctx.userSettings.bodyWeightKg ?: return Progress(0.0, def.targetValue, 0.0, "kg")
        val best = stats.bestWeightKg ?: return Progress(0.0, def.targetValue, 0.0, "kg")
        val percent = if (bodyWeight == 0.0) 0.0 else min(1.0, best / bodyWeight)
        return Progress(
            current = best,
            target = bodyWeight,
            percent = percent,
            unit = "kg"
        )
    }
}

class StreakGoalEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val end = ctx.toLocalDate(now)
        val daysWithWorkouts = ctx.workouts.filter { it.sessions > 0 }.map { it.date }.toSet()
        var streak = 0
        var cursor = end
        while (daysWithWorkouts.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        val target = def.targetValue
        val percent = if (target == 0.0) 1.0 else min(1.0, streak.toDouble() / target)
        return Progress(
            current = streak.toDouble(),
            target = target,
            percent = percent,
            unit = "days"
        )
    }
}

class TimeUnderTensionEvaluator : Evaluator {
    override fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress {
        val metadata = inst.metadata
        val windowDays = metadata?.windowDays ?: def.windowDays ?: 7
        val end = ctx.toLocalDate(now)
        val start = end.minusDays(windowDays - 1)
        val minutes = ctx.workouts.filter { it.date in start..end }.sumOf { it.minutesActive }
        val seconds = minutes * 60.0
        val percent = if (def.targetValue == 0.0) 1.0 else min(1.0, seconds / def.targetValue)
        return Progress(
            current = seconds,
            target = def.targetValue,
            percent = percent,
            unit = "seconds"
        )
    }
}

private fun DataContext.toLocalDate(instant: Instant): LocalDate = instant.toLocalDateTime(timeZone).date

object WorkoutDailyCategory {
    const val PUSH = 1 shl 0
    const val PULL = 1 shl 1
    const val LEGS = 1 shl 2
}

fun LocalDate.daysSince(previous: LocalDate): Int = kotlin.math.abs(this.toEpochDays() - previous.toEpochDays())

private fun LocalDate.minusDays(days: Int): LocalDate =
    this.minus(days, DateTimeUnit.DAY)

private fun LocalDate.plusDays(days: Int): LocalDate =
    this.plus(days, DateTimeUnit.DAY)

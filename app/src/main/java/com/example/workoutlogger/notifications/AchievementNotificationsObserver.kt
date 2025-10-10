package com.example.workoutlogger.notifications

import com.example.workoutlogger.domain.repository.AchievementsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class AchievementNotificationsObserver @Inject constructor(
    private val achievementsRepository: AchievementsRepository,
    private val notificationHelper: AchievementNotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        notificationHelper.ensureChannel()
        scope.launch {
            achievementsRepository.observeEvents().collectLatest { event ->
                notificationHelper.showEvent(event)
            }
        }
    }
}

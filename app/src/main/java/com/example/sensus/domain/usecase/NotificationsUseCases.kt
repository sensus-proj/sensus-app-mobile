package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class NotificationsGetSettingsUseCase(
    private val repository: NotificationsRepository,
    private val session: SessionRepository
) {
    val sessionChanges: Flow<Unit> = session.sessionState.map { it.notificationIdentity() }.distinctUntilChanged().map { }

    suspend operator fun invoke(): Result<NotificationsPreferences> = withNotificationsSession(session) { repository.getSettings() }
}

class NotificationsUpdateSettingsUseCase(
    private val repository: NotificationsRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(settings: NotificationsPreferences): Result<NotificationsPreferences> = withNotificationsSession(session) {
        if (settings.radiusKm in NotificationsPreferences.MIN_RADIUS_KM..NotificationsPreferences.MAX_RADIUS_KM) {
            repository.updateSettings(settings)
        } else {
            Result.Failure(AppError.Validation())
        }
    }
}

class NotificationsRegisterDeviceUseCase(
    private val provider: NotificationsPushDeviceProvider,
    private val repository: NotificationsRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(): Result<Unit> = withNotificationsSession(session) { isCurrent ->
        when (val result = provider.getDevice()) {
            is Result.Failure -> result
            is Result.Success -> if (isCurrent()) repository.registerDevice(result.data) else Result.Failure(AppError.Unauthorized())
        }
    }
}

private fun SessionState.notificationIdentity(): Pair<String, String?>? =
    if (isSessionActive && !isGuest && !activeToken.isNullOrBlank()) activeToken to activeEmail else null

private suspend fun <T> withNotificationsSession(session: SessionRepository, operation: suspend (() -> Boolean) -> Result<T>): Result<T> {
    val identity = session.sessionState.value.notificationIdentity()
    return if (identity == null) {
        Result.Failure(AppError.Unauthorized())
    } else {
        val isCurrent = { session.sessionState.value.notificationIdentity() == identity }
        val result = operation(isCurrent)
        if (isCurrent()) result else Result.Failure(AppError.Unauthorized())
    }
}

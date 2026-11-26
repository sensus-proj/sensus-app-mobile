package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Mantiene registrado el token FCM de este telefono para el usuario registrado activo (FT-39).
 *
 * - Al iniciar sesion (o al abrir la app con sesion registrada) registra el dispositivo una vez.
 * - `onNewToken` llama a [sync] con el token nuevo.
 * - Si el backend responde 503 (FCM no disponible) o no hay red, reintenta con espera creciente.
 * - Invitados y sesiones cerradas nunca registran nada.
 */
class NotificationsDeviceSync(
    private val register: NotificationsRegisterDeviceUseCase,
    private val session: SessionRepository,
    private val permission: NotificationPermissionChecker,
    private val scope: CoroutineScope,
    private val retryDelaysMillis: List<Long> = DEFAULT_RETRY_DELAYS_MILLIS
) {
    companion object {
        val DEFAULT_RETRY_DELAYS_MILLIS = listOf(30_000L, 120_000L, 600_000L)
    }

    private var started = false
    private var pending: Job? = null

    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            session.sessionState.map { it.notificationIdentity() }.distinctUntilChanged().collect { identity ->
                if (identity == null) cancelPending() else sync()
            }
        }
    }

    /** Registra el dispositivo si hay sesion registrada y permiso de notificaciones. Sustituye un intento en curso. */
    @Synchronized
    fun sync(token: String? = null) {
        if (session.sessionState.value.notificationIdentity() == null || !permission.canPostNotifications()) return
        cancelPending()
        pending = scope.launch { registerWithRetry(token) }
    }

    private suspend fun registerWithRetry(token: String?) {
        var result = register(token)
        for (wait in retryDelaysMillis) {
            if (!result.isRetryable()) return
            delay(wait)
            result = register(token)
        }
    }

    @Synchronized
    private fun cancelPending() {
        pending?.cancel()
        pending = null
    }
}

internal fun Result<Unit>.isRetryable(): Boolean {
    val error = (this as? Result.Failure)?.error ?: return false
    return error is AppError.Network || (error is AppError.Generic && error.message == NotificationsRepository.PUSH_UNAVAILABLE)
}

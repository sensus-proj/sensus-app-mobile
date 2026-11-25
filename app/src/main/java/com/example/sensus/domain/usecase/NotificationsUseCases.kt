package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.NotificationsLocalStore
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
    private val session: SessionRepository,
    private val permission: NotificationPermissionChecker
) {
    /** [token] llega desde `onNewToken`; si es `null` se pide al SDK de Firebase. */
    suspend operator fun invoke(token: String? = null): Result<Unit> = withNotificationsSession(session) { isCurrent ->
        if (!permission.canPostNotifications()) return@withNotificationsSession permissionDenied()
        when (val result = provider.getDevice(token)) {
            is Result.Failure -> result
            is Result.Success -> when {
                !isCurrent() -> Result.Failure(AppError.Unauthorized())
                !permission.canPostNotifications() -> permissionDenied()
                else -> repository.registerDevice(result.data)
            }
        }
    }

    private fun permissionDenied(): Result<Nothing> = Result.Failure(AppError.Forbidden(PERMISSION_REQUIRED))

    companion object {
        const val PERMISSION_REQUIRED = "Notification permission required"
    }
}

/**
 * Ubicacion para "oferta nueva cerca de ti" (FT-39). Solo se comparte con consentimiento explicito, permiso de
 * ubicacion en primer plano y sesion registrada. El backend la descarta a las 24 h, por eso se renueva al abrir la app.
 */
class NotificationsLocationUseCase(
    private val repository: NotificationsRepository,
    private val session: SessionRepository,
    private val location: LocationRepository,
    private val store: NotificationsLocalStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    companion object {
        const val PERMISSION_REQUIRED = "Location permission required"
        const val LOCATION_UNAVAILABLE = "Location unavailable"
        const val REFRESH_INTERVAL_MILLIS = 6L * 60 * 60 * 1000
    }

    fun isEnabled(): Boolean = store.isLocationSharingEnabled()

    /** Renueva la ubicacion si el usuario la activo y han pasado [REFRESH_INTERVAL_MILLIS] desde el ultimo envio. */
    suspend fun refreshIfDue(): Result<Unit> {
        if (!store.isLocationSharingEnabled() || !location.hasLocationPermission()) return Result.Success(Unit)
        val elapsed = clock() - store.lastLocationSharedAtMillis()
        return if (elapsed in 0 until REFRESH_INTERVAL_MILLIS) Result.Success(Unit) else share()
    }

    /** Guarda el consentimiento y envia la ubicacion. Requiere que la UI ya haya obtenido el permiso de Android. */
    suspend fun enable(): Result<Unit> {
        if (!location.hasLocationPermission()) return Result.Failure(AppError.Forbidden(PERMISSION_REQUIRED))
        if (session.sessionState.value.notificationIdentity() == null) return Result.Failure(AppError.Unauthorized())
        store.setLocationSharingEnabled(true)
        return share()
    }

    /** Retira el consentimiento localmente de inmediato y pide al backend borrar la ubicacion. */
    suspend fun disable(): Result<Unit> {
        store.setLocationSharingEnabled(false)
        store.setLastLocationSharedAtMillis(0L)
        return withNotificationsSession(session) { repository.deleteLocation() }
    }

    private suspend fun share(): Result<Unit> = withNotificationsSession(session) {
        val current = location.getFreshLocation()
        if (!current.isShareable()) {
            Result.Failure(AppError.NotFound(LOCATION_UNAVAILABLE))
        } else {
            repository.shareLocation(current).also { if (it is Result.Success) store.setLastLocationSharedAtMillis(clock()) }
        }
    }
}

private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0

/** Descarta la ubicacion por defecto (sin GPS el repositorio devuelve un punto fijo) y coordenadas fuera de rango. */
internal fun UserLocation.isShareable(): Boolean = this != UserLocation() &&
    latitude in -MAX_LATITUDE..MAX_LATITUDE &&
    longitude in -MAX_LONGITUDE..MAX_LONGITUDE

internal fun SessionState.notificationIdentity(): Pair<String, String?>? =
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

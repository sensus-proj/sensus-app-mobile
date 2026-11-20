package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.model.UserLocation

interface NotificationsRepository {
    companion object {
        /** Mensaje de [com.example.sensus.common.AppError.Generic] cuando el backend responde 503 (FCM no disponible). */
        const val PUSH_UNAVAILABLE = "Push service unavailable"
    }

    suspend fun getSettings(): Result<NotificationsPreferences>
    suspend fun updateSettings(settings: NotificationsPreferences): Result<NotificationsPreferences>
    suspend fun registerDevice(device: NotificationsDevice): Result<Unit>
    suspend fun shareLocation(location: UserLocation): Result<Unit>
    suspend fun deleteLocation(): Result<Unit>
}

interface NotificationsPushDeviceProvider {
    companion object {
        const val CONFIGURATION_UNAVAILABLE = "Firebase Android resources unavailable"
    }

    /** Usa [token] si ya se conoce (por ejemplo, desde `onNewToken`); si es `null`, lo pide al SDK. */
    suspend fun getDevice(token: String? = null): Result<NotificationsDevice>
}

/**
 * Datos locales de notificaciones que pertenecen a la instalacion, no a la sesion.
 *
 * No contiene secretos ni datos personales: un UUID aleatorio, banderas y una marca de tiempo. Por eso vive fuera
 * de `SecurePreferences`, cuyo `clearAll()` se ejecuta en cada cierre de sesion y cambiaria el `deviceId`.
 */
interface NotificationsLocalStore {
    /** Identificador estable de esta instalacion (UUID). Se crea la primera vez y no cambia al cerrar sesion. */
    fun installationId(): String
    fun isLocationSharingEnabled(): Boolean
    fun setLocationSharingEnabled(enabled: Boolean)
    fun lastLocationSharedAtMillis(): Long
    fun setLastLocationSharedAtMillis(value: Long)
    fun wasPromptShown(prompt: NotificationsPrompt): Boolean
    fun markPromptShown(prompt: NotificationsPrompt)

    /** Olvida el consentimiento de ubicacion de la sesion. Conserva el identificador y los avisos de la instalacion. */
    fun clearUserScoped()
}

enum class NotificationsPrompt { NOTIFICATIONS, LOCATION }

interface NotificationPermissionChecker {
    /** `true` si Android permite mostrar notificaciones (en Android 12 o menor no hace falta permiso). */
    fun canPostNotifications(): Boolean

    /** `true` si en este Android hay que pedir `POST_NOTIFICATIONS` (13+) y aun no se concede. */
    fun needsNotificationPermission(): Boolean = !canPostNotifications()
}

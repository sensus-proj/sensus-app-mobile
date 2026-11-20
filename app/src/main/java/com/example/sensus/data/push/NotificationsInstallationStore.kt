package com.example.sensus.data.push

import android.content.Context
import android.content.SharedPreferences
import com.example.sensus.domain.repository.NotificationsLocalStore
import com.example.sensus.domain.repository.NotificationsPrompt
import java.util.UUID

/**
 * Datos de notificaciones ligados a la instalacion (FT-39).
 *
 * Archivo propio, separado de `SecurePreferences`: `SecurePreferences.clearAll()` corre en cada cierre de sesion
 * y en cada 401, y el `deviceId` debe sobrevivir a ambos. Aqui no hay secretos ni datos personales (UUID aleatorio,
 * banderas y una marca de tiempo); el token FCM y el JWT nunca se guardan en este archivo. `allowBackup=false`
 * en el manifiesto evita que el UUID se copie a otro telefono.
 */
class NotificationsInstallationStore internal constructor(preferencesFactory: () -> SharedPreferences) : NotificationsLocalStore {

    constructor(context: Context) : this({ context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE) })

    private val prefs: SharedPreferences by lazy(preferencesFactory)
    private val lock = Any()

    override fun installationId(): String = synchronized(lock) {
        prefs.getString(KEY_INSTALLATION_ID, null)?.takeIf { it.isValidId() }
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_INSTALLATION_ID, it).apply() }
    }

    override fun isLocationSharingEnabled(): Boolean = prefs.getBoolean(KEY_LOCATION_ENABLED, false)

    override fun setLocationSharingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply()
    }

    override fun lastLocationSharedAtMillis(): Long = prefs.getLong(KEY_LOCATION_SHARED_AT, 0L)

    override fun setLastLocationSharedAtMillis(value: Long) {
        prefs.edit().putLong(KEY_LOCATION_SHARED_AT, value).apply()
    }

    override fun wasPromptShown(prompt: NotificationsPrompt): Boolean = prefs.getBoolean(prompt.key(), false)

    override fun markPromptShown(prompt: NotificationsPrompt) {
        prefs.edit().putBoolean(prompt.key(), true).apply()
    }

    override fun clearUserScoped() {
        prefs.edit()
            .remove(KEY_LOCATION_ENABLED)
            .remove(KEY_LOCATION_SHARED_AT)
            .apply()
    }

    private fun NotificationsPrompt.key(): String = KEY_PROMPT_PREFIX + name.lowercase()

    private fun String.isValidId(): Boolean = length in 1..MAX_DEVICE_ID_LENGTH && isNotBlank()

    private companion object {
        const val FILE_NAME = "sensus_push_installation"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_LOCATION_ENABLED = "nearby_location_enabled"
        const val KEY_LOCATION_SHARED_AT = "nearby_location_shared_at"
        const val KEY_PROMPT_PREFIX = "prompt_shown_"
        const val MAX_DEVICE_ID_LENGTH = 100
    }
}

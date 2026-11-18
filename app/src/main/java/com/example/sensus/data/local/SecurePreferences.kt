package com.example.sensus.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class SessionType {
    UNAUTHENTICATED,
    GUEST,
    REGISTERED
}

data class StoredMerchantStatus(
    val isRegistered: Boolean = false,
    val storeName: String? = null,
    val address: String? = null,
    val postalCode: String? = null,
    val photoUrl: String? = null,
    val isStoreOpen: Boolean = true
)

class SecurePreferences private constructor(preferencesFactory: () -> SharedPreferences) {

    constructor(context: Context) : this({ buildPreferences(context) })

    /** Solo para pruebas unitarias con un [SharedPreferences] en memoria. */
    internal constructor(preferences: SharedPreferences) : this({ preferences })

    private val prefs: SharedPreferences by lazy(preferencesFactory)

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun saveUserSession(name: String, email: String, photoUrl: String? = null) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHOTO, photoUrl ?: "")
            .apply()
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserPhoto(): String? = prefs.getString(KEY_USER_PHOTO, null)?.takeIf { it.isNotBlank() }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun saveSessionType(type: SessionType) {
        prefs.edit().putString(KEY_SESSION_TYPE, type.name).apply()
    }

    fun getSessionType(): SessionType {
        val name = prefs.getString(KEY_SESSION_TYPE, SessionType.UNAUTHENTICATED.name)
        return try {
            SessionType.valueOf(name ?: SessionType.UNAUTHENTICATED.name)
        } catch (_: Exception) {
            SessionType.UNAUTHENTICATED
        }
    }

    fun isGuest(): Boolean = getSessionType() == SessionType.GUEST

    fun saveGuestSession(token: String, maxRadiusMeters: Int = DEFAULT_GUEST_RADIUS_METERS) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_SESSION_TYPE, SessionType.GUEST.name)
            .putInt(KEY_GUEST_RADIUS, maxRadiusMeters)
            .apply()
    }

    fun getGuestRadiusMeters(): Int = prefs.getInt(KEY_GUEST_RADIUS, DEFAULT_GUEST_RADIUS_METERS)

    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    var storedMerchantStatus: StoredMerchantStatus
        get() = StoredMerchantStatus(
            isRegistered = prefs.getBoolean(KEY_MERCHANT_REGISTERED, false),
            storeName = prefs.getString(KEY_MERCHANT_STORE_NAME, null),
            address = prefs.getString(KEY_MERCHANT_ADDRESS, null),
            postalCode = prefs.getString(KEY_MERCHANT_POSTAL_CODE, null),
            photoUrl = prefs.getString(KEY_MERCHANT_PHOTO_URL, null),
            isStoreOpen = prefs.getBoolean(KEY_MERCHANT_STORE_OPEN, true)
        )
        set(value) {
            prefs.edit().apply {
                putBoolean(KEY_MERCHANT_REGISTERED, value.isRegistered)
                val normalizedName = value.storeName?.trim()?.takeIf { it.isNotBlank() }
                if (value.isRegistered && normalizedName != null) {
                    putString(KEY_MERCHANT_STORE_NAME, normalizedName)
                    putBoolean(KEY_MERCHANT_STORE_OPEN, value.isStoreOpen)
                    value.address?.trim()?.takeIf { it.isNotBlank() }?.let { putString(KEY_MERCHANT_ADDRESS, it) }
                    value.postalCode?.trim()?.takeIf { it.isNotBlank() }?.let { putString(KEY_MERCHANT_POSTAL_CODE, it) }
                    value.photoUrl?.trim()?.takeIf { it.isNotBlank() }?.let { putString(KEY_MERCHANT_PHOTO_URL, it) }
                } else {
                    remove(KEY_MERCHANT_STORE_NAME)
                    remove(KEY_MERCHANT_ADDRESS)
                    remove(KEY_MERCHANT_POSTAL_CODE)
                    remove(KEY_MERCHANT_PHOTO_URL)
                    remove(KEY_MERCHANT_STORE_OPEN)
                }
                apply()
            }
        }

    /**
     * Borra los datos de sesión y de usuario, pero **conserva la clave de cifrado de la base SQLCipher**
     * ([KEY_DB_PASSPHRASE]). Si se borrara, el siguiente arranque generaría una clave nueva y el archivo
     * `sensus_encrypted.db`, aún cifrado con la anterior, dejaría de abrirse ("file is not a database").
     */
    fun clearAll() {
        val databasePassphrase = prefs.getString(KEY_DB_PASSPHRASE, null)
        prefs.edit().apply {
            clear()
            if (!databasePassphrase.isNullOrBlank()) {
                putString(KEY_DB_PASSPHRASE, databasePassphrase)
            }
        }.apply()
    }

    companion object {
        /** Clave bajo la que se guarda la frase de paso de SQLCipher. Nunca debe borrarse con la sesión. */
        const val KEY_DB_PASSPHRASE = "sensus_db_encrypted_passphrase"

        private fun buildPreferences(context: Context): SharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback para entornos sin Keystore / pruebas
            context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        }

        private const val DEFAULT_GUEST_RADIUS_METERS = 2000
        private const val PREFS_FILENAME = "sensus_secure_prefs"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_SESSION_TYPE = "key_session_type"
        private const val KEY_GUEST_RADIUS = "key_guest_radius"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_PHOTO = "key_user_photo"
        private const val KEY_MERCHANT_REGISTERED = "key_merchant_registered"
        private const val KEY_MERCHANT_STORE_NAME = "key_merchant_store_name"
        private const val KEY_MERCHANT_ADDRESS = "key_merchant_address"
        private const val KEY_MERCHANT_POSTAL_CODE = "key_merchant_postal_code"
        private const val KEY_MERCHANT_PHOTO_URL = "key_merchant_photo_url"
        private const val KEY_MERCHANT_STORE_OPEN = "key_merchant_store_open"
    }
}

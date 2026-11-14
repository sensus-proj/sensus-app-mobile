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
    val postalCode: String? = null
)

class SecurePreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
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
    }

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
            postalCode = prefs.getString(KEY_MERCHANT_POSTAL_CODE, null)
        )
        set(value) {
            prefs.edit().apply {
                putBoolean(KEY_MERCHANT_REGISTERED, value.isRegistered)
                val normalizedName = value.storeName?.trim()?.takeIf { it.isNotBlank() }
                if (value.isRegistered && normalizedName != null) {
                    putString(KEY_MERCHANT_STORE_NAME, normalizedName)
                    value.address?.trim()?.takeIf { it.isNotBlank() }?.let { putString(KEY_MERCHANT_ADDRESS, it) }
                    value.postalCode?.trim()?.takeIf { it.isNotBlank() }?.let { putString(KEY_MERCHANT_POSTAL_CODE, it) }
                } else {
                    remove(KEY_MERCHANT_STORE_NAME)
                    remove(KEY_MERCHANT_ADDRESS)
                    remove(KEY_MERCHANT_POSTAL_CODE)
                }
                apply()
            }
        }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
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
    }
}

package com.example.sensus.data.local.security

import android.util.Base64
import com.example.sensus.data.local.SecurePreferences
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de clave de cifrado para SQLCipher (AES-256).
 * Almacena y recupera una clave criptográficamente segura de 256 bits (32 bytes)
 * respaldada por Android Keystore a través de [SecurePreferences].
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    private val securePreferences: SecurePreferences
) {
    companion object {
        private const val KEY_DB_PASSPHRASE = "sensus_db_encrypted_passphrase"
        private const val PASSPHRASE_LENGTH_BYTES = 32 // 256 bits
    }

    /**
     * Obtiene o genera una frase de paso segura en bytes (AES-256).
     * No expone cadenas String planas persistentes para prevenir filtración en memoria.
     */
    fun getOrCreatePassphrase(): ByteArray {
        val existingBase64 = securePreferences.getString(KEY_DB_PASSPHRASE)
        if (!existingBase64.isNullOrBlank()) {
            return Base64.decode(existingBase64, Base64.NO_WRAP)
        }

        val randomBytes = ByteArray(PASSPHRASE_LENGTH_BYTES)
        SecureRandom().nextBytes(randomBytes)
        val encoded = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
        securePreferences.saveString(KEY_DB_PASSPHRASE, encoded)
        return randomBytes
    }
}

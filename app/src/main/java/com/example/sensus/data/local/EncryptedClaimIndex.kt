package com.example.sensus.data.local

import androidx.security.crypto.EncryptedSharedPreferences

class EncryptedClaimIndex internal constructor(private val encryptedPreferences: () -> EncryptedSharedPreferences?) {
    fun rememberClaimId(account: String, offerId: String, claimId: String) {
        try {
            encryptedPreferences()?.edit()?.putString("claim:$offerId:$account", claimId)?.apply()
        } catch (_: RuntimeException) {
            // A cache failure must not turn a confirmed server reservation into a retry.
        }
    }

    fun findClaimId(account: String, offerId: String): String? = try {
        encryptedPreferences()?.getString("claim:$offerId:$account", null)
    } catch (_: RuntimeException) {
        null
    }
}

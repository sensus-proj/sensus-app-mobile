package com.example.sensus.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClaimIndexEncryptionTest {
    @Test
    fun confirmedIdSurvivesReopeningAndIsClearedWithTheSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = "test_claim_index_" + UUID.randomUUID()
        try {
            open(context, file).claimIndex.rememberClaimId("buyer", "offer", "claim")
            val reopened = open(context, file)
            assertEquals("claim", reopened.claimIndex.findClaimId("buyer", "offer"))
            assertNull(reopened.claimIndex.findClaimId("another-buyer", "offer"))
            reopened.clearAll()
            assertNull(open(context, file).claimIndex.findClaimId("buyer", "offer"))
        } finally {
            context.deleteSharedPreferences(file)
        }
    }

    private fun open(context: Context, file: String): SecurePreferences {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val encrypted = EncryptedSharedPreferences.create(
            context,
            file,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return SecurePreferences(encrypted)
    }
}

package com.example.sensus.data.local

import android.util.Base64
import com.example.sensus.data.local.security.DatabaseKeyManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DatabaseKeyManagerTest {

    private lateinit var securePreferences: SecurePreferences
    private lateinit var keyManager: DatabaseKeyManager

    @Before
    fun setUp() {
        securePreferences = mockk(relaxed = true)
        keyManager = DatabaseKeyManager(securePreferences)
        mockkStatic(Base64::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun getOrCreatePassphrase_shouldGenerateAndSave_whenNoKeyExists() {
        every { securePreferences.getString("sensus_db_encrypted_passphrase") } returns null
        every { Base64.encodeToString(any(), Base64.NO_WRAP) } returns "mocked_base64_key"

        val passphrase = keyManager.getOrCreatePassphrase()

        assertNotNull(passphrase)
        assertEquals(32, passphrase.size)
    }

    @Test
    fun getOrCreatePassphrase_shouldReturnExisting_whenKeyPresent() {
        val existingBytes = ByteArray(32) { it.toByte() }
        every { securePreferences.getString("sensus_db_encrypted_passphrase") } returns "existing_base64"
        every { Base64.decode("existing_base64", Base64.NO_WRAP) } returns existingBytes

        val passphrase = keyManager.getOrCreatePassphrase()

        assertEquals(existingBytes, passphrase)
    }
}

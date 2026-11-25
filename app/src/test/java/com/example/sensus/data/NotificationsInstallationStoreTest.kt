package com.example.sensus.data

import android.content.SharedPreferences
import com.example.sensus.data.push.NotificationsInstallationStore
import com.example.sensus.domain.repository.NotificationsPrompt
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsInstallationStoreTest {
    private val preferences = InMemoryPreferences()
    private fun store() = NotificationsInstallationStore { preferences }

    @Test
    fun installationIdIsAUuidAndStableAcrossInstances() {
        val first = store().installationId()
        assertEquals(first, UUID.fromString(first).toString())
        assertEquals(first, store().installationId())
        assertTrue(first.length <= 100)
    }

    @Test
    fun clearUserScopedForgetsConsentAndKeepsInstallationPrompts() {
        val store = store()
        val id = store.installationId()
        store.setLocationSharingEnabled(true)
        store.setLastLocationSharedAtMillis(42L)
        store.markPromptShown(NotificationsPrompt.LOCATION)
        store.markPromptShown(NotificationsPrompt.NOTIFICATIONS)

        store.clearUserScoped()

        assertEquals(id, store.installationId())
        assertFalse(store.isLocationSharingEnabled())
        assertEquals(0L, store.lastLocationSharedAtMillis())
        assertTrue(store().wasPromptShown(NotificationsPrompt.LOCATION))
        assertTrue(store().wasPromptShown(NotificationsPrompt.NOTIFICATIONS))
    }

    @Test
    fun corruptedIdIsReplaced() {
        preferences.data["installation_id"] = ""
        val id = store().installationId()
        assertEquals(id, UUID.fromString(id).toString())
    }

    /** SharedPreferences en memoria para pruebas JVM. */
    private class InMemoryPreferences : SharedPreferences {
        val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearFirst = false

            override fun putString(key: String, value: String?) = apply { pending[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
            override fun putInt(key: String, value: Int) = apply { pending[key] = value }
            override fun putLong(key: String, value: Long) = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
            override fun remove(key: String) = apply { pending[key] = null }
            override fun clear() = apply { clearFirst = true }
            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearFirst) data.clear()
                pending.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
            }
        }
    }
}

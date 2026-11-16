package com.example.sensus.data

import android.content.SharedPreferences
import com.example.sensus.data.local.SecurePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regresión de docs/errores-conocidos.md (E-02): cerrar sesión o recibir un 401 llama a `clearAll()`. Si ese
 * método borra la frase de paso de SQLCipher, el siguiente arranque genera una clave nueva y la base local
 * deja de abrirse ("file is not a database") y la app se cierra al entrar a Sensi.
 */
class SecurePreferencesClearAllTest {

    private val store = InMemoryPreferences()
    private val prefs = SecurePreferences(store)

    @Test
    fun clearAll_preservesDatabasePassphrase() {
        prefs.saveString(SecurePreferences.KEY_DB_PASSPHRASE, "clave-sqlcipher-base64")
        prefs.saveAuthToken("jwt")
        prefs.saveUserSession("Rodrigo", "r@example.com")

        prefs.clearAll()

        assertEquals("clave-sqlcipher-base64", prefs.getString(SecurePreferences.KEY_DB_PASSPHRASE))
    }

    @Test
    fun clearAll_stillWipesSessionAndUserData() {
        prefs.saveString(SecurePreferences.KEY_DB_PASSPHRASE, "clave")
        prefs.saveAuthToken("jwt")
        prefs.saveUserSession("Rodrigo", "r@example.com")

        prefs.clearAll()

        assertNull(prefs.getAuthToken())
        assertNull(prefs.getUserName())
        assertNull(prefs.getUserEmail())
    }

    @Test
    fun clearAll_withoutPassphrase_doesNotInventOne() {
        prefs.saveAuthToken("jwt")

        prefs.clearAll()

        assertNull(prefs.getString(SecurePreferences.KEY_DB_PASSPHRASE))
    }

    @Test
    fun clearAll_repeatedly_keepsTheSamePassphrase() {
        prefs.saveString(SecurePreferences.KEY_DB_PASSPHRASE, "clave")

        repeat(3) { prefs.clearAll() }

        assertEquals("clave", prefs.getString(SecurePreferences.KEY_DB_PASSPHRASE))
    }

    /** SharedPreferences en memoria: evita depender de Android en pruebas JVM. */
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

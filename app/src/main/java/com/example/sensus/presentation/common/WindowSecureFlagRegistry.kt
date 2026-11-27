package com.example.sensus.presentation.common

import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import java.util.WeakHashMap

internal object WindowSecureFlagRegistry {
    private data class Entry(val count: Int, val preExistingSecure: Boolean)

    private val entries = WeakHashMap<Window, Entry>()

    @Synchronized
    fun acquire(window: Window?) {
        if (window == null) return
        val existing = entries[window]
        if (existing == null) {
            val alreadySecured = (window.attributes.flags and FLAG_SECURE) != 0
            entries[window] = Entry(count = 1, preExistingSecure = alreadySecured)
            if (!alreadySecured) window.addFlags(FLAG_SECURE)
        } else {
            entries[window] = existing.copy(count = existing.count + 1)
        }
    }

    @Synchronized
    fun release(window: Window?) {
        if (window == null) return
        val existing = entries[window] ?: return
        if (existing.count > 1) {
            entries[window] = existing.copy(count = existing.count - 1)
        } else {
            entries.remove(window)
            if (!existing.preExistingSecure) window.clearFlags(FLAG_SECURE)
        }
    }
}

package com.example.sensus.presentation.common

import android.view.Window
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SecureScreenEffectTest {

    @Test
    fun acquireAndRelease_tracksNestedReferencesOnWindow() {
        val attrs = LayoutParams()
        val window = mockk<Window>(relaxed = true) {
            every { attributes } returns attrs
            every { addFlags(FLAG_SECURE) } answers { attrs.flags = attrs.flags or FLAG_SECURE }
            every { clearFlags(FLAG_SECURE) } answers { attrs.flags = attrs.flags and FLAG_SECURE.inv() }
        }

        WindowSecureFlagRegistry.acquire(window)
        verify(exactly = 1) { window.addFlags(FLAG_SECURE) }
        assertEquals(FLAG_SECURE, attrs.flags and FLAG_SECURE)

        WindowSecureFlagRegistry.acquire(window)
        verify(exactly = 1) { window.addFlags(FLAG_SECURE) }

        WindowSecureFlagRegistry.release(window)
        verify(exactly = 0) { window.clearFlags(FLAG_SECURE) }
        assertEquals(FLAG_SECURE, attrs.flags and FLAG_SECURE)

        WindowSecureFlagRegistry.release(window)
        verify(exactly = 1) { window.clearFlags(FLAG_SECURE) }
        assertEquals(0, attrs.flags and FLAG_SECURE)
    }

    @Test
    fun preExistingSecureFlag_isPreservedWhenLastReferenceReleases() {
        val attrs = LayoutParams().apply { flags = FLAG_SECURE }
        val window = mockk<Window>(relaxed = true) {
            every { attributes } returns attrs
        }

        WindowSecureFlagRegistry.acquire(window)
        WindowSecureFlagRegistry.release(window)

        verify(exactly = 0) { window.addFlags(any()) }
        verify(exactly = 0) { window.clearFlags(any()) }
    }

    @Test
    fun nullWindow_isSafeNoOp() {
        WindowSecureFlagRegistry.acquire(null)
        WindowSecureFlagRegistry.release(null)
    }
}

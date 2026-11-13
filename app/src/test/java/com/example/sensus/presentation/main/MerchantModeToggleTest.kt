package com.example.sensus.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantModeToggleTest {

    @Test
    fun registeredMerchant_canSwitchBackAndForthBetweenBuyerAndMerchant() {
        val state = MainAppState().apply {
            isMerchantRegistered = true
            isMerchantMode = true
        }

        toggleMerchantMode(state, isGuest = false, enableMerchant = false)
        assertFalse(state.isMerchantMode)

        state.merchantTabIndex = 2
        toggleMerchantMode(state, isGuest = false, enableMerchant = true)
        assertTrue(state.isMerchantMode)
        assertEquals(0, state.merchantTabIndex)
        assertFalse(state.showMerchantRegister)

        toggleMerchantMode(state, isGuest = false, enableMerchant = false)
        toggleMerchantMode(state, isGuest = false, enableMerchant = true)
        assertTrue(state.isMerchantMode)
    }

    @Test
    fun unregisteredUser_enablingMerchant_opensRegistration() {
        val state = MainAppState()

        toggleMerchantMode(state, isGuest = false, enableMerchant = true)

        assertTrue(state.showMerchantRegister)
        assertFalse(state.isMerchantMode)
    }

    @Test
    fun guest_enablingMerchant_showsGuestPrompt() {
        val state = MainAppState()

        toggleMerchantMode(state, isGuest = true, enableMerchant = true)

        assertTrue(state.showGuestPrompt)
        assertFalse(state.isMerchantMode)
        assertFalse(state.showMerchantRegister)
    }
}

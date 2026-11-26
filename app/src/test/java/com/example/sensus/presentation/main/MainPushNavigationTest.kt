package com.example.sensus.presentation.main

import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.sensus.R
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.NotificationsPrompt
import com.example.sensus.presentation.push.PushViewEffect
import com.example.sensus.presentation.push.PushViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** FT-39: el toque de una notificacion deja visible su destino en la pantalla principal. */
class MainPushNavigationTest {
    private val context = mockk<Context>(relaxed = true)
    private val offer = ProductOffer("offer-1", "Pan", "Panaderia", "Panaderia", 30.0, 15.0, 50, 3, 200)

    @Before
    fun setup() {
        mockkStatic(Toast::class)
        mockkStatic(ContextCompat::class)
        val toast = mockk<Toast> { every { show() } just runs }
        every { Toast.makeText(any<Context>(), any<Int>(), any()) } returns toast
    }

    @After
    fun teardown() {
        unmockkStatic(Toast::class)
        unmockkStatic(ContextCompat::class)
    }

    private fun stateWithOverlays() = MainAppState().apply {
        isMerchantMode = true
        showNotifications = true
        activeRouteOffer = offer
        showSavedInterests = true
        showCartSheet = true
        selectedOfferForEdit = offer
        showMerchantRegister = true
    }

    @Test
    fun offerNotificationClosesOverlaysAndOpensDetailInBuyerMode() {
        val state = stateWithOverlays()
        applyPushNavigation(state, PushViewEffect.OpenOffer(offer), context)
        assertSame(offer, state.selectedProductForDetail)
        assertFalse(state.isMerchantMode)
        assertFalse(state.showNotifications)
        assertNull(state.activeRouteOffer)
        assertFalse(state.showSavedInterests)
        assertFalse(state.showCartSheet)
        assertNull(state.selectedOfferForEdit)
        assertFalse(state.showMerchantRegister)
    }

    @Test
    fun claimNotificationOpensTheExistingClaim() {
        val state = stateWithOverlays()
        applyPushNavigation(state, PushViewEffect.OpenClaim(offer), context)
        assertSame(offer, state.activeApartadoOffer)
        assertTrue(state.activeClaimExpected)
        assertNull(state.selectedProductForDetail)
    }

    @Test
    fun friendsNotificationOpensCommunityFriendsTab() {
        val state = MainAppState()
        val before = state.openFriendsTabSignal
        applyPushNavigation(state, PushViewEffect.OpenFriends, context)
        assertEquals(TAB_COMMUNITY, state.buyerTabIndex)
        assertEquals(before + 1, state.openFriendsTabSignal)
    }

    @Test
    fun merchantNotificationOpensSalesOnlyForActiveMerchants() {
        val buyer = MainAppState()
        applyPushNavigation(buyer, PushViewEffect.OpenMerchantClaims, context)
        assertFalse(buyer.isMerchantMode)

        val merchant = MainAppState().apply {
            isMerchantRegistered = true
            showNotifications = true
        }
        applyPushNavigation(merchant, PushViewEffect.OpenMerchantClaims, context)
        assertTrue(merchant.isMerchantMode)
        assertEquals(MERCHANT_TAB_SALES, merchant.merchantTabIndex)
        assertFalse(merchant.showNotifications)
    }

    @Test
    fun guestAndMissingOfferAreReportedWithoutNavigating() {
        val state = MainAppState()
        applyPushNavigation(state, PushViewEffect.GuestRequired, context)
        assertTrue(state.showGuestPrompt)
        applyPushNavigation(state, PushViewEffect.OfferUnavailable, context)
        verify { Toast.makeText(context, R.string.push_offer_unavailable, Toast.LENGTH_SHORT) }
        applyPushNavigation(state, PushViewEffect.RequestLocationPermission, context)
        assertNull(state.selectedProductForDetail)
    }

    @Test
    fun rationaleAndPermissionMapping() {
        assertEquals(NotificationsPrompt.NOTIFICATIONS, PushViewState(showNotificationRationale = true, showLocationRationale = true).activePrompt())
        assertEquals(NotificationsPrompt.LOCATION, PushViewState(showLocationRationale = true).activePrompt())
        assertNull(PushViewState().activePrompt())

        assertEquals(R.string.push_rationale_title, NotificationsPrompt.NOTIFICATIONS.rationaleTexts().title)
        assertEquals(R.string.push_location_accept, NotificationsPrompt.LOCATION.rationaleTexts().confirm)

        assertEquals(NotificationsPrompt.NOTIFICATIONS, PushViewEffect.RequestNotificationPermission.permissionPrompt())
        assertEquals(NotificationsPrompt.LOCATION, PushViewEffect.RequestLocationPermission.permissionPrompt())
        assertNull(PushViewEffect.OpenFriends.permissionPrompt())
    }

    @Test
    fun grantedPermissionChecks() {
        // En la JVM de pruebas SDK_INT es 0 (< Android 13): no hace falta POST_NOTIFICATIONS.
        assertTrue(context.isGranted(NotificationsPrompt.NOTIFICATIONS))
        every { ContextCompat.checkSelfPermission(context, any()) } returns android.content.pm.PackageManager.PERMISSION_DENIED
        assertFalse(context.isGranted(NotificationsPrompt.LOCATION))
        every { ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns
            android.content.pm.PackageManager.PERMISSION_GRANTED
        assertTrue(context.isGranted(NotificationsPrompt.LOCATION))
    }
}

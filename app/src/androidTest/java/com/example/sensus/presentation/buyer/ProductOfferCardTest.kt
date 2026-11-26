package com.example.sensus.presentation.buyer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sensus.R
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductOfferCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cardOnlyShowsVerificationAndStatusFromTheMerchantProfile() {
        val offer = ProductOffer("offer", "Pan", "Tienda", "Panadería", 10.0, 5.0, 50, 2, 100)
        val profile = mutableStateOf<MerchantProfile?>(null)
        compose.setContent { SensusTheme { ProductOfferCard(offer.copy(merchantProfile = profile.value), {}) } }
        compose.onNodeWithText("Verificado").assertDoesNotExist()
        compose.onNodeWithText("Cerrado ahora").assertDoesNotExist()
        compose.runOnIdle {
            profile.value = MerchantProfile(nombreComercial = "Tienda", direccion = "", codigoPostal = "", activo = false, isVerified = true)
        }
        compose.onNodeWithText("Verificado").assertExists()
        compose.onNodeWithText("Cerrado ahora").assertExists()
    }

    @Test
    fun compactOffer_keepsPriceVisibleAndSaveIndependentFromOpeningDetails() {
        val saved = mutableStateOf(false)
        var opened = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val offer = ProductOffer(
            id = "bread",
            name = "Pan de masa madre",
            storeName = "Panadería Central",
            category = "Panadería",
            originalPrice = 100.0,
            discountPrice = 60.0,
            discountPercent = 40,
            unitsLeft = 2,
            distanceMeters = 350
        )

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.5f)) {
                SensusTheme {
                    Box(Modifier.width(320.dp)) {
                        ProductOfferCard(offer, { opened++ }, isSaved = saved.value, onSaveClick = { saved.value = !saved.value })
                    }
                }
            }
        }

        compose.onNodeWithText("$60.00 MXN").assertHasClickAction()
        compose.onNodeWithContentDescription(context.getString(R.string.saved_interests_pin_add)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.saved_interests_pin_saved)).assertExists()
        compose.runOnIdle { assertEquals(0, opened) }
        compose.onNodeWithText(offer.name).performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}

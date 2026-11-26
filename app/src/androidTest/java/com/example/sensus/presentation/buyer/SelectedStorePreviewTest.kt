package com.example.sensus.presentation.buyer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.domain.model.MapStoreCluster
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** FT-11 T11.15: P-09 merchant preview shown when a map marker is selected. */
@RunWith(AndroidJUnit4::class)
class SelectedStorePreviewTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun preview_showsStoreAndAtMostTwoOffers() {
        compose.setContent {
            SensusTheme { SelectedStoreBottomCard(cluster = cluster(), actions = actions()) }
        }

        compose.onNodeWithText("Panaderia Norte").assertExists()
        compose.onNodeWithText("450 m", substring = true).assertExists()
        compose.onNodeWithText("Pan dulce").assertExists()
        compose.onNodeWithText("Bolillo").assertExists()
        compose.onNodeWithText("Concha").assertDoesNotExist()
    }

    @Test
    fun preview_actionsReachCatalogAndClose() {
        var opened: MapStoreCluster? = null
        var closed = 0
        val data = cluster()
        compose.setContent {
            SensusTheme {
                SelectedStoreBottomCard(
                    cluster = data,
                    actions = actions(onClose = { closed++ }, onViewMore = { opened = it })
                )
            }
        }

        compose.onNodeWithText("Ver más").performClick()
        compose.onNodeWithContentDescription("Cerrar").performClick()

        compose.runOnIdle {
            assertEquals(data.storeKey, opened?.storeKey)
            assertEquals(1, closed)
        }
    }

    private fun actions(onClose: () -> Unit = {}, onViewMore: (MapStoreCluster) -> Unit = {}) =
        SelectedStoreCardActions(onClose = onClose, onRouteClick = {}, onViewMoreClick = onViewMore)

    private fun cluster(): MapStoreCluster {
        val offers = listOf("Pan dulce", "Bolillo", "Concha").mapIndexed { index, name -> offer("o$index", name) }
        return MapStoreCluster(
            storeKey = "store-1",
            merchantId = "m-1",
            storeName = "Panaderia Norte",
            address = "Calle 5",
            postalCode = "",
            latitude = 19.0,
            longitude = -99.0,
            distanceMeters = 450,
            merchantProfile = null,
            matchingOffers = offers,
            allStoreOffers = offers,
            initialCategory = StoreCategory.FOOD
        )
    }

    private fun offer(id: String, name: String) = ProductOffer(
        id = id,
        name = name,
        storeName = "Panaderia Norte",
        category = "Comida",
        originalPrice = 20.0,
        discountPrice = 10.0,
        discountPercent = 50,
        unitsLeft = 4,
        distanceMeters = 450
    )
}

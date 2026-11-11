package com.example.sensus.data

import com.example.sensus.data.cart.CartManager
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.domain.model.AddToCartResult
import com.example.sensus.domain.model.ModerationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CartAndModerationTest {

    private val offerStoreA1 = ProductOffer(
        id = "off_a1",
        name = "Detergente 1 L",
        storeName = "Bodega A.",
        category = "Limpieza y Hogar",
        originalPrice = 100.0,
        discountPrice = 50.0,
        discountPercent = 50,
        unitsLeft = 5,
        distanceMeters = 200,
        couponCode = "SNS-A1-50"
    )

    private val offerStoreA2 = ProductOffer(
        id = "off_a2",
        name = "Refresco 2 L",
        storeName = "Bodega A.",
        category = "Bebidas",
        originalPrice = 40.0,
        discountPrice = 20.0,
        discountPercent = 50,
        unitsLeft = 10,
        distanceMeters = 200,
        couponCode = "SNS-A2-20"
    )

    private val offerStoreB = ProductOffer(
        id = "off_b1",
        name = "Arroz 1 kg",
        storeName = "Abarrotes B.",
        category = "Abarrotes",
        originalPrice = 50.0,
        discountPrice = 25.0,
        discountPercent = 50,
        unitsLeft = 3,
        distanceMeters = 400,
        couponCode = "SNS-B1-25"
    )

    @Before
    fun setUp() {
        CartManager.clearCart()
    }

    // ==========================================
    // CartManager Tests (Mismo comercio y límites)
    // ==========================================

    @Test
    fun addToCart_shouldAddItems_fromSameStore() {
        val res1 = CartManager.addToCart(offerStoreA1, 2)
        assertTrue(res1 is AddToCartResult.Success)

        val res2 = CartManager.addToCart(offerStoreA2, 3)
        assertTrue(res2 is AddToCartResult.Success)

        val cart = CartManager.cart.value
        assertEquals("Bodega A.", cart.storeName)
        assertEquals(2, cart.items.size)
        assertEquals(5, cart.totalItemsCount)
        assertEquals(160.0, cart.totalDiscountPrice, 0.01) // (2 * 50) + (3 * 20)
    }

    @Test
    fun addToCart_shouldPreventDifferentStore_andReturnConflict() {
        CartManager.addToCart(offerStoreA1, 1)

        val conflictRes = CartManager.addToCart(offerStoreB, 1)
        assertTrue(conflictRes is AddToCartResult.StoreConflict)
        val conflict = conflictRes as AddToCartResult.StoreConflict
        assertEquals("Bodega A.", conflict.currentStoreName)
        assertEquals("Abarrotes B.", conflict.newStoreName)

        // El carrito original permanece inalterado
        assertEquals("Bodega A.", CartManager.cart.value.storeName)
        assertEquals(1, CartManager.cart.value.items.size)
    }

    @Test
    fun replaceCart_shouldClearPreviousStore_andSetNewStore() {
        CartManager.addToCart(offerStoreA1, 1)
        CartManager.replaceCart(offerStoreB, 2)

        val cart = CartManager.cart.value
        assertEquals("Abarrotes B.", cart.storeName)
        assertEquals(1, cart.items.size)
        assertEquals(2, cart.totalItemsCount)
    }

    @Test
    fun updateQuantity_and_removeItem_shouldWorkCorrectly() {
        CartManager.addToCart(offerStoreA1, 1)
        CartManager.addToCart(offerStoreA2, 1)

        CartManager.updateQuantity(offerStoreA1.id, 4)
        assertEquals(4, CartManager.cart.value.items.first { it.offer.id == offerStoreA1.id }.quantity)

        CartManager.updateQuantity(offerStoreA1.id, 0)
        assertFalse(CartManager.cart.value.items.any { it.offer.id == offerStoreA1.id })

        CartManager.removeItem(offerStoreA2.id)
        assertTrue(CartManager.cart.value.isEmpty)
    }

    // ==========================================
    // ApartadoOrder & QR Consolidado Tests
    // ==========================================

    @Test
    fun apartadoOrder_fromSingleOffer_multiQuantity_shouldCalculateTotal() {
        val order = ApartadoOrder.fromSingleOffer(offerStoreA1, 4)
        assertEquals(1, order.items.size)
        assertEquals(4, order.items.first().quantity)
        assertEquals(200.0, order.totalDiscountPrice, 0.01) // 4 * 50
        assertEquals(400.0, order.totalOriginalPrice, 0.01) // 4 * 100
        assertEquals(200.0, order.totalSavedAmount, 0.01)
        assertTrue(order.couponCode.contains("4X"))
    }

    @Test
    fun apartadoOrder_fromCart_shouldConsolidateMultipleOffers() {
        CartManager.addToCart(offerStoreA1, 2) // $100
        CartManager.addToCart(offerStoreA2, 1) // $20
        val cart = CartManager.cart.value

        val order = ApartadoOrder.fromCart(cart)
        assertEquals("Bodega A.", order.storeName)
        assertEquals(2, order.items.size)
        assertEquals(120.0, order.totalDiscountPrice, 0.01)
        assertEquals(120.0, order.totalSavedAmount, 0.01)
        assertTrue(order.couponCode.startsWith("SNS-CART-"))
    }

    // ==========================================
    // Image Moderation (Backend Integration) Tests
    // ==========================================

    @Test
    fun moderationService_shouldReject_emptyBytes() = runTest {
        val service = ImageModerationService()
        val res = service.moderateImage(ByteArray(0))
        assertTrue(res is ModerationResult.Rejected)
    }

    @Test
    fun moderationService_shouldApprove_whenNoBackendConfigured() = runTest {
        val service = ImageModerationService(mediaApi = null)
        val res = service.moderateImage(byteArrayOf(1, 2, 3))
        assertTrue(res is ModerationResult.Approved)
    }

    @Test
    fun moderationService_shouldHandlePolicyMessage_whenFlagged() = runTest {
        assertEquals(
            "No se pudo subir la imagen por políticas de seguridad.",
            ImageModerationService.SECURITY_POLICY_MESSAGE
        )
    }
}

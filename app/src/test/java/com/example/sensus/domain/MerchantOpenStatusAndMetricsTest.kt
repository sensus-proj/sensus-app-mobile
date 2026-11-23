package com.example.sensus.domain

import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantOfferMetrics
import com.example.sensus.domain.model.MerchantOpenStatus
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.openStatusAt
import com.example.sensus.domain.model.scheduleFor
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantOpenStatusAndMetricsTest {

    // 2026-11-23 is a Monday.
    private fun monday(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 23, hour, minute)

    private fun profile(vararg days: HorarioDia, active: Boolean = true) =
        MerchantProfile(nombreComercial = "Tienda", direccion = "Calle 1", codigoPostal = "42800", horarios = days.toList(), activo = active)

    @Test
    fun openOnlyInsidePublishedRange() {
        val merchant = profile(HorarioDia("Lunes", "08:00", "20:00"))

        assertEquals(MerchantOpenStatus.CLOSED, merchant.openStatusAt(monday(7, 59)))
        assertEquals(MerchantOpenStatus.OPEN, merchant.openStatusAt(monday(8)))
        assertEquals(MerchantOpenStatus.OPEN, merchant.openStatusAt(monday(19, 59)))
        assertEquals(MerchantOpenStatus.CLOSED, merchant.openStatusAt(monday(20)))
        assertEquals("08:00 - 20:00", merchant.scheduleFor(DayOfWeek.MONDAY))
    }

    @Test
    fun overnightRangeAndSingleDigitHours() {
        val merchant = profile(HorarioDia("lunes", "18:00", "2:00"))

        assertEquals(MerchantOpenStatus.OPEN, merchant.openStatusAt(monday(23)))
        assertEquals(MerchantOpenStatus.CLOSED, merchant.openStatusAt(monday(1)))
        assertEquals(MerchantOpenStatus.OPEN, merchant.openStatusAt(monday(1).plusDays(1)))
        assertEquals(MerchantOpenStatus.UNKNOWN, merchant.openStatusAt(monday(2).plusDays(1)))
        assertEquals(MerchantOpenStatus.CLOSED, merchant.openStatusAt(monday(10)))
    }

    @Test
    fun closedDayInactiveMerchantAndMissingDataNeverClaimOpen() {
        assertEquals(MerchantOpenStatus.CLOSED, profile(HorarioDia("Lunes", null, null, estaCerrado = true)).openStatusAt(monday(12)))
        assertEquals(MerchantOpenStatus.CLOSED, profile(HorarioDia("Lunes", "08:00", "20:00"), active = false).openStatusAt(monday(12)))
        assertEquals(MerchantOpenStatus.UNKNOWN, profile(HorarioDia("Martes", "08:00", "20:00")).openStatusAt(monday(12)))
        assertEquals(MerchantOpenStatus.UNKNOWN, profile(HorarioDia("Lunes", "abc", "20:00")).openStatusAt(monday(12)))
        assertEquals(MerchantOpenStatus.UNKNOWN, (null as MerchantProfile?).openStatusAt(monday(12)))
        assertNull(profile(HorarioDia("Lunes", null, null, estaCerrado = true)).scheduleFor(DayOfWeek.MONDAY))
        assertNull(profile().scheduleFor(DayOfWeek.MONDAY))
    }

    @Test
    fun metricsSplitInventoryByLifecycle() {
        fun offer(id: String, units: Int, status: OfferLifecycleStatus) =
            ProductOffer(id, "Oferta $id", "Tienda", "Abarrotes", 20.0, 10.0, 50, units, 100, status = status)

        val metrics = MerchantOfferMetrics.from(
            listOf(
                offer("a", 10, OfferLifecycleStatus.ACTIVE),
                offer("b", 2, OfferLifecycleStatus.ACTIVE),
                offer("c", 0, OfferLifecycleStatus.ACTIVE),
                offer("d", 5, OfferLifecycleStatus.PAUSED),
                offer("e", 4, OfferLifecycleStatus.EXPIRED)
            )
        )

        assertEquals(MerchantOfferMetrics(active = 2, paused = 1, finished = 2, availableUnits = 12, lowStock = 1), metrics)
        assertEquals(5, metrics.total)
        assertEquals(MerchantOfferMetrics(), MerchantOfferMetrics.from(emptyList()))
    }
}

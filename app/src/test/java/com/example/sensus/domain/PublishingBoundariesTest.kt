package com.example.sensus.domain

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sensus.data.local.db.SensusDatabaseMigrations
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.PublishingMerchantPolicy
import com.example.sensus.domain.model.UserLocation
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishingBoundariesTest {
    @Test
    fun nearbyMerchants_useCoordinatesDeduplicateAndRequireBackendId() {
        val offer = ProductOffer(
            "1", "Pan", "Local", "Panadería", 10.0, 5.0, 50, 7, 0,
            latitude = 20.0, longitude = -99.0, merchantId = "shop-1"
        )
        val offers = listOf(
            offer,
            offer.copy(id = "2"),
            offer.copy(id = "3", merchantId = null),
            offer.copy(id = "4", merchantId = "far-shop", latitude = 21.0)
        )
        assertEquals(listOf("1"), PublishingMerchantPolicy.nearby(offers, UserLocation(20.0, -99.0), 2000).map { it.id })
    }

    @Test
    fun migration2To3_addsFeedStockAndExpirationWithoutDroppingExistingData() {
        val statements = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") statements += args!![0] as String
            null
        } as SupportSQLiteDatabase
        SensusDatabaseMigrations.MIGRATION_2_3.migrate(db)
        assertEquals(3, statements.size)
        assertTrue(statements[0].contains("feed_posts"))
        assertTrue(statements[1].contains("unitsLeft"))
        assertTrue(statements[2].contains("expirationDate"))
        assertFalse(statements.any { it.contains("DROP", ignoreCase = true) || it.contains("DELETE", ignoreCase = true) })
    }

    @Test
    fun expirationPicker_preservesLocalDayWhenUtcDateAlreadyChanged() {
        val local = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Mexico_City")).apply {
            clear()
            set(2026, java.util.Calendar.NOVEMBER, 12, 20, 0, 0)
        }
        val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = com.example.sensus.presentation.merchant.expirationCalendarDayUtc(local.timeInMillis, local.timeZone)
        }
        assertEquals(12, utc.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(0, utc.get(java.util.Calendar.HOUR_OF_DAY))
    }
}

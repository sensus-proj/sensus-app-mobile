package com.example.sensus.domain

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sensus.data.local.db.SensusDatabaseMigrations
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishingBoundariesTest {
    @Test
    fun migration2To3_addsStockAndExpirationWithoutDroppingExistingData() {
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

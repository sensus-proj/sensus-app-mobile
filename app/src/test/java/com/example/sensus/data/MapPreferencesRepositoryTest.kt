package com.example.sensus.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sensus.data.local.db.DatabaseFactory
import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.entity.UserPreferenceEntity
import com.example.sensus.data.repository.MapPreferencesRepositoryImpl
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.toDomain
import com.example.sensus.presentation.common.toStyle
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPreferencesRepositoryTest {

    @Test
    fun mapLayer_fromStorageKey_mapsKnownAndDegradesUnknownToStandard() {
        assertEquals(MapLayer.STANDARD, MapLayer.fromStorageKey("STANDARD"))
        assertEquals(MapLayer.SATELLITE, MapLayer.fromStorageKey(" satellite "))
        assertEquals(MapLayer.TERRAIN, MapLayer.fromStorageKey("Terrain"))
        assertEquals(MapLayer.DARK, MapLayer.fromStorageKey("dark"))
        assertEquals(MapLayer.STANDARD, MapLayer.fromStorageKey(null))
        assertEquals(MapLayer.STANDARD, MapLayer.fromStorageKey("UNKNOWN"))
    }

    @Test
    fun mapLayer_styleRoundtrip_preservesAllVariants() {
        MapLayer.entries.forEach { domainLayer ->
            val uiStyle = domainLayer.toStyle()
            assertEquals(domainLayer, uiStyle.toDomain())
        }
        assertEquals(MapLayer.DARK, MapLayerStyle.DARK.toDomain())
    }

    @Test
    fun repository_coldLoadCachesResultAndAvoidsDuplicateDaoReads() = runTest {
        val dao = mockk<UserPreferenceDao>(relaxed = true)
        every { dao.getByKey(MapPreferencesRepositoryImpl.KEY_MAP_LAYER) } returns
            UserPreferenceEntity("map_layer", "SATELLITE", 10L)

        val repo = MapPreferencesRepositoryImpl(dao, Dispatchers.Unconfined)
        assertNull(repo.peekCachedLayer())

        val first = repo.getPreferredLayer()
        val second = repo.getPreferredLayer()

        assertEquals(MapLayer.SATELLITE, first)
        assertEquals(MapLayer.SATELLITE, second)
        assertEquals(MapLayer.SATELLITE, repo.peekCachedLayer())
        verify(exactly = 1) { dao.getByKey(MapPreferencesRepositoryImpl.KEY_MAP_LAYER) }
    }

    @Test
    fun repository_saveAndErrorFallback_workSafely() = runTest {
        val dao = mockk<UserPreferenceDao>(relaxed = true)
        val savedSlot = slot<UserPreferenceEntity>()
        every { dao.upsert(capture(savedSlot)) } returns Unit
        every { dao.getByKey(MapPreferencesRepositoryImpl.KEY_MAP_LAYER) } throws IllegalStateException("locked")

        val failingRepo = MapPreferencesRepositoryImpl(dao, Dispatchers.Unconfined)
        assertEquals(MapLayer.STANDARD, failingRepo.getPreferredLayer())

        val saveUseCase = SavePreferredMapLayerUseCase(failingRepo)
        val getUseCase = GetPreferredMapLayerUseCase(failingRepo)
        saveUseCase(MapLayer.DARK)

        assertEquals("map_layer", savedSlot.captured.prefKey)
        assertEquals("DARK", savedSlot.captured.prefValue)
        assertEquals(MapLayer.DARK, getUseCase.peekCached())
        assertEquals(MapLayer.DARK, getUseCase())
    }

    @Test
    fun databaseMigration1To2_createsUserPreferencesTableWithoutDrops() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlSlot = slot<String>()
        every { db.execSQL(capture(sqlSlot)) } returns Unit

        DatabaseFactory.MIGRATION_1_2.migrate(db)

        verify(exactly = 1) { db.execSQL(any<String>()) }
        assertTrue(sqlSlot.captured.contains("CREATE TABLE IF NOT EXISTS `user_preferences`"))
        assertTrue(sqlSlot.captured.contains("PRIMARY KEY(`prefKey`)"))
    }
}

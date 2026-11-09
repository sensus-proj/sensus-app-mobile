package com.example.sensus.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.data.local.db.SensusDatabase
import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.data.local.db.entity.PendingSyncEntity
import com.example.sensus.data.local.db.entity.UserSessionEntity
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: SensusDatabase
    private val testDbFile = "test_sensus_encrypted.db"
    private val testPassphrase = "SuperSecretAES256TestPassphraseForSQLCipher123!".toByteArray(Charsets.UTF_8)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbFile)

        val factory = SupportFactory(testPassphrase)
        db = Room.databaseBuilder(context, SensusDatabase::class.java, testDbFile)
            .openHelperFactory(factory)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(testDbFile)
    }

    @Test
    fun offerDao_crudOperations_workCorrectlyWithSQLCipher() {
        val offerDao = db.offerDao()

        val offer = OfferEntity(
            "off-sqlcipher-1",
            "Caja de Frutas",
            "Fruta fresca del día",
            50.0,
            100.0,
            "Perecederos",
            "https://cdn.sensus.mx/fruit.jpg",
            20.05,
            -99.34,
            "m-fruteria",
            "Frutería Central",
            System.currentTimeMillis()
        )

        offerDao.insertOffer(offer)

        val retrieved = offerDao.getOfferById("off-sqlcipher-1")
        assertNotNull(retrieved)
        assertEquals("Caja de Frutas", retrieved.title)
        assertEquals(50.0, retrieved.price, 0.01)

        val byCategory = offerDao.getOffersByCategory("Perecederos")
        assertEquals(1, byCategory.size)
        assertEquals("off-sqlcipher-1", byCategory.first().id)

        offerDao.deleteOfferById("off-sqlcipher-1")
        assertNull(offerDao.getOfferById("off-sqlcipher-1"))
    }

    @Test
    fun userSessionDao_saveAndClear_persistsAndPurgesProperly() {
        val sessionDao = db.userSessionDao()

        val session = UserSessionEntity(
            "usr-encrypted-1",
            "carlos@sensus.mx",
            "Carlos Gómez",
            "Comercio",
            "jwt-access-token-999",
            "jwt-refresh-token-888",
            1799999999L
        )

        sessionDao.saveSession(session)

        val retrieved = sessionDao.getSession()
        assertNotNull(retrieved)
        assertEquals("usr-encrypted-1", retrieved.userId)
        assertEquals("Carlos Gómez", retrieved.fullName)
        assertEquals("Comercio", retrieved.role)

        sessionDao.clearSession()
        assertNull(sessionDao.getSession())
    }

    @Test
    fun pendingSyncDao_enqueueAndClear_managesOfflineQueue() {
        val syncDao = db.pendingSyncDao()

        val syncItem = PendingSyncEntity(
            1L,
            "CREATE_OFFER",
            "{\"name\":\"Pan Francés\"}",
            System.currentTimeMillis(),
            "PENDING"
        )

        val insertedId = syncDao.insert(syncItem)
        assertTrue(insertedId > 0)

        val pending = syncDao.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("CREATE_OFFER", pending.first().operationType)
        assertEquals("PENDING", pending.first().status)

        syncDao.delete(pending.first())
        assertTrue(syncDao.getPendingOperations().isEmpty())
    }
}

package com.example.sensus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sensus.data.local.db.SensusDatabase
import com.example.sensus.data.local.db.entity.FeedPostEntity
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.media.AndroidImageCompressor
import com.example.sensus.domain.media.ImageUploadPolicy
import com.example.sensus.domain.model.ProductOffer
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishingCacheIntegrationTest {
    @Test
    fun encryptedCache_preservesOfferStockAndReplacesFeedPages() = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SQLiteDatabase.loadLibs(context)
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val database = Room.inMemoryDatabaseBuilder(context, SensusDatabase::class.java)
            .openHelperFactory(SupportFactory(passphrase)).build()
        try {
            val offer = ProductOffer(
                "offer-1", "Pan", "Local", "Panadería", 100.0, 60.0, 40, 17, 0,
                expirationDate = "Mañana 20:00", merchantId = "shop-1"
            )
            database.offerDao().insertOffer(offer.toEntity())
            assertEquals(17, database.offerDao().getOfferById("offer-1").unitsLeft)
            val dao = database.feedPostDao()
            dao.replacePage("nearby", 1, listOf(FeedPostEntity("p1", "nearby", 1, 0, "approved")))
            assertEquals("p1", dao.getPage("nearby", 1).single().id)
            dao.replacePage("nearby", 1, emptyList())
            assertTrue(dao.getPage("nearby", 1).isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun selectedPhoto_isCompressedWithinUploadLimit() = runBlocking {
        val bitmap = android.graphics.Bitmap.createBitmap(2000, 2000, android.graphics.Bitmap.Config.ARGB_8888)
        val raw = java.io.ByteArrayOutputStream().use { output ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        bitmap.recycle()
        val compressed = AndroidImageCompressor().compressForUpload(raw)
        assertTrue(compressed != null && compressed.isNotEmpty() && compressed.size <= ImageUploadPolicy.MAX_UPLOAD_BYTES)
    }
}

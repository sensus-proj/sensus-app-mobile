package com.example.sensus.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.media.ImageUploadPolicy
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DIMENSION_PX = 1600
private const val JPEG_MAX_QUALITY = 90
private const val JPEG_MIN_QUALITY = 40
private const val JPEG_QUALITY_STEP = 10
private val JPEG_QUALITY_STEPS = (JPEG_MIN_QUALITY..JPEG_MAX_QUALITY step JPEG_QUALITY_STEP).reversed().toList()
private const val SCALE_DOWN_FACTOR = 0.75f
private const val MIN_DIMENSION_PX = 320

/**
 * Comprime a JPEG garantizando <= [ImageUploadPolicy.MAX_UPLOAD_BYTES] (FT-15, RF-15-03, NFR-022-01).
 * Reduce primero la calidad y, si no basta, la resolucion; si aun asi excede el limite devuelve null.
 */
class AndroidImageCompressor(
    private val maxBytes: Int = ImageUploadPolicy.MAX_UPLOAD_BYTES
) : ImageCompressor {

    override suspend fun compressForUpload(rawBytes: ByteArray): ByteArray? = withContext(Dispatchers.Default) {
        if (rawBytes.isEmpty()) return@withContext null
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return@withContext null
        var bitmap = scaleToMaxDimension(decoded, MAX_DIMENSION_PX)
        var result = compressToLimit(maxBytes, JPEG_QUALITY_STEPS) { quality -> encodeJpeg(bitmap, quality) }
        while (result == null && minOf(bitmap.width, bitmap.height) > MIN_DIMENSION_PX) {
            val target = (maxOf(bitmap.width, bitmap.height) * SCALE_DOWN_FACTOR).toInt()
            bitmap = scaleToMaxDimension(bitmap, target)
            result = compressToLimit(maxBytes, JPEG_QUALITY_STEPS) { quality -> encodeJpeg(bitmap, quality) }
        }
        result
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }

    private fun scaleToMaxDimension(source: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(source.width, source.height)
        if (largest <= maxDimension) return source
        val ratio = maxDimension.toFloat() / largest
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}

/** Busca la primera calidad cuyo resultado cabe en [maxBytes]; logica pura y verificable en JVM. */
internal fun compressToLimit(maxBytes: Int, qualitySteps: List<Int>, encode: (Int) -> ByteArray): ByteArray? = qualitySteps.asSequence()
    .map(encode)
    .firstOrNull { it.isNotEmpty() && it.size <= maxBytes }

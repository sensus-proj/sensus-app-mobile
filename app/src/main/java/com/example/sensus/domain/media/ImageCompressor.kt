package com.example.sensus.domain.media

/** Limites de carga de imagenes moderadas (FT-15, NFR-022-01). */
object ImageUploadPolicy {
    /**
     * Límite seguro bajo el tope de 4.5 MB (`4_500_000` bytes) del backend considerando cabeceras multipart.
     */
    const val MAX_UPLOAD_BYTES: Int = 4_000_000
    const val JPEG_MIME_TYPE: String = "image/jpeg"
    const val TOO_LARGE_MESSAGE: String = "No se pudo preparar la imagen (máximo 4 MB). Elige otra foto."

    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        var sample = 1
        val largest = maxOf(width, height)
        while (largest / sample > maxDimension * 2) {
            sample *= 2
        }
        return sample
    }
}

/** Comprime una imagen para su carga, garantizando el limite de [ImageUploadPolicy.MAX_UPLOAD_BYTES]. */
interface ImageCompressor {
    /** Devuelve un JPEG <= 4.5 MB, o null si la imagen es invalida o no puede reducirse al limite. */
    suspend fun compressForUpload(rawBytes: ByteArray): ByteArray?
}

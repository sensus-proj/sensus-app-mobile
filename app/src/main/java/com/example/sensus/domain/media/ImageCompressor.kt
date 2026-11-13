package com.example.sensus.domain.media

/** Limites de carga de imagenes moderadas (FT-15, NFR-022-01). */
object ImageUploadPolicy {
    const val MAX_UPLOAD_BYTES: Int = 4_718_592
    const val JPEG_MIME_TYPE: String = "image/jpeg"
    const val TOO_LARGE_MESSAGE: String = "No se pudo preparar la imagen (máximo 4.5 MB). Elige otra foto."
}

/** Comprime una imagen para su carga, garantizando el limite de [ImageUploadPolicy.MAX_UPLOAD_BYTES]. */
interface ImageCompressor {
    /** Devuelve un JPEG <= 4.5 MB, o null si la imagen es invalida o no puede reducirse al limite. */
    suspend fun compressForUpload(rawBytes: ByteArray): ByteArray?
}

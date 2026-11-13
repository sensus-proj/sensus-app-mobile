package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.UploadedMedia

interface MediaRepository {
    /** Sube la imagen via multipart; el backend la modera fail-closed antes de almacenarla (FT-15, RF-15-03/04). */
    suspend fun uploadAndModerate(bytes: ByteArray, mimeType: String): Result<UploadedMedia>
}

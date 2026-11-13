package com.example.sensus.domain.repository

import com.example.sensus.domain.model.ModerationResult

interface ImageModerationRepository {
    suspend fun moderateImage(imageBytes: ByteArray, textContext: String? = null): ModerationResult
}

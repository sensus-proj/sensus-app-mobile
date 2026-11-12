package com.example.sensus.domain.repository

import com.example.sensus.domain.model.MapLayer

interface MapPreferencesRepository {
    fun peekCachedLayer(): MapLayer?
    suspend fun getPreferredLayer(): MapLayer
    suspend fun savePreferredLayer(layer: MapLayer)
}

package com.example.sensus.domain.usecase

import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.repository.MapPreferencesRepository

/**
 * Recupera la capa del mapa almacenada localmente en almacenamiento cifrado.
 */
class GetPreferredMapLayerUseCase(
    private val mapPreferencesRepository: MapPreferencesRepository
) {
    fun peekCached(): MapLayer? = mapPreferencesRepository.peekCachedLayer()

    suspend operator fun invoke(): MapLayer = mapPreferencesRepository.getPreferredLayer()
}

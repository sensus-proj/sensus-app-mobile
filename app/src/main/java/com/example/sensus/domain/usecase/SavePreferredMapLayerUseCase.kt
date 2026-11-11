package com.example.sensus.domain.usecase

import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.repository.MapPreferencesRepository

/**
 * Guarda la preferencia de capa visual del mapa elegida por el usuario.
 */
class SavePreferredMapLayerUseCase(
    private val mapPreferencesRepository: MapPreferencesRepository
) {
    suspend operator fun invoke(layer: MapLayer) {
        mapPreferencesRepository.savePreferredLayer(layer)
    }
}

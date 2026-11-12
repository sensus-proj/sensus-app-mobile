package com.example.sensus.domain.model

/**
 * Capas visuales soportadas en el mapa persistidas como preferencia del usuario.
 */
enum class MapLayer(val storageKey: String) {
    STANDARD("STANDARD"),
    SATELLITE("SATELLITE"),
    TERRAIN("TERRAIN"),
    DARK("DARK");

    companion object {
        fun fromStorageKey(raw: String?): MapLayer {
            val normalized = raw?.trim().orEmpty()
            return entries.firstOrNull { it.storageKey.equals(normalized, ignoreCase = true) } ?: STANDARD
        }
    }
}

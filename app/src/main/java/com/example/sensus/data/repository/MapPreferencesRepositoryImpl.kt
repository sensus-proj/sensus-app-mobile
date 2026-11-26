package com.example.sensus.data.repository

import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.entity.UserPreferenceEntity
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.repository.MapPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persiste preferencias locales visuales dentro de la base Room cifrada con SQLCipher.
 */
class MapPreferencesRepositoryImpl(
    private val userPreferenceDao: () -> UserPreferenceDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MapPreferencesRepository {

    @Volatile
    private var cachedLayer: MapLayer? = null

    override fun peekCachedLayer(): MapLayer? = cachedLayer

    override suspend fun getPreferredLayer(): MapLayer {
        cachedLayer?.let { return it }
        val resolved = runCatching {
            withContext(ioDispatcher) {
                userPreferenceDao().getByKey(KEY_MAP_LAYER)?.prefValue
            }
        }.map(MapLayer::fromStorageKey).getOrDefault(MapLayer.STANDARD)
        cachedLayer = resolved
        return resolved
    }

    override suspend fun savePreferredLayer(layer: MapLayer) {
        cachedLayer = layer
        runCatching {
            withContext(ioDispatcher) {
                userPreferenceDao().upsert(
                    UserPreferenceEntity(KEY_MAP_LAYER, layer.storageKey, System.currentTimeMillis())
                )
            }
        }
    }

    override suspend fun getPreferredRadiusMeters(): Int? = withContext(ioDispatcher) {
        try {
            userPreferenceDao().getByKey(KEY_NEARBY_RADIUS)?.prefValue?.toIntOrNull()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun savePreferredRadiusMeters(radiusMeters: Int) {
        withContext(ioDispatcher) {
            try {
                userPreferenceDao().upsert(UserPreferenceEntity(KEY_NEARBY_RADIUS, radiusMeters.toString(), System.currentTimeMillis()))
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Keep the active radius if local storage is unavailable.
            }
        }
    }

    companion object {
        private const val KEY_NEARBY_RADIUS = "radio_preferido_m"
        const val KEY_MAP_LAYER = "map_layer"
    }
}

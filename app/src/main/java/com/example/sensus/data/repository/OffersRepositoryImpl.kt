package com.example.sensus.data.repository

import com.example.sensus.data.MockDataProvider
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.mapper.OfferMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.OffersRepository

/**
 * Implementación Offline-First de OffersRepository respaldada por Room + SQLCipher (OfferDao).
 * Estrategia de conflicto para inventario: server-wins (Constitución §5).
 *
 * TODO: [API-MIGRATION] En cuanto se integre el endpoint remoto GET /api/v1/offers/nearby (Spec 007 / FT-07),
 * se debe eliminar obligatoriamente la semilla de respaldo MockDataProvider y sincronizar los datos directamente
 * entre el cliente Retrofit y la base de datos cifrada local Room (OfferDao).
 */
@Suppress("DEPRECATION")
class OffersRepositoryImpl(
    private val offerDao: OfferDao? = null
) : OffersRepository {

    override fun getOffers(): List<ProductOffer> {
        val dao = offerDao ?: return MockDataProvider.sampleOffers
        return try {
            val cachedEntities = dao.getOffers()
            if (!cachedEntities.isNullOrEmpty()) {
                cachedEntities.map { it.toDomain() }
            } else {
                val initialOffers = MockDataProvider.sampleOffers
                dao.insertOffers(initialOffers.map { it.toEntity() })
                initialOffers
            }
        } catch (_: Exception) {
            MockDataProvider.sampleOffers
        }
    }

    override fun addOffer(offer: ProductOffer) {
        MockDataProvider.addOffer(offer)
        try {
            offerDao?.insertOffer(offer.toEntity())
        } catch (_: Exception) {
            // Degradación segura si el DAO no está disponible en el hilo actual
        }
    }

    /**
     * Estrategia de resolución de conflictos Server-Wins (Constitución §5):
     * El catálogo remoto reemplaza autoritativamente el caché local de ofertas.
     */
    override fun syncOffersFromServer(remoteOffers: List<ProductOffer>) {
        val dao = offerDao ?: return
        try {
            dao.clearOffers()
            dao.insertOffers(remoteOffers.map { it.toEntity() })
        } catch (_: Exception) {
            // Degradación segura ante error de E/S local
        }
    }
}

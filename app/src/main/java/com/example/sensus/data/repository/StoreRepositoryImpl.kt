package com.example.sensus.data.repository

import com.example.sensus.domain.model.FriendShare
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.NotificationItem
import com.example.sensus.domain.repository.StoreRepository

/**
 * Valores iniciales de la tienda, notificaciones y amigos mientras llegan los datos reales de la API.
 * No contiene datos de muestra: los datos simulados viven solo en `src/test` y `src/androidTest` (FT-27).
 */
class StoreRepositoryImpl : StoreRepository {
    override fun getSampleStore(): MerchantStore = MerchantStore()
    override fun getSampleNotifications(): List<NotificationItem> = emptyList()
    override fun getSampleFriends(): List<FriendShare> = emptyList()
}

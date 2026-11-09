package com.example.sensus.data.repository

import com.example.sensus.data.MockDataProvider
import com.example.sensus.domain.model.ChatMessage
import com.example.sensus.domain.model.FriendShare
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.NotificationItem
import com.example.sensus.domain.repository.StoreRepository

/**
 * Implementación de StoreRepository.
 * TODO: [API-MIGRATION] Eliminar dependencia de MockDataProvider una vez integrados los servicios
 * remotos de chat Sensi IA, notificaciones en la nube y feed social de comunidad.
 */
class StoreRepositoryImpl : StoreRepository {
    override fun getSampleStore(): MerchantStore = MockDataProvider.sampleStore
    override fun getSampleNotifications(): List<NotificationItem> = MockDataProvider.sampleNotifications
    override fun getSampleFriends(): List<FriendShare> = MockDataProvider.sampleFriends
    override fun getSampleChatMessages(): List<ChatMessage> = MockDataProvider.sampleChatMessages
}

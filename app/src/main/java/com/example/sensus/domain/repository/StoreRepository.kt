package com.example.sensus.domain.repository

import com.example.sensus.domain.model.ChatMessage
import com.example.sensus.domain.model.FriendShare
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.NotificationItem

interface StoreRepository {
    fun getSampleStore(): MerchantStore
    fun getSampleNotifications(): List<NotificationItem>
    fun getSampleFriends(): List<FriendShare>
    fun getSampleChatMessages(): List<ChatMessage>
}

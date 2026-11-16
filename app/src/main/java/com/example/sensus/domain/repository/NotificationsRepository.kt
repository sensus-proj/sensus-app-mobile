package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.NotificationsPreferences

interface NotificationsRepository {
    suspend fun getSettings(): Result<NotificationsPreferences>
    suspend fun updateSettings(settings: NotificationsPreferences): Result<NotificationsPreferences>
    suspend fun registerDevice(device: NotificationsDevice): Result<Unit>
}

interface NotificationsPushDeviceProvider {
    companion object {
        const val CONFIGURATION_UNAVAILABLE = "Firebase Android resources unavailable"
    }

    suspend fun getDevice(): Result<NotificationsDevice>
}

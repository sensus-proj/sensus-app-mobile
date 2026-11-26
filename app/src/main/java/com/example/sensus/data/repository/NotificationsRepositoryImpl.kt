package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.NotificationsApiService
import com.example.sensus.data.remote.dto.NotificationsDeviceRequest
import com.example.sensus.data.remote.dto.NotificationsLocationRequest
import com.example.sensus.data.remote.dto.NotificationsSettingsDto
import com.example.sensus.data.remote.dto.NotificationsUpdateRequest
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.NotificationsRepository
import javax.inject.Inject
import retrofit2.Response

private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val PLATFORM_ANDROID = "Android"

class NotificationsRepositoryImpl @Inject constructor(private val api: NotificationsApiService) : NotificationsRepository {
    override suspend fun getSettings(): Result<NotificationsPreferences> = safeNetworkCall("Settings unavailable") {
        mapSettings(api.getSettings())
    }

    override suspend fun updateSettings(settings: NotificationsPreferences): Result<NotificationsPreferences> = safeNetworkCall("Settings unavailable") {
        mapSettings(api.updateSettings(NotificationsUpdateRequest(settings.pushEnabled, settings.offerAlerts, settings.socialAlerts, settings.radiusKm)))
    }

    override suspend fun registerDevice(device: NotificationsDevice): Result<Unit> = safeNetworkCall("Registration unavailable") {
        val response = api.registerDevice(NotificationsDeviceRequest(device.deviceId, PLATFORM_ANDROID, device.token))
        when {
            !response.isSuccessful -> failure(response.code())
            response.body()?.isActive == true -> Result.Success(Unit)
            else -> Result.Failure(AppError.Generic())
        }
    }

    override suspend fun shareLocation(location: UserLocation): Result<Unit> = safeNetworkCall("Location unavailable") {
        emptyResult(api.shareLocation(NotificationsLocationRequest(location.latitude, location.longitude)))
    }

    override suspend fun deleteLocation(): Result<Unit> = safeNetworkCall("Location unavailable") {
        emptyResult(api.deleteLocation())
    }

    private fun emptyResult(response: Response<Unit>): Result<Unit> = if (response.isSuccessful) Result.Success(Unit) else failure(response.code())

    private fun mapSettings(response: Response<NotificationsSettingsDto>): Result<NotificationsPreferences> {
        val body = response.body()
        return when {
            !response.isSuccessful -> failure(response.code())
            body == null -> Result.Failure(AppError.Generic())
            body.radiusKm !in NotificationsPreferences.MIN_RADIUS_KM..NotificationsPreferences.MAX_RADIUS_KM ->
                Result.Failure(AppError.Validation())
            else -> Result.Success(NotificationsPreferences(body.pushEnabled, body.offerAlerts, body.socialAlerts, body.radiusKm))
        }
    }

    private fun failure(code: Int): Result.Failure = if (code == HTTP_SERVICE_UNAVAILABLE) {
        Result.Failure(AppError.Generic(NotificationsRepository.PUSH_UNAVAILABLE))
    } else {
        Result.Failure(mapCommonHttpStatus(code, null, "Session required", "Service unavailable"))
    }
}

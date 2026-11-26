package com.example.sensus.data.push

import android.content.Context
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.repository.NotificationsLocalStore
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Entrega el par (deviceId, token FCM) para `PUT /api/v1/users/device-token`.
 *
 * `deviceId` es el UUID estable de la instalacion ([NotificationsLocalStore.installationId]), nunca el token.
 * El token es una credencial: no se guarda ni se escribe en logs.
 */
class NotificationsFirebaseDeviceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: NotificationsLocalStore
) : NotificationsPushDeviceProvider {
    override suspend fun getDevice(token: String?): Result<NotificationsDevice> {
        return try {
            FirebaseApp.initializeApp(context) ?: return Result.Failure(
                AppError.NotFound(NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE)
            )
            buildDevice(store.installationId(), resolveToken(token))
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: FirebaseException) {
            Result.Failure(AppError.Generic())
        } catch (_: IOException) {
            Result.Failure(AppError.Network())
        } catch (_: IllegalStateException) {
            Result.Failure(AppError.Generic())
        } catch (_: IllegalArgumentException) {
            Result.Failure(AppError.Generic())
        }
    }

    private suspend fun resolveToken(token: String?): String? = token?.takeIf { it.isNotBlank() } ?: FirebaseMessaging.getInstance().token.awaitDeviceTask()

    private fun buildDevice(id: String, token: String?): Result<NotificationsDevice> = if (id.isBlank() || token.isNullOrBlank()) {
        Result.Failure(AppError.Generic())
    } else {
        Result.Success(NotificationsDevice(id, token))
    }
}

private suspend fun <T> Task<T>.awaitDeviceTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}

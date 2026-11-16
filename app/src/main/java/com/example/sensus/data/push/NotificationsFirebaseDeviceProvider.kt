package com.example.sensus.data.push

import android.content.Context
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.NotificationsDevice
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

class NotificationsFirebaseDeviceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationsPushDeviceProvider {
    override suspend fun getDevice(): Result<NotificationsDevice> {
        return try {
            val app = FirebaseApp.initializeApp(context) ?: return Result.Failure(
                AppError.NotFound(NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE)
            )
            val id = FirebaseInstallations.getInstance(app).id.awaitDeviceTask()
            val token = FirebaseMessaging.getInstance().token.awaitDeviceTask()
            if (id.isBlank() || token.isBlank()) Result.Failure(AppError.Generic()) else Result.Success(NotificationsDevice(id, token))
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
}

private suspend fun <T> Task<T>.awaitDeviceTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}

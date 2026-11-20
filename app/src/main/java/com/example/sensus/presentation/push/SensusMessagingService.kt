package com.example.sensus.presentation.push

import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Punto de entrada de FCM (FT-39). No escribe el token ni el contenido del mensaje en logs.
 */
@AndroidEntryPoint
class SensusMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var deviceSync: NotificationsDeviceSync

    /** FCM rota el token: se vuelve a registrar si hay usuario registrado y permiso de notificaciones. */
    override fun onNewToken(token: String) {
        deviceSync.sync(token)
    }

    /** Solo se invoca con la app en primer plano (o para mensajes de solo datos). */
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        PushNotificationPresenter.show(applicationContext, notification.title, notification.body, message.data)
    }
}

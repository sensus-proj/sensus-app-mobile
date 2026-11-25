package com.example.sensus.presentation.push

import com.example.sensus.SensusApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Punto de entrada de FCM (FT-39). No escribe el token ni el contenido del mensaje en logs.
 */
class SensusMessagingService : FirebaseMessagingService() {

    /** FCM rota el token: se vuelve a registrar si hay usuario registrado y permiso de notificaciones. */
    override fun onNewToken(token: String) {
        (application as? SensusApp)?.notificationsDeviceSync?.sync(token)
    }

    /** Solo se invoca con la app en primer plano (o para mensajes de solo datos). */
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        PushNotificationPresenter.show(applicationContext, notification.title, notification.body, message.data)
    }
}

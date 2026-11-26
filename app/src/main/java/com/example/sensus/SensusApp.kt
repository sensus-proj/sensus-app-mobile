package com.example.sensus

import android.app.Application
import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.example.sensus.presentation.push.PushNotificationPresenter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SensusApp : Application() {

    @Inject
    lateinit var notificationsDeviceSync: NotificationsDeviceSync

    override fun onCreate() {
        super.onCreate()
        // FT-39: el canal debe existir antes de que FCM muestre un aviso con la app en segundo plano.
        PushNotificationPresenter.ensureChannel(this)
        // Registra el token FCM al iniciar sesion y al abrir la app con sesion registrada.
        notificationsDeviceSync.start()
    }
}

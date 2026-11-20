package com.example.sensus.presentation.push

import android.content.Intent
import android.os.Bundle
import com.example.sensus.domain.model.PushPayload
import com.example.sensus.domain.model.PushTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** Extrae el destino de una notificacion del intent que abre [com.example.sensus.MainActivity] (FT-39). */
object PushLaunchParser {
    /** Extras que FCM agrega al intent cuando el sistema muestra la notificacion con la app en segundo plano. */
    private val FCM_MARKERS = setOf("google.message_id", "gcm.n.e", "from")

    fun parse(intent: Intent?): PushTarget? {
        val extras = intent?.extras ?: return null
        return if (isPushLaunch(extras)) PushPayload.parse(extras.allowedStrings()) else null
    }

    /** Quita del intent las claves de la notificacion para que una rotacion o un `recreate` no la reabran. */
    fun consume(intent: Intent?) {
        intent ?: return
        intent.removeExtra(PushNotificationPresenter.EXTRA_FROM_PUSH)
        (PushPayload.ALLOWED_KEYS + FCM_MARKERS).forEach(intent::removeExtra)
    }

    private fun isPushLaunch(extras: Bundle): Boolean =
        extras.getBoolean(PushNotificationPresenter.EXTRA_FROM_PUSH, false) || FCM_MARKERS.any(extras::containsKey)

    @Suppress("DEPRECATION")
    private fun Bundle.allowedStrings(): Map<String, String?> = PushPayload.ALLOWED_KEYS.associateWith { key -> get(key) as? String }
}

/**
 * Cola de un solo elemento entre [com.example.sensus.MainActivity] y la pantalla principal (FT-39).
 * Si la app se abre sin sesion, el destino espera hasta que el usuario entra a la pantalla principal.
 */
@Singleton
class PushOpenRequests @Inject constructor() {
    private val pendingTarget = MutableStateFlow<PushTarget?>(null)
    val pending: StateFlow<PushTarget?> = pendingTarget.asStateFlow()

    fun submit(target: PushTarget) {
        pendingTarget.value = target
    }

    fun consume(): PushTarget? = pendingTarget.getAndUpdate { null }
}

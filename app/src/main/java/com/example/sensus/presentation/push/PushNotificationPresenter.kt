package com.example.sensus.presentation.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sensus.MainActivity
import com.example.sensus.R
import com.example.sensus.domain.model.PushPayload

/**
 * Canal y presentacion de notificaciones push en primer plano (FT-39).
 *
 * Con la app en segundo plano o cerrada, Android muestra la notificacion por su cuenta usando el canal declarado
 * en el manifiesto; al tocarla abre [MainActivity] con el bloque `data` como extras.
 */
object PushNotificationPresenter {
    const val EXTRA_FROM_PUSH = "com.example.sensus.extra.FROM_PUSH"
    private const val MAX_TITLE_LENGTH = 120
    private const val MAX_BODY_LENGTH = 500

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            context.getString(R.string.push_channel_id),
            context.getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.push_channel_description) }
        manager.createNotificationChannel(channel)
    }

    /**
     * Muestra el titulo y cuerpo que envia el backend. No agrega datos del apartado ni el QR: solo se ven en la
     * pantalla tras abrir la notificacion. Los extras del toque llevan unicamente claves permitidas.
     */
    fun show(context: Context, title: String?, body: String?, data: Map<String, String>) {
        val safeTitle = title?.trim()?.take(MAX_TITLE_LENGTH)?.takeIf { it.isNotEmpty() } ?: return
        if (!canPost(context)) return
        ensureChannel(context)
        val extras = PushPayload.sanitize(data)
        val notificationId = (extras.toString() + safeTitle).hashCode()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_PUSH, true)
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = body?.trim()?.take(MAX_BODY_LENGTH).orEmpty()
        val notification = NotificationCompat.Builder(context, context.getString(R.string.push_channel_id))
            .setSmallIcon(R.drawable.ic_stat_sensus)
            .setContentTitle(safeTitle)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

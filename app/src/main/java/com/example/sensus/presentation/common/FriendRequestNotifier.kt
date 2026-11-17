package com.example.sensus.presentation.common

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sensus.MainActivity
import com.example.sensus.R
import com.example.sensus.domain.model.PendingFriendRequest

private const val CHANNEL_ID = "sensus_friend_requests"
private const val CHANNEL_NAME = "Solicitudes de amistad"
private const val FRIEND_REQUEST_BASE_ID = 25000

object FriendRequestNotifier {
    const val OPEN_REQUESTS_URI = "sensus://amigos/solicitudes"

    fun showPendingRequestNotification(context: Context, request: PendingFriendRequest) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(OPEN_REQUESTS_URI)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, FRIEND_REQUEST_BASE_ID, tapIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nueva solicitud de amistad")
            .setContentText("${request.senderName} quiere agregarte en Sensus. Toca para aceptar o rechazar.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            FRIEND_REQUEST_BASE_ID + request.requestId.hashCode(),
            notification
        )
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos cuando otro vecino te envía una solicitud de amistad en Sensus."
        }
        manager.createNotificationChannel(channel)
    }
}

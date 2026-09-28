// HU04 - Parte 3: recepción de notificaciones push (FCM).
// - onMessageReceived: guarda el mensaje en NotificationsStore y muestra la
//   notificación en la barra de estado (canal "chaski_riders").
// - onNewToken: se suscribe al topic general "riders"; el token real se
//   enviará al backend cuando exista el módulo de pedidos.
// - Al tocar la notificación se abre MainActivity con NEW_TASK|CLEAR_TASK:
//   si hay sesión se entra a Home, si no a Access (routing existente).
package com.example.chaskirider.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chaskirider.MainActivity
import com.example.chaskirider.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChaskiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_RIDERS)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: return
        NotificationsStore.add(ChaskiNotification(title = title, body = body))
        showNotification(title, body)
    }

    // El permiso POST_NOTIFICATIONS se pide en runtime desde HomeScreen (Parte 3);
    // sin él la notificación simplemente no se muestra (no crashea).
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, body: String) {
        ensureChannel(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "chaski_riders"
        const val TOPIC_RIDERS = "riders"
        private const val NOTIFICATION_ID = 1001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones de Chaski Rider",
                    NotificationManager.IMPORTANCE_HIGH
                )
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }
}

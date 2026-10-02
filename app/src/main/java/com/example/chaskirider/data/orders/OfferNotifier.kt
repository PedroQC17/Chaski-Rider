package com.example.chaskirider.data.orders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.chaskirider.MainActivity
import com.example.chaskirider.R

class OfferNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private var shown: String? = null
    fun update(id: String?, secondsLeft: Int) {
        if (id == null || secondsLeft <= 0) { manager.cancel(NOTIFICATION_ID); shown = null; return }
        if (id == shown) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL,
            context.getString(R.string.orders_channel), NotificationManager.IMPORTANCE_HIGH))
        val intent = Intent(context, MainActivity::class.java).putExtra(EXTRA_DEMO, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.orders_new_offer))
            .setContentText(context.getString(R.string.orders_notification))
            .setContentIntent(pending).setOnlyAlertOnce(true).setAutoCancel(true).setTimeoutAfter(secondsLeft * 1000L)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
        shown = id
    }
    companion object {
        const val EXTRA_DEMO = "open_demo_offer"
        private const val CHANNEL = "chaski_demo_offers"
        private const val NOTIFICATION_ID = 2007
    }
}

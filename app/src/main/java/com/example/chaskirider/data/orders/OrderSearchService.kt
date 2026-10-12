package com.example.chaskirider.data.orders

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.chaskirider.MainActivity
import com.example.chaskirider.R
import com.example.chaskirider.di.AppContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class OrderSearchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = OrderSearchEngine(AppContainer.demoOfferRepository, AppContainer.riderSession, AppContainer.locationRepository)
        if (!engine.canSearch()) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "Disponibilidad de pedidos", NotificationManager.IMPORTANCE_LOW))
            val pending = PendingIntent.getActivity(this, ID, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_notification).setContentTitle("Estás conectado")
                .setContentText("Buscando pedidos cerca de ti").setContentIntent(pending)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true).setOnlyAlertOnce(true).build()
            ServiceCompat.startForeground(this, ID, notification,
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (error: RuntimeException) {
            Log.w("OrderSearchService", "No se pudo iniciar la búsqueda en segundo plano", error)
            stopSelf(); return START_NOT_STICKY
        }
        if (!started) {
            started = true
            scope.launch {
                AppContainer.riderSession.user.collectLatest {
                    if (!engine.canSearch()) { stopSelf(); return@collectLatest }
                    var nextSearch = SystemClock.elapsedRealtime() + 5_000
                    while (isActive && engine.canSearch()) {
                        try {
                            val search = SystemClock.elapsedRealtime() >= nextSearch
                            if (search) nextSearch = SystemClock.elapsedRealtime() + 20_000
                            val snapshot = engine.poll(search)
                            val offer = snapshot?.offer
                            val seconds = if (offer != null) ((offer.expiresAt - snapshot.serverTime + 999) / 1000).coerceAtLeast(0).toInt() else 0
                            AppContainer.offerNotifier.update(offer?.id, seconds)
                            AppContainer.orderChanges.tryEmit(Unit)
                        } catch (error: CancellationException) { throw error }
                        catch (error: Exception) { Log.w("OrderSearchService", "No se pudo consultar un pedido", error) }
                        delay(5_000)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        AppContainer.offerNotifier.update(null, 0)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val ID = 2008
        private const val CHANNEL = "chaski_availability"
        fun start(context: Context) {
            val permission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            if (!permission) return
            try { ContextCompat.startForegroundService(context, Intent(context, OrderSearchService::class.java)) }
            catch (error: RuntimeException) { Log.w("OrderSearchService", "No se pudo iniciar el servicio", error) }
        }
        fun stop(context: Context) { context.stopService(Intent(context, OrderSearchService::class.java)) }
    }
}

package com.example.chaskirider.data.orders

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.chaskirider.MainActivity
import com.example.chaskirider.R
import com.example.chaskirider.data.location.TrackingLocationSource
import com.example.chaskirider.domain.orders.OrderRecord
import com.example.chaskirider.domain.orders.OrderStatus
import com.example.chaskirider.domain.orders.OrderTracking
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class OrderTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var activeKey: String? = null
    private var database: DemoOrderDatabase? = null
    private val auth = FirebaseAuth.getInstance()
    private var uid: String? = null
    private val authListener = FirebaseAuth.AuthStateListener { if (it.currentUser?.uid != uid) stopSelf() }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val owner = intent?.getStringExtra("uid")
        val orderId = intent?.getStringExtra("orderId")
        if (owner == null || orderId == null || auth.currentUser?.uid != owner) {
            stopSelf(); return START_NOT_STICKY
        }
        val key = "$owner/$orderId"
        if (activeKey == key && job?.isActive == true) return START_NOT_STICKY
        uid = owner
        activeKey = key
        job?.cancel()
        try {
            val manager = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "Ubicación durante la entrega", NotificationManager.IMPORTANCE_LOW))
            ServiceCompat.startForeground(this, ID, notification("Preparando seguimiento del pedido"),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (error: RuntimeException) {
            Log.w("OrderTracking", "No se pudo iniciar el seguimiento", error)
            stopSelf(); return START_NOT_STICKY
        }
        auth.removeAuthStateListener(authListener)
        auth.addAuthStateListener(authListener)
        val db = database ?: openOrderDatabase(this).also { database = it }
        val gson = Gson()
        val source = TrackingLocationSource(this)
        val remote = FirebaseOrderTracking(owner, orderId)
        suspend fun current(): OrderRecord? {
            if (auth.currentUser?.uid != owner) return null
            return db.orders().record(owner, orderId)?.let { gson.fromJson(it.content, OrderRecord::class.java) }
        }
        val tracking = OrderTracking(owner, orderId, ::current, source::current, remote::publish)
        job = scope.launch {
            db.orders().observeRecord(owner, orderId).collectLatest { entity ->
                val record = entity?.let { gson.fromJson(it.content, OrderRecord::class.java) }
                if (record?.status != OrderStatus.ON_THE_WAY || record.riderId != owner || record.pickedUpAt == null) {
                    stopSelf(); return@collectLatest
                }
                while (isActive) {
                    try {
                        when (tracking.update()) {
                            OrderTracking.Result.STOPPED -> { stopSelf(); return@collectLatest }
                            OrderTracking.Result.WAITING_GPS -> updateNotification("Activa ubicación y revisa la señal GPS")
                            OrderTracking.Result.WAITING_SYNC -> updateNotification("Esperando sincronización del pedido")
                            OrderTracking.Result.SENT -> updateNotification("Compartiendo ubicación durante la entrega")
                        }
                    } catch (_: TimeoutCancellationException) { updateNotification("Sin conexión. Reintentando seguimiento") }
                    catch (error: CancellationException) { throw error }
                    catch (error: SecurityException) {
                        updateNotification("Permiso de ubicación requerido. Vuelve a Chaski Rider")
                        stopSelf(); return@collectLatest
                    } catch (error: Exception) {
                        Log.w("OrderTracking", "No se pudo actualizar la posición", error)
                        updateNotification("No se pudo enviar ubicación. Reintentando")
                    }
                    delay(15_000)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun notification(message: String): Notification {
        val pending = PendingIntent.getActivity(this, ID, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Entrega en curso").setContentText(message).setContentIntent(pending)
            .setOngoing(true).setOnlyAlertOnce(true).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(ID, notification(message))
    }

    override fun onDestroy() {
        auth.removeAuthStateListener(authListener)
        scope.cancel()
        database?.close()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val ID = 2009
        private const val CHANNEL = "chaski_delivery_tracking"
        fun start(context: Context, uid: String, orderId: String) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, OrderTrackingService::class.java)
                    .putExtra("uid", uid).putExtra("orderId", orderId))
            } catch (error: RuntimeException) { Log.w("OrderTracking", "Abre la app para reanudar el seguimiento", error) }
        }
        fun stop(context: Context) { context.stopService(Intent(context, OrderTrackingService::class.java)) }
    }
}

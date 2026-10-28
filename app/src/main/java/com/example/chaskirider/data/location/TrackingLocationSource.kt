package com.example.chaskirider.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.location.LocationManagerCompat
import com.example.chaskirider.domain.orders.TrackingPosition
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class TrackingLocationSource(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val manager = context.getSystemService(LocationManager::class.java)
    @SuppressLint("MissingPermission")
    suspend fun current(): TrackingPosition? {
        if (!LocationManagerCompat.isLocationEnabled(manager)) return null
        val token = CancellationTokenSource()
        return try {
            val location = withTimeoutOrNull(10_000) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
            } ?: return null
            if (!location.hasAccuracy() || SystemClock.elapsedRealtime() - location.elapsedRealtimeNanos / 1_000_000 !in 0..30_000)
                return null
            TrackingPosition(location.latitude, location.longitude, location.accuracy, location.time)
                .takeIf { it.isRecent(System.currentTimeMillis()) }
        } finally { token.cancel() }
    }
}

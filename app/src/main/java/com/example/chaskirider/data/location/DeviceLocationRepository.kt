package com.example.chaskirider.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.repository.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class DeviceLocationRepository(context: Context) : LocationRepository {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val manager = context.applicationContext.getSystemService(android.location.LocationManager::class.java)
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): Result<GeoPoint?> {
        if (!androidx.core.location.LocationManagerCompat.isLocationEnabled(manager)) return Result.success(null)
        val cancellation = CancellationTokenSource()
        return try {
            val location = withTimeoutOrNull(15_000.milliseconds) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await()
            } ?: run {
                client.lastLocation.await()
            }
            Result.success(location?.takeIf {
                it.hasAccuracy() && com.example.chaskirider.domain.orders.OrderLocationRules.usableFix(
                    it.latitude, it.longitude, it.accuracy, SystemClock.elapsedRealtime() - it.elapsedRealtimeNanos / 1_000_000)
            }?.let { GeoPoint(it.latitude, it.longitude) })
        } catch (error: CancellationException) { throw error
        } catch (error: Exception) {
            Result.failure(error)
        } finally { cancellation.cancel() }
    }
}

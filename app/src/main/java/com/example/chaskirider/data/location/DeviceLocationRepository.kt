package com.example.chaskirider.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.repository.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class DeviceLocationRepository(context: Context) : LocationRepository {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    @SuppressLint("MissingPermission") // La ruta solicita el permiso; SecurityException se devuelve como error.
    override suspend fun currentLocation(): Result<GeoPoint?> {
        val cancellation = CancellationTokenSource()
        Log.d("DeviceLocationRepo", "Requesting current location...")
        return try {
            val location = withTimeoutOrNull(15_000) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
            } ?: run {
                Log.w("DeviceLocationRepo", "getCurrentLocation returned null or timed out. Trying lastLocation fallback...")
                client.lastLocation.await()
            }
            if (location != null) {
                Log.d("DeviceLocationRepo", "Location obtained: lat=${location.latitude}, lng=${location.longitude}")
            } else {
                Log.w("DeviceLocationRepo", "Location is still null after lastLocation fallback.")
            }
            Result.success(location?.let { GeoPoint(it.latitude, it.longitude) })
        } catch (error: CancellationException) { throw error
        } catch (error: Exception) {
            Log.e("DeviceLocationRepo", "Error obtaining location", error)
            Result.failure(error)
        } finally { cancellation.cancel() }
    }
}

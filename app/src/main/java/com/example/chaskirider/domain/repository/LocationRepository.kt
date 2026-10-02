package com.example.chaskirider.domain.repository
import com.example.chaskirider.domain.model.GeoPoint
interface LocationRepository {
    suspend fun currentLocation(): Result<GeoPoint?>
}

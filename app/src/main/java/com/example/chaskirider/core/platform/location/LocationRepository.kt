package com.example.chaskirider.core.platform.location

import com.example.chaskirider.core.platform.location.GeoPoint

interface LocationRepository {
    suspend fun currentLocation(): Result<GeoPoint?>
}

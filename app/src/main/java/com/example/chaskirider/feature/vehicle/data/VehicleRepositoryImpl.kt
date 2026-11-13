package com.example.chaskirider.feature.vehicle.data

import com.example.chaskirider.core.firebase.firebaseResult
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.vehicle.data.remote.VehicleRemote
import com.example.chaskirider.feature.vehicle.domain.VehicleRepository
import com.example.chaskirider.feature.vehicle.domain.VehicleType

class VehicleRepositoryImpl(
    private val texts: TextProvider,
    private val remote: VehicleRemote,
    private val refreshUser: suspend () -> Unit
) : VehicleRepository {
    override suspend fun load() = firebaseResult(texts) { remote.load() }

    override suspend fun save(vehicle: VehicleType) = firebaseResult(texts) {
        remote.save(vehicle).also { refreshUser() }
    }
}

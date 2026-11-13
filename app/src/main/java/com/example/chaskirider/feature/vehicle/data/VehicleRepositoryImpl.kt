package com.example.chaskirider.feature.vehicle.data

import com.example.chaskirider.core.firebase.firebaseResult
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.vehicle.data.local.VehicleLocal
import com.example.chaskirider.feature.vehicle.data.remote.VehicleRemote
import com.example.chaskirider.feature.vehicle.domain.VehicleRepository
import com.example.chaskirider.feature.vehicle.domain.VehicleType

class VehicleRepositoryImpl(
    private val texts: TextProvider,
    private val remote: VehicleRemote,
    private val local: VehicleLocal,
    private val currentUid: () -> String?,
    private val refreshUser: suspend () -> Unit
) : VehicleRepository {
    override suspend fun load(): Result<VehicleType> {
        val uid = currentUid()
        val cached = uid?.let { runCatching { local.load(it) }.getOrNull() }
        return firebaseResult(texts) { remote.load() }.fold(
            onSuccess = { value ->
                uid?.let { runCatching { local.save(it, value) } }
                Result.success(value)
            },
            onFailure = { error -> cached?.let { Result.success(it) } ?: Result.failure(error) }
        )
    }

    override suspend fun save(vehicle: VehicleType): Result<VehicleType> = firebaseResult(texts) {
        remote.save(vehicle).also { saved ->
            currentUid()?.let { runCatching { local.save(it, saved) } }
            refreshUser()
        }
    }
}

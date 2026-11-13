package com.example.chaskirider.feature.vehicle.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.example.chaskirider.feature.vehicle.domain.VehicleType

@Entity(tableName = "vehicle_cache")
data class VehicleCacheEntity(@PrimaryKey val uid: String, val vehicleType: String)

@Dao
interface VehicleCacheDao {
    @Query("SELECT * FROM vehicle_cache WHERE uid = :uid")
    suspend fun load(uid: String): VehicleCacheEntity?

    @Upsert
    suspend fun save(entity: VehicleCacheEntity)

    @Query("DELETE FROM vehicle_cache WHERE uid = :uid")
    suspend fun clear(uid: String)
}

/** Caché local del tipo de vehículo para mostrarlo aunque Firestore no responda. */
interface VehicleLocal {
    suspend fun load(uid: String): VehicleType?
    suspend fun save(uid: String, vehicle: VehicleType)
    suspend fun clear(uid: String)
}

class RoomVehicleLocal(private val dao: VehicleCacheDao) : VehicleLocal {
    override suspend fun load(uid: String) = dao.load(uid)?.let {
        VehicleType.fromId(it.vehicleType)
    }

    override suspend fun save(uid: String, vehicle: VehicleType) =
        dao.save(VehicleCacheEntity(uid, vehicle.name))

    override suspend fun clear(uid: String) = dao.clear(uid)
}

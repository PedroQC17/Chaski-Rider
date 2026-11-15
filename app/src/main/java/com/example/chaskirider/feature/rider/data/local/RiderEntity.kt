package com.example.chaskirider.feature.rider.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Upsert
import com.example.chaskirider.feature.rider.domain.Rider

@Entity(tableName = "riders")
data class RiderEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val lastName: String,
    val dni: String,
    val phone: String,
    val email: String,
    val profilePhotoPath: String,
    val vehicleType: String?,
    val status: String,
    val rejectionReason: String,
    val submissionVersion: Long,
    val termsAccepted: Boolean,
    val isAvailable: Boolean,
    val isEnabled: Boolean
)

@Dao
interface RiderDao {
    @Upsert
    suspend fun save(entity: RiderEntity)
}

class RiderStore(private val dao: RiderDao) {
    suspend fun save(user: Rider) = dao.save(user.toEntity())
}

fun Rider.toEntity() = RiderEntity(
    uid = id,
    name = name,
    lastName = lastName,
    dni = dni,
    phone = phone,
    email = email,
    profilePhotoPath = profilePhotoPath,
    vehicleType = vehicleTypeId,
    status = status.name,
    rejectionReason = rejectionReason,
    submissionVersion = submissionVersion,
    termsAccepted = termsAccepted,
    isAvailable = isAvailable,
    isEnabled = isEnabled
)

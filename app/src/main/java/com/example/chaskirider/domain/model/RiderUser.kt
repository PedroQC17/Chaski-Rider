package com.example.chaskirider.domain.model

data class RiderUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val vehicleType: VehicleType = VehicleType.NONE,
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = true,
    val documentStatus: DocumentStatus = DocumentStatus.PENDING
)

enum class DocumentStatus {
    PENDING,
    APPROVED,
    OBSERVED
}

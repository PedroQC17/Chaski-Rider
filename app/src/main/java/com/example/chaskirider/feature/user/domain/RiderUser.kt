package com.example.chaskirider.feature.user.domain

data class RiderUser(
    val id: String = "",
    val name: String = "",
    val lastName: String = "",
    val dni: String = "",
    val phone: String = "",
    val email: String = "",
    val profilePhotoPath: String = "",
    val hasPassword: Boolean = false,
    val vehicleTypeId: String? = null,
    val status: RegistrationStatus = RegistrationStatus.INCOMPLETE,
    val rejectionReason: String = "",
    val submissionVersion: Long = 0,
    val currentStep: Int = 1,
    val termsAccepted: Boolean = false,
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = true
)

enum class RegistrationStatus {
    INCOMPLETE,
    PENDING_REVIEW,
    APPROVED,
    NEEDS_CORRECTION,
    REJECTED
}

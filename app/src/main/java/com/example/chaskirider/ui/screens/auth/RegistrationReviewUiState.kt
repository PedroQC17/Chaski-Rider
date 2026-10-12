package com.example.chaskirider.ui.screens.auth

import com.example.chaskirider.domain.model.*

enum class RegistrationReviewStage { INCOMPLETE, PENDING, CORRECTION, REJECTED, APPROVED, DISABLED }

data class RegistrationReviewUiState(
    val user: RiderUser? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
) {
    // Account restrictions take priority over the registration decision.
    val stage: RegistrationReviewStage get() = when {
        user?.isEnabled == false -> RegistrationReviewStage.DISABLED
        else -> when (user?.status) {
            RegistrationStatus.PENDING_REVIEW -> RegistrationReviewStage.PENDING
            RegistrationStatus.NEEDS_CORRECTION -> RegistrationReviewStage.CORRECTION
            RegistrationStatus.REJECTED -> RegistrationReviewStage.REJECTED
            RegistrationStatus.APPROVED -> RegistrationReviewStage.APPROVED
            else -> RegistrationReviewStage.INCOMPLETE
        }
    }
    val canEdit get() = user != null && stage in listOf(RegistrationReviewStage.INCOMPLETE, RegistrationReviewStage.CORRECTION)
    val canWork get() = user != null && stage == RegistrationReviewStage.APPROVED
}

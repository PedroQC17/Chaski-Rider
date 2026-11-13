package com.example.chaskirider.feature.user.ui.viewmodel

import com.example.chaskirider.feature.documents.domain.DocumentReview
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RegistrationStatus

enum class RegistrationReviewStage { INCOMPLETE, PENDING, CORRECTION, REJECTED, APPROVED, DISABLED }

data class RegistrationReviewUiState(
    val user: RiderUser? = null,
    val documentReviews: Map<String, DocumentReview> = emptyMap(),
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

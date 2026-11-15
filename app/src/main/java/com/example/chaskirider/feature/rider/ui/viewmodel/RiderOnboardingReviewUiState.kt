package com.example.chaskirider.feature.rider.ui.viewmodel

import com.example.chaskirider.feature.documents.domain.DocumentReview
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderStatus

enum class RiderOnboardingReviewStage { INCOMPLETE, PENDING, CORRECTION, REJECTED, APPROVED, DISABLED }

data class RiderOnboardingReviewUiState(
    val user: Rider? = null,
    val documentReviews: Map<String, DocumentReview> = emptyMap(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
) {
    // Account restrictions take priority over the registration decision.
    val stage: RiderOnboardingReviewStage get() = when {
        user?.isEnabled == false -> RiderOnboardingReviewStage.DISABLED
        else -> when (user?.status) {
            RiderStatus.PENDING_REVIEW -> RiderOnboardingReviewStage.PENDING
            RiderStatus.NEEDS_CORRECTION -> RiderOnboardingReviewStage.CORRECTION
            RiderStatus.REJECTED -> RiderOnboardingReviewStage.REJECTED
            RiderStatus.APPROVED -> RiderOnboardingReviewStage.APPROVED
            else -> RiderOnboardingReviewStage.INCOMPLETE
        }
    }
    val canEdit get() = user != null && stage in listOf(RiderOnboardingReviewStage.INCOMPLETE, RiderOnboardingReviewStage.CORRECTION)
    val canWork get() = user != null && stage == RiderOnboardingReviewStage.APPROVED
}

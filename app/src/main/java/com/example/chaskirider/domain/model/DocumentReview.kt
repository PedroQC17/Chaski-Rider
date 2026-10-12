package com.example.chaskirider.domain.model

enum class DocumentReviewStatus { PENDING, APPROVED, OBSERVED }

data class DocumentReview(
    val status: DocumentReviewStatus = DocumentReviewStatus.PENDING,
    val reason: String = ""
)

package com.example.chaskirider.feature.orders.domain

enum class EvidenceIssue {
    UNREADABLE,
    BLURRY,
    TOO_DARK,
    LOW_RESOLUTION
}

data class EvidenceQuality(val issues: List<EvidenceIssue> = emptyList()) {
    val sufficient: Boolean get() = issues.isEmpty()

    companion object {
        const val MIN_DIMENSION = 480
        const val DARK_LUMINANCE = 45.0
        const val BLUR_VARIANCE = 80.0

        fun fromMetrics(
            width: Int,
            height: Int,
            meanLuminance: Double,
            laplacianVariance: Double
        ): EvidenceQuality {
            if (width <= 0 || height <= 0) {
                return EvidenceQuality(listOf(EvidenceIssue.UNREADABLE))
            }
            val issues = buildList {
                if (minOf(width, height) < MIN_DIMENSION) add(EvidenceIssue.LOW_RESOLUTION)
                if (meanLuminance < DARK_LUMINANCE) add(EvidenceIssue.TOO_DARK)
                if (laplacianVariance < BLUR_VARIANCE) add(EvidenceIssue.BLURRY)
            }
            return EvidenceQuality(issues.distinct())
        }
    }
}

interface EvidenceAnalyzer {
    suspend fun analyze(uri: String): EvidenceQuality
}

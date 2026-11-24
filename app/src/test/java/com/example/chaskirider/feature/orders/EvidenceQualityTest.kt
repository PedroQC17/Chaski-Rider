package com.example.chaskirider.feature.orders

import com.example.chaskirider.feature.orders.domain.EvidenceIssue
import com.example.chaskirider.feature.orders.domain.EvidenceQuality
import org.junit.Assert.*
import org.junit.Test

class EvidenceQualityTest {
    @Test fun acceptsWellLitSharpHighResolutionPhoto() {
        val quality = EvidenceQuality.fromMetrics(
            width = 1200,
            height = 1600,
            meanLuminance = 120.0,
            laplacianVariance = 200.0
        )

        assertTrue(quality.sufficient)
        assertTrue(quality.issues.isEmpty())
    }

    @Test fun detectsDarkPhoto() {
        val quality = EvidenceQuality.fromMetrics(
            width = 1200,
            height = 1600,
            meanLuminance = 10.0,
            laplacianVariance = 200.0
        )

        assertEquals(listOf(EvidenceIssue.TOO_DARK), quality.issues)
    }

    @Test fun detectsBlurryPhoto() {
        val quality = EvidenceQuality.fromMetrics(
            width = 1200,
            height = 1600,
            meanLuminance = 120.0,
            laplacianVariance = 20.0
        )

        assertEquals(listOf(EvidenceIssue.BLURRY), quality.issues)
    }

    @Test fun detectsInsufficientResolution() {
        val quality = EvidenceQuality.fromMetrics(
            width = 320,
            height = 480,
            meanLuminance = 120.0,
            laplacianVariance = 200.0
        )

        assertEquals(listOf(EvidenceIssue.LOW_RESOLUTION), quality.issues)
    }

    @Test fun reportsUnreadableWhenDimensionsAreInvalid() {
        val quality = EvidenceQuality.fromMetrics(
            width = 0,
            height = 0,
            meanLuminance = 0.0,
            laplacianVariance = 0.0
        )

        assertEquals(listOf(EvidenceIssue.UNREADABLE), quality.issues)
        assertFalse(quality.sufficient)
    }
}

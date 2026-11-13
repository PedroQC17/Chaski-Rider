package com.example.chaskirider

import com.example.chaskirider.feature.orders.domain.CallAttemptDetector
import com.example.chaskirider.feature.orders.domain.PhoneCallState.*
import org.junit.Assert.*
import org.junit.Test

class CallAttemptDetectorTest {
    @Test fun onlyCountsOnceAfterAnExplicitAttemptFromIdle() {
        val detector = CallAttemptDetector()
        assertFalse(detector.arm())
        detector.onState(IDLE)
        assertTrue(detector.arm())
        assertFalse(detector.arm())
        assertTrue(detector.onState(OFFHOOK))
        assertFalse(detector.onState(OFFHOOK))
        assertFalse(detector.onState(IDLE))
        assertTrue(detector.arm())
        assertTrue(detector.onState(OFFHOOK))
    }

    @Test fun incomingOrExistingCallsAndFailedLaunchesDoNotCount() {
        val detector = CallAttemptDetector()
        assertFalse(detector.onState(OFFHOOK))
        assertFalse(detector.arm())
        detector.onState(IDLE)
        detector.arm()
        detector.onState(RINGING)
        assertFalse(detector.onState(OFFHOOK))
        detector.onState(IDLE)
        detector.arm()
        detector.cancel()
        assertFalse(detector.onState(OFFHOOK))
    }
}

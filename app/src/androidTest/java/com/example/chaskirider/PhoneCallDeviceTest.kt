package com.example.chaskirider

import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.platform.DevicePhoneCalls
import com.example.chaskirider.feature.orders.domain.CallAttemptDetector
import com.example.chaskirider.feature.orders.domain.PhoneCallState
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class PhoneCallDeviceTest {
    @Test fun realOutgoingAttemptIsObservedOnce() = runBlocking {
        val number = InstrumentationRegistry.getArguments().getString("testCallNumber")
        assumeTrue("Solo ejecutar una llamada con destino autorizado explícitamente", !number.isNullOrBlank())
        val calls = DevicePhoneCalls(InstrumentationRegistry.getInstrumentation().targetContext)
        val detector = CallAttemptDetector()
        val ready = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var count = 0
        val job = launch(Dispatchers.Main.immediate) {
            calls.states().collect { state ->
                if (state == PhoneCallState.IDLE && !ready.isCompleted) ready.complete(Unit)
                if (detector.onState(state)) {
                    count++
                    android.util.Log.i("ChaskiCallTest", "ATTEMPT_OBSERVED")
                    started.complete(Unit)
                }
                if (state == PhoneCallState.IDLE && started.isCompleted) finished.complete(Unit)
            }
        }
        try {
            withTimeout(10_000) { ready.await() }
            withContext(Dispatchers.Main) {
                check(detector.arm())
                try { calls.start(number!!) } catch (error: Exception) { detector.cancel(); throw error }
            }
            withTimeout(35_000) { started.await() }
            withTimeout(35_000) { finished.await() }
            assertEquals(1, count)
        } finally {
            job.cancelAndJoin()
        }
    }
}

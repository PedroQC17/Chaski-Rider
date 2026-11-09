package com.example.chaskirider.domain.support

enum class PhoneCallState { IDLE, RINGING, OFFHOOK }

interface PhoneCalls {
    fun states(): kotlinx.coroutines.flow.Flow<PhoneCallState>
    fun start(number: String)
}

class CallAttemptDetector {
    private var state: PhoneCallState? = null
    private var armed = false

    fun arm(): Boolean {
        if (state != PhoneCallState.IDLE || armed) return false
        armed = true
        return true
    }

    fun cancel() { armed = false }

    fun onState(value: PhoneCallState): Boolean {
        val previous = state
        state = value
        if (value == PhoneCallState.RINGING) armed = false
        if (armed && previous == PhoneCallState.IDLE && value == PhoneCallState.OFFHOOK) {
            armed = false
            return true
        }
        return false
    }
}

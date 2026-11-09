package com.example.chaskirider.domain.support

data class ContactCheckpoint(val firstCallAt: Long? = null, val secondCallAt: Long? = null)
interface CustomerAbsenceService {
    suspend fun arrive(uid: String, orderId: String, latitude: Double, longitude: Double)
    suspend fun call(uid: String, orderId: String, attempt: Int): ContactCheckpoint
}

package com.example.chaskirider.domain.support

data class CustomerContact(
    val arrivedAt: Long,
    val firstCallAt: Long? = null,
    val firstCallElapsed: Long? = null,
    val boot: Int? = null,
    val secondCallAt: Long? = null,
    val reminderSeen: Boolean = false,
    val phone: String? = null,
    val serverRegistered: Boolean = false
) {
    fun elapsedMillis(now: Long, elapsed: Long, currentBoot: Int): Long {
        val started = firstCallAt ?: return 0
        return if (boot == currentBoot && firstCallElapsed != null)
            (elapsed - firstCallElapsed).coerceAtLeast(0)
        else (now - started).coerceAtLeast(0)
    }
    fun canCallAgain(waited: Long) = firstCallAt == null || (secondCallAt == null && waited >= 180_000)
    fun canContactSupport(waited: Long) = firstCallAt != null && secondCallAt != null && waited >= 600_000
    fun showReminder(waited: Long) = firstCallAt != null && !reminderSeen && waited in 480_000 until 600_000
}

interface CustomerContactStore {
    suspend fun load(uid: String, orderId: String): CustomerContact?
    suspend fun save(uid: String, orderId: String, contact: CustomerContact)
}

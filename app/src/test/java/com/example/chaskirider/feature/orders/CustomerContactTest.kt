package com.example.chaskirider.feature.orders
import com.example.chaskirider.feature.orders.domain.CustomerContact
import org.junit.Assert.*
import org.junit.Test

class CustomerContactTest {
    @Test fun twoCallsAndFullTenMinutesAreRequired() {
        val arrived = CustomerContact(100)
        assertTrue(arrived.canCallAgain(0))
        assertFalse(arrived.canContactSupport(900_000))
        val first = arrived.copy(firstCallAt = 200, firstCallElapsed = 20, boot = 1)
        assertFalse(first.canCallAgain(179_999))
        assertTrue(first.canCallAgain(180_000))
        assertFalse(first.canContactSupport(600_000))
        val second = first.copy(secondCallAt = 180_200)
        assertFalse(second.canCallAgain(900_000))
        assertFalse(second.canContactSupport(599_999))
        assertTrue(second.canContactSupport(600_000))
    }

    @Test fun reminderIsShownOnceDuringTheLastTwoMinutes() {
        val progress = CustomerContact(100, firstCallAt = 200)
        assertFalse(progress.showReminder(479_999))
        assertTrue(progress.showReminder(480_000))
        assertTrue(progress.showReminder(599_999))
        assertFalse(progress.showReminder(600_000))
        assertFalse(progress.copy(reminderSeen = true).showReminder(500_000))
    }

    @Test fun sameBootIgnoresWallClockEditsAndRebootUsesPersistedTime() {
        val progress = CustomerContact(100, firstCallAt = 1000, firstCallElapsed = 2000, boot = 4)
        assertEquals(3000L, progress.elapsedMillis(900_000, 5000, 4))
        assertEquals(3000L, progress.elapsedMillis(0, 5000, 4))
        assertEquals(6000L, progress.elapsedMillis(7000, 50, 5))
        assertEquals(0L, progress.elapsedMillis(500, 50, 5))
    }
}

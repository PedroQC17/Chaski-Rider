package com.example.chaskirider

import com.example.chaskirider.domain.orders.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OrderTrackingTest {
    private val point = RoutePoint(-12.0, -77.0)
    private val record = OrderRecord("order", "alice", OrderStatus.ON_THE_WAY, "Local", point, point, 1, 2, 3, null)
    private val position = TrackingPosition(-12.0, -77.0, 10f, 100_000)

    @Test fun onlyPickedUpActiveOrderCanRequestOrPublishPosition() = runTest {
        for (status in OrderStatus.entries) {
            var requested = false
            var published = false
            val engine = OrderTracking("alice", "order", { record.copy(status = status) },
                { requested = true; position }, { published = true; true }, { 100_000 })
            val result = engine.update()
            assertEquals(status == OrderStatus.ON_THE_WAY, requested)
            assertEquals(status == OrderStatus.ON_THE_WAY, published)
            assertEquals(if (status == OrderStatus.ON_THE_WAY) OrderTracking.Result.SENT else OrderTracking.Result.STOPPED, result)
        }
    }

    @Test fun accountOrStageChangeWhileObtainingGpsPreventsPublish() = runTest {
        var active: OrderRecord? = record
        var published = false
        val engine = OrderTracking("alice", "order", { active }, { active = null; position },
            { published = true; true }, { 100_000 })
        assertEquals(OrderTracking.Result.STOPPED, engine.update())
        assertFalse(published)
        active = record.copy(riderId = "bob")
        assertEquals(OrderTracking.Result.STOPPED, engine.update())
    }

    @Test fun staleFutureInaccurateAndInvalidPositionsAreNotSent() = runTest {
        for (sample in listOf(position.copy(recordedAt = 69_999), position.copy(recordedAt = 100_001),
                position.copy(accuracyMeters = 101f), position.copy(accuracyMeters = Float.NaN), position.copy(latitude = 91.0))) {
            var published = false
            val engine = OrderTracking("alice", "order", { record }, { sample }, { published = true; true }, { 100_000 })
            assertEquals(OrderTracking.Result.WAITING_GPS, engine.update())
            assertFalse(published)
        }
    }

    @Test fun waitingForRemoteRecordDoesNotInventSuccessfulTransmission() = runTest {
        val engine = OrderTracking("alice", "order", { record }, { position }, { false }, { 100_000 })
        assertEquals(OrderTracking.Result.WAITING_SYNC, engine.update())
    }
}

package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.orders.*
import java.util.UUID

/**
 * Repositorio de demo puramente local con datos simulados sin conexión al backend.
 * Útil para pruebas y demostraciones cuando no hay backend disponible.
 */
class LocalDemoOfferRepository : OfferRepository {
    
    override suspend fun execute(
        action: OfferAction,
        offerId: String?,
        lat: Double?,
        lng: Double?
    ): OfferSnapshot {
        return when (action) {
            OfferAction.OFFER -> generateMockOffer()
            OfferAction.ACCEPT -> generateMockAcceptedOffer(offerId ?: "")
            OfferAction.REJECT -> generateEmptySnapshot()
            OfferAction.STATE -> generateEmptySnapshot()
            OfferAction.ARRIVE_MERCHANT -> generateArrivedAtMerchant()
            OfferAction.REPORT_NOT_READY -> generateNotReadyReported()
            OfferAction.PICKUP -> generatePickedUp()
            OfferAction.DELIVER -> generateDelivered()
            OfferAction.RESET -> generateEmptySnapshot()
        }
    }
    
    private fun generateMockOffer(): OfferSnapshot {
        val now = System.currentTimeMillis()
        val expiresAt = now + 60_000 // 1 minuto para aceptar
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = false,
            batch = null,
            offer = DeliveryOffer(
                id = UUID.randomUUID().toString(),
                expiresAt = expiresAt,
                quote = DeliveryQuote(
                    stops = listOf(
                        DeliveryStop(
                            id = "stop_1",
                            orderId = "order_123",
                            zone = "Miraflores",
                            point = RoutePoint(-12.1195, -77.0302)
                        )
                    ),
                    approachMeters = 1200,
                    deliveryMeters = 3500,
                    durationSeconds = 1800,
                    baseCents = 500,
                    approachCents = 200,
                    deliveryCents = 800,
                    tipCents = 150,
                    guaranteeCents = 650,
                    totalCents = 1150,
                    additionalCents = 0,
                    path = listOf(
                        RoutePoint(-12.1195, -77.0302),
                        RoutePoint(-12.1215, -77.0285),
                        RoutePoint(-12.1240, -77.0270)
                    )
                ),
                reason = "proximidad",
                reasonMeters = 1200
            ),
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = null,
            waitStartedAt = null,
            isNotReadyReported = false,
            waitingCompensationCents = 0,
            pickedUpAt = null,
            deliveredAt = null,
            isDelivered = false
        )
    }
    
    private fun generateMockAcceptedOffer(offerId: String): OfferSnapshot {
        val now = System.currentTimeMillis()
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = false,
            batch = DeliveryQuote(
                stops = listOf(
                    DeliveryStop(
                        id = "stop_1",
                        orderId = "order_123",
                        zone = "Miraflores",
                        point = RoutePoint(-12.1195, -77.0302)
                    )
                ),
                approachMeters = 1200,
                deliveryMeters = 3500,
                durationSeconds = 1800,
                baseCents = 500,
                approachCents = 200,
                deliveryCents = 800,
                tipCents = 150,
                guaranteeCents = 650,
                totalCents = 1150,
                additionalCents = 0,
                path = listOf(
                    RoutePoint(-12.1195, -77.0302),
                    RoutePoint(-12.1215, -77.0285),
                    RoutePoint(-12.1240, -77.0270)
                )
            ),
            offer = null,
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = null,
            waitStartedAt = null,
            isNotReadyReported = false,
            waitingCompensationCents = 0,
            pickedUpAt = null,
            deliveredAt = null,
            isDelivered = false
        )
    }
    
    private fun generateArrivedAtMerchant(): OfferSnapshot {
        val now = System.currentTimeMillis()
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = false,
            batch = DeliveryQuote(
                stops = listOf(
                    DeliveryStop(
                        id = "stop_1",
                        orderId = "order_123",
                        zone = "Miraflores",
                        point = RoutePoint(-12.1195, -77.0302)
                    )
                ),
                approachMeters = 1200,
                deliveryMeters = 3500,
                durationSeconds = 1800,
                baseCents = 500,
                approachCents = 200,
                deliveryCents = 800,
                tipCents = 150,
                guaranteeCents = 650,
                totalCents = 1150,
                additionalCents = 0,
                path = listOf(
                    RoutePoint(-12.1195, -77.0302),
                    RoutePoint(-12.1215, -77.0285),
                    RoutePoint(-12.1240, -77.0270)
                )
            ),
            offer = null,
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = now,
            waitStartedAt = null,
            isNotReadyReported = false,
            waitingCompensationCents = 0,
            pickedUpAt = null,
            deliveredAt = null,
            isDelivered = false
        )
    }
    
    private fun generateNotReadyReported(): OfferSnapshot {
        val now = System.currentTimeMillis()
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = false,
            batch = DeliveryQuote(
                stops = listOf(
                    DeliveryStop(
                        id = "stop_1",
                        orderId = "order_123",
                        zone = "Miraflores",
                        point = RoutePoint(-12.1195, -77.0302)
                    )
                ),
                approachMeters = 1200,
                deliveryMeters = 3500,
                durationSeconds = 1800,
                baseCents = 500,
                approachCents = 200,
                deliveryCents = 800,
                tipCents = 150,
                guaranteeCents = 650,
                totalCents = 1150,
                additionalCents = 0,
                path = listOf(
                    RoutePoint(-12.1195, -77.0302),
                    RoutePoint(-12.1215, -77.0285),
                    RoutePoint(-12.1240, -77.0270)
                )
            ),
            offer = null,
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = now,
            waitStartedAt = now,
            isNotReadyReported = true,
            waitingCompensationCents = 100,
            pickedUpAt = null,
            deliveredAt = null,
            isDelivered = false
        )
    }
    
    private fun generatePickedUp(): OfferSnapshot {
        val now = System.currentTimeMillis()
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = true,
            batch = DeliveryQuote(
                stops = listOf(
                    DeliveryStop(
                        id = "stop_1",
                        orderId = "order_123",
                        zone = "Miraflores",
                        point = RoutePoint(-12.1195, -77.0302)
                    )
                ),
                approachMeters = 1200,
                deliveryMeters = 3500,
                durationSeconds = 1800,
                baseCents = 500,
                approachCents = 200,
                deliveryCents = 800,
                tipCents = 150,
                guaranteeCents = 650,
                totalCents = 1150,
                additionalCents = 0,
                path = listOf(
                    RoutePoint(-12.1195, -77.0302),
                    RoutePoint(-12.1215, -77.0285),
                    RoutePoint(-12.1240, -77.0270)
                )
            ),
            offer = null,
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = now,
            waitStartedAt = null,
            isNotReadyReported = false,
            waitingCompensationCents = 0,
            pickedUpAt = now,
            deliveredAt = null,
            isDelivered = false
        )
    }
    
    private fun generateDelivered(): OfferSnapshot {
        val now = System.currentTimeMillis()
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = true,
            batch = DeliveryQuote(
                stops = listOf(
                    DeliveryStop(
                        id = "stop_1",
                        orderId = "order_123",
                        zone = "Miraflores",
                        point = RoutePoint(-12.1195, -77.0302)
                    )
                ),
                approachMeters = 1200,
                deliveryMeters = 3500,
                durationSeconds = 1800,
                baseCents = 500,
                approachCents = 200,
                deliveryCents = 800,
                tipCents = 150,
                guaranteeCents = 650,
                totalCents = 1150,
                additionalCents = 0,
                path = listOf(
                    RoutePoint(-12.1195, -77.0302),
                    RoutePoint(-12.1215, -77.0285),
                    RoutePoint(-12.1240, -77.0270)
                )
            ),
            offer = null,
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = now,
            waitStartedAt = null,
            isNotReadyReported = false,
            waitingCompensationCents = 0,
            pickedUpAt = now,
            deliveredAt = now,
            isDelivered = true
        )
    }
    
    private fun generateEmptySnapshot(): OfferSnapshot {
        val now = System.currentTimeMillis()
        
        return OfferSnapshot(
            serverTime = now,
            pickedUp = false,
            batch = null,
            offer = null,
            merchant = RoutePoint(-12.1195, -77.0302),
            merchantName = "Restaurant Central",
            merchantZone = "Miraflores",
            demandZones = emptyList(),
            arrivedAt = null,
            waitStartedAt = null,
            isNotReadyReported = false,
            waitingCompensationCents = 0,
            pickedUpAt = null,
            deliveredAt = null,
            isDelivered = false
        )
    }
}

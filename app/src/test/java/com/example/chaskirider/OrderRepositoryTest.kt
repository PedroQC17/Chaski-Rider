package com.example.chaskirider

// HU06 - Parte 1: reglas del repositorio de pedidos (mock):
// - Solo un repartidor Disponible recibe ofertas.
// - Una oferta a la vez; con pedido activo no se ofrecen más (sin conflictos).
// - Aceptar marca el pedido activo; rechazar permite recibir la siguiente.
// - Aceptar/Rechazar solo funciona con la oferta vigente.
import com.example.chaskirider.data.repository.OrderRepositoryImpl
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {
    private fun availableRider() = RiderUser(
        id = "u1",
        status = RegistrationStatus.APPROVED,
        isEnabled = true,
        isAvailable = true,
        vehicleType = VehicleType.MOTORCYCLE
    )

    @Test fun onlyAvailableRidersReceiveOffers() = runBlocking {
        val repo = OrderRepositoryImpl()
        val denied = repo.nextOffer(availableRider().copy(isAvailable = false))
        assertTrue(denied.isFailure)
        assertEquals("Solo los repartidores disponibles pueden recibir ofertas", denied.exceptionOrNull()?.message)
        assertTrue(repo.nextOffer(availableRider()).isSuccess)
    }

    @Test fun offerContainsPickupDestinationFareAndReason() = runBlocking {
        val offer = OrderRepositoryImpl().nextOffer(availableRider()).getOrThrow()
        assertTrue(offer.pickupAddress.isNotBlank())
        assertTrue(offer.destinationAddress.isNotBlank())
        assertTrue(offer.fareSoles > 0.0)
        assertTrue(offer.distanceKm > 0.0)
        assertTrue(offer.reason.contains("km"))
        assertTrue(offer.reason.contains("motocicleta"))
    }

    @Test fun onlyOneOfferAtATime() = runBlocking {
        val repo = OrderRepositoryImpl()
        val first = repo.nextOffer(availableRider()).getOrThrow()
        assertTrue(repo.nextOffer(availableRider()).isFailure)
        repo.reject(first.id)
        assertTrue(repo.nextOffer(availableRider()).isSuccess)
    }

    @Test fun acceptMarksActiveOrderAndBlocksNewOffers() = runBlocking {
        val repo = OrderRepositoryImpl()
        val offer = repo.nextOffer(availableRider()).getOrThrow()
        val accepted = repo.accept(offer.id).getOrThrow()
        assertEquals(offer.id, accepted.id)
        assertEquals(offer.id, repo.acceptedOrder.value?.id)
        assertTrue(repo.nextOffer(availableRider()).isFailure)
    }

    @Test fun cannotAcceptOrRejectAnOfferThatIsNotCurrent() = runBlocking {
        val repo = OrderRepositoryImpl()
        val offer = repo.nextOffer(availableRider()).getOrThrow()
        assertTrue(repo.accept("oferta-ajena").isFailure)
        assertTrue(repo.reject("oferta-ajena").isFailure)
        assertTrue(repo.accept(offer.id).isSuccess)
    }

    @Test fun rejectClearsOfferAndKeepsRiderSearching() = runBlocking {
        val repo = OrderRepositoryImpl()
        val offer = repo.nextOffer(availableRider()).getOrThrow()
        assertTrue(repo.reject(offer.id).isSuccess)
        assertNull(repo.acceptedOrder.value)
        assertTrue(repo.accept(offer.id).isFailure)
    }
}

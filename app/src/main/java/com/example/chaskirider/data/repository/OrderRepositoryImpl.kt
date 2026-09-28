
package com.example.chaskirider.data.repository

import com.example.chaskirider.domain.model.RideOffer
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType
import com.example.chaskirider.domain.repository.OrderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.round
import kotlin.random.Random

class OrderRepositoryImpl : OrderRepository {

    private val _acceptedOrder = MutableStateFlow<RideOffer?>(null)
    override val acceptedOrder: StateFlow<RideOffer?> = _acceptedOrder.asStateFlow()

    private var currentOffer: RideOffer? = null

    override suspend fun nextOffer(user: RiderUser): Result<RideOffer> = safe {
        require(user.isAvailable) { "Solo los repartidores disponibles pueden recibir ofertas" }
        require(_acceptedOrder.value == null) { "Ya tienes un pedido activo; termínalo antes de recibir otra oferta" }
        require(currentOffer == null) { "Ya tienes una oferta pendiente de respuesta" }
        createOffer(user.vehicleType).also { currentOffer = it }
    }

    override suspend fun accept(offerId: String): Result<RideOffer> = safe {
        val offer = requireNotNull(currentOffer) { "No tienes una oferta activa" }
        require(offer.id == offerId) { "Esta oferta ya no está disponible" }
        currentOffer = null
        _acceptedOrder.value = offer
        offer
    }

    override suspend fun reject(offerId: String): Result<Unit> = safe {
        val offer = requireNotNull(currentOffer) { "No tienes una oferta activa" }
        require(offer.id == offerId) { "Esta oferta ya no está disponible" }
        currentOffer = null
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) { Result.failure(e) }

    private fun createOffer(vehicle: VehicleType): RideOffer {
        val distance = round1(0.3 + Random.nextDouble() * 4.5)
        val fare = round(fareBase(distance) * 2) / 2.0
        return RideOffer(
            id = UUID.randomUUID().toString(),
            pickupAddress = PICKUPS.random(),
            destinationAddress = DESTINATIONS.random(),
            fareSoles = fare,
            distanceKm = distance,
            
            reason = "Estás a $distance km de la recogida y tu ${vehicleLabel(vehicle)} es compatible con este pedido."
        )
    }

    private fun fareBase(distance: Double) = 5.0 + distance * 2.2
    private fun round1(value: Double) = round(value * 10) / 10.0

    private fun vehicleLabel(vehicle: VehicleType) = when (vehicle) {
        VehicleType.BICYCLE -> "bicicleta"
        VehicleType.MOTORCYCLE -> "motocicleta"
        VehicleType.CAR -> "automóvil"
        VehicleType.NONE -> "vehículo"
    }

    companion object {
        private val PICKUPS = listOf(
            "Av. Larco 120, Miraflores",
            "Av. Pardo 540, Miraflores",
            "Jr. de la Unión 450, Cercado de Lima",
            "Av. Arequipa 2650, Lince",
            "Av. Petit Thouars 3400, Miraflores",
            "Av. Benavides 1180, Surquillo"
        )
        private val DESTINATIONS = listOf(
            "Av. La Marina 2000, San Miguel",
            "Av. Brasil 2900, Jesús María",
            "Av. Primavera 1500, Surco",
            "Av. Angamos 600, Miraflores",
            "Av. Universitaria 1200, San Martín de Porres",
            "Av. El Sol 650, Cercado de Lima"
        )
    }
}

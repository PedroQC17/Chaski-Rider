package com.example.chaskirider.feature.rider.domain

import com.example.chaskirider.feature.rider.domain.RiderPolicy

import com.example.chaskirider.feature.rider.domain.VehiclePolicy
import com.example.chaskirider.feature.rider.domain.VehicleType

object RiderOnboardingService {
    fun normalizePhone(phone: String): String = RiderPolicy.normalizePhone(phone)

    fun nextStep(user: Rider): Int = when {
        !user.hasPassword -> 1
        RiderPolicy.personalError(user.name, user.lastName, user.dni, user.phone, user.termsAccepted) != null -> 1
        !VehiclePolicy.isSelected(VehicleType.fromId(user.vehicleTypeId)) -> 2
        else -> 3
    }
}

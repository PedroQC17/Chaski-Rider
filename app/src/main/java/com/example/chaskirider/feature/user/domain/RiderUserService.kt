package com.example.chaskirider.feature.user.domain

import com.example.chaskirider.feature.user.domain.RiderUserPolicy

import com.example.chaskirider.feature.vehicle.domain.VehiclePolicy
import com.example.chaskirider.feature.vehicle.domain.VehicleType

object RiderUserService {
    fun normalizePhone(phone: String): String = RiderUserPolicy.normalizePhone(phone)

    fun nextStep(user: RiderUser): Int = when {
        !user.hasPassword -> 1
        RiderUserPolicy.personalError(user.name, user.lastName, user.dni, user.phone, user.termsAccepted) != null -> 1
        !VehiclePolicy.isSelected(VehicleType.fromId(user.vehicleTypeId)) -> 2
        else -> 3
    }
}

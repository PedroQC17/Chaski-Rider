package com.example.chaskirider.feature.vehicle.domain

object VehiclePolicy {
    val selectable = listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE)

    fun isSelected(vehicle: VehicleType) = vehicle in selectable

    fun requiredDocuments(vehicle: VehicleType): List<String> =
        listOf("dniFront", "dniBack", "bankStatement") +
            if (vehicle == VehicleType.MOTORCYCLE) listOf("driverLicense", "soat") else emptyList()
}

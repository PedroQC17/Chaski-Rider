package com.example.chaskirider.feature.vehicle.domain

enum class VehicleType {
    BICYCLE,
    MOTORCYCLE,
    CAR, // Solo compatibilidad al leer perfiles antiguos; no se permite seleccionarlo ni guardarlo.
    NONE;

    companion object {
        fun fromId(id: String?): VehicleType = entries.firstOrNull { it.name == id } ?: NONE
    }
}

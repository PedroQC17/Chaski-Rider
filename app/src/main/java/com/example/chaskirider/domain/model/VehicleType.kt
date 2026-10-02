package com.example.chaskirider.domain.model

enum class VehicleType {
    BICYCLE,
    MOTORCYCLE,
    CAR, // Solo compatibilidad al leer perfiles antiguos; no se permite seleccionarlo ni guardarlo.
    NONE
}

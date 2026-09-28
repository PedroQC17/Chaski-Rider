package com.example.chaskirider.domain.model

enum class RegistrationStatus {
    INCOMPLETE,       // Faltan datos o documentos
    PENDING_REVIEW,   // Enviado y pendiente de revisión administrativa
    APPROVED,         // Autorizado para operar
    NEEDS_CORRECTION  // Requiere cambios por parte del usuario
}

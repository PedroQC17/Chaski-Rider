package com.example.chaskirider.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R

fun registrationDocumentLabel(type: String): Int = when (type) {
    "dniFront" -> R.string.text_dni_frente
    "dniBack" -> R.string.text_dni_reverso
    "driverLicense" -> R.string.text_licencia_de_conducir
    "soat" -> R.string.text_soat
    else -> R.string.text_estado_de_cuenta
}

@Composable
fun RegistrationCorrectionNotice(reason: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.registration_review_instructions), fontWeight = FontWeight.SemiBold)
            Text(reason.ifBlank { stringResource(R.string.registration_reason_unavailable) })
        }
    }
}

package com.example.chaskirider.feature.documents.ui.screens

import com.example.chaskirider.feature.vehicle.domain.VehiclePolicy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.user.ui.components.ChevronRightIcon
import com.example.chaskirider.feature.user.ui.components.*
import com.example.chaskirider.ui.theme.*

@Composable
fun ProfileDocumentsScreen(vehicleType: VehicleType, paths: Map<String, String>, onNavigateBack: () -> Unit,
    onDocumentView: (String) -> Unit, onRequestChange: () -> Unit) {
    val labels = mapOf("dniFront" to R.string.text_dni_frente, "dniBack" to R.string.text_dni_reverso,
        "driverLicense" to R.string.text_licencia_de_conducir, "soat" to R.string.text_soat, "bankStatement" to R.string.text_estado_de_cuenta)
    ProfileDetailsLayout(stringResource(R.string.text_documentos), onNavigateBack, onRequestChange) {
        VehiclePolicy.requiredDocuments(vehicleType).forEach { type ->
            val exists = !paths[type].isNullOrBlank()
            Surface(onClick = { onDocumentView(type) }, enabled = exists, shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderLight)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(labels.getValue(type)), Modifier.weight(1f))
                    if (exists) ChevronRightIcon(color = Orange) else Text(stringResource(R.string.text_pendiente), color = TextMuted)
                }
            }
        }
    }
}

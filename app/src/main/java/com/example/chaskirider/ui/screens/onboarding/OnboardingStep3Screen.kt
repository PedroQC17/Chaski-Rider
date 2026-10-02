package com.example.chaskirider.ui.screens.onboarding

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.components.OnboardingHeader
import com.example.chaskirider.ui.theme.*

@Preview(name = "Paso 3: Documentación y Banco", showBackground = true, showSystemUi = true)
@Composable
fun OnboardingStep3ScreenPreview() {
    ChaskiRiderTheme {
        OnboardingStep3Screen(
            vehicleType = VehicleType.MOTORCYCLE,
            initialBankInfo = BankInfo(
                bankName = "BCP",
                holderName = "Ana García",
                accountNumber = "191-12345678-0-12",
                cci = "00219100123456780123"
            ),
            documentsMap = mapOf(
                "dniFront" to DocumentFile("dniFront", uploadState = DocumentUploadState.UPLOADED),
                "dniBack" to DocumentFile("dniBack", uploadState = DocumentUploadState.UPLOADED)
            )
        )
    }
}

@Composable
fun OnboardingStep3Screen(
    vehicleType: VehicleType,
    initialBankInfo: BankInfo = BankInfo(),
    onNavigateBack: () -> Unit = {},
    onDocumentPick: (String, Uri) -> Unit = { _, _ -> },
    onDocumentView: (String) -> Unit = {},
    onFinishRegistrationClick: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    documentsMap: Map<String, DocumentFile> = emptyMap(),
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var bank by rememberSaveable { mutableStateOf(initialBankInfo.bankName) }
    var holder by rememberSaveable { mutableStateOf(initialBankInfo.holderName) }
    var account by rememberSaveable { mutableStateOf(initialBankInfo.accountNumber) }
    var cci by rememberSaveable { mutableStateOf(initialBankInfo.cci) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val type = selectedType
        if (uri != null && type != null) onDocumentPick(type, uri)
    }
    val required = RegistrationValidation.requiredDocuments(vehicleType)
    val labels = mapOf("dniFront" to stringResource(R.string.text_dni_frente), "dniBack" to stringResource(R.string.text_dni_reverso),
        "driverLicense" to stringResource(R.string.text_licencia_de_conducir), "soat" to stringResource(R.string.text_soat), "bankStatement" to stringResource(R.string.text_estado_de_cuenta))
    Column(Modifier.fillMaxSize().background(BackgroundLight).systemBarsPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
        OnboardingHeader(3, onNavigateBack = { if (!isLoading) onNavigateBack() })
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.text_documentacion), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.text_sube_archivos_pdf_jpg_o_png_de), color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        required.filter { it != "bankStatement" }.forEach { type ->
            DocumentRow(labels.getValue(type), documentsMap[type], !isLoading,
                onPick = { selectedType = type; picker.launch(RegistrationValidation.mimeTypes) },
                onView = { onDocumentView(type) })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.text_informacion_bancaria), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(stringResource(R.string.text_la_cuenta_donde_recibiras_tus_pagos), fontSize = 13.sp, color = TextMuted)
        BankSelector(bank, !isLoading) { bank = it }
        OutlinedTextField(holder, { holder = it }, label = { Text(stringResource(R.string.text_titular_de_la_cuenta)) }, enabled = !isLoading,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(account, { account = it }, label = { Text(stringResource(R.string.text_numero_de_cuenta)) }, enabled = !isLoading,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(cci, { cci = it.filter { c -> c in '0'..'9' }.take(20) },
            label = { Text(stringResource(R.string.registration_cci)) }, enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        DocumentRow(stringResource(R.string.text_estado_de_cuenta), documentsMap["bankStatement"], !isLoading,
            onPick = { selectedType = "bankStatement"; picker.launch(RegistrationValidation.mimeTypes) },
            onView = { onDocumentView("bankStatement") })
        Spacer(Modifier.height(20.dp))
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onFinishRegistrationClick(bank.trim(),holder.trim(),account.trim(),cci.trim()) },
            enabled = !isLoading && required.all { documentsMap[it]?.uploadState == DocumentUploadState.UPLOADED } &&
                RegistrationValidation.bankError(BankInfo(bank,holder,account,cci)) == null,
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
            if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(stringResource(R.string.text_finalizar_registro))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DocumentRow(title: String, document: DocumentFile?, enabled: Boolean, onPick: () -> Unit, onView: () -> Unit) {
    val state = document?.uploadState ?: DocumentUploadState.NOT_SELECTED
    Surface(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state == DocumentUploadState.UPLOADED) Icons.Default.CheckCircle else Icons.Default.Info,
                    null, tint = if (state == DocumentUploadState.UPLOADED) Color(0xFF278653) else Orange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, Modifier.weight(1f), color = TextDark, fontWeight = FontWeight.Medium)
                Text(when (state) {
                    DocumentUploadState.UPLOADED -> stringResource(R.string.text_subido)
                    DocumentUploadState.UPLOADING -> stringResource(R.string.text_subiendo)
                    DocumentUploadState.ERROR -> stringResource(R.string.text_error)
                    DocumentUploadState.SELECTED -> stringResource(R.string.selected)
                    else -> stringResource(R.string.text_pendiente)
                }, fontSize = 12.sp, color = TextMuted)
            }
            document?.errorMessage?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
            Row {
                TextButton(onClick = onPick, enabled = enabled) {
                    Text(if (state == DocumentUploadState.UPLOADED) stringResource(R.string.replace_document) else if (state == DocumentUploadState.ERROR) stringResource(R.string.retry) else stringResource(R.string.attach_document))
                }
                if (state == DocumentUploadState.UPLOADED) TextButton(onClick = onView, enabled = enabled) { Text(stringResource(R.string.text_ver_archivo)) }
            }
        }
    }
}

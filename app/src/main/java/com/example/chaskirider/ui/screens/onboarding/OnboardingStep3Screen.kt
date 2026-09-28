package com.example.chaskirider.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.components.OnboardingHeader
import com.example.chaskirider.ui.theme.*

@Composable
fun OnboardingStep3Screen(
    vehicleType: VehicleType,
    initialBankInfo: BankInfo = BankInfo(),
    onNavigateBack: () -> Unit = {},
    onDocumentPick: (String, Uri) -> Unit = { _, _ -> },
    onDocumentView: (String) -> Unit = {},
    onSaveBank: (BankInfo) -> Unit = {},
    onFinishRegistrationClick: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    documentsMap: Map<String, DocumentFile> = emptyMap(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null
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
    val labels = mapOf("dniFront" to "DNI · Frente", "dniBack" to "DNI · Reverso",
        "driverLicense" to "Licencia de conducir", "soat" to "SOAT", "bankStatement" to "Estado de cuenta")
    Column(Modifier.fillMaxSize().background(BackgroundLight).systemBarsPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
        OnboardingHeader(3, onNavigateBack = { if (!isLoading) onNavigateBack() })
        Spacer(Modifier.height(24.dp))
        Text("Documentación", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text("Sube archivos PDF, JPG o PNG de hasta 10 MB. Tus documentos se guardan de forma privada.", color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        required.filter { it != "bankStatement" }.forEach { type ->
            DocumentRow(labels.getValue(type), documentsMap[type], !isLoading,
                onPick = { selectedType = type; picker.launch(RegistrationValidation.mimeTypes) },
                onView = { onDocumentView(type) })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Información bancaria", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("La cuenta donde recibirás tus pagos.", fontSize = 13.sp, color = TextMuted)
        OutlinedTextField(bank, { bank = it }, label = { Text("Banco") }, enabled = !isLoading,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(holder, { holder = it }, label = { Text("Titular de la cuenta") }, enabled = !isLoading,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(account, { account = it }, label = { Text("Número de cuenta") }, enabled = !isLoading,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(cci, { cci = it.filter { c -> c in '0'..'9' }.take(20) },
            label = { Text("CCI (20 dígitos, opcional si ingresas cuenta)") }, enabled = !isLoading,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { onSaveBank(BankInfo(bank, holder, account, cci)) }, enabled = !isLoading) {
            Text("Guardar datos bancarios")
        }
        DocumentRow("Estado de cuenta", documentsMap["bankStatement"], !isLoading,
            onPick = { selectedType = "bankStatement"; picker.launch(RegistrationValidation.mimeTypes) },
            onView = { onDocumentView("bankStatement") })
        Spacer(Modifier.height(20.dp))
        Text("Revisaremos tu información. Podrás consultar el resultado en la app.",
            Modifier.fillMaxWidth().background(Color(0xFFFFF7F2), RoundedCornerShape(12.dp)).padding(16.dp),
            color = TextMuted, fontSize = 13.sp)
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
        successMessage?.let { Text(it, color = TextDark) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onFinishRegistrationClick(bank.trim(),holder.trim(),account.trim(),cci.trim()) },
            enabled = !isLoading && required.all { documentsMap[it]?.uploadState == DocumentUploadState.UPLOADED } &&
                RegistrationValidation.bankError(BankInfo(bank,holder,account,cci)) == null,
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
            if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Finalizar registro")
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
                    DocumentUploadState.UPLOADED -> "Subido"
                    DocumentUploadState.UPLOADING -> "Subiendo…"
                    DocumentUploadState.ERROR -> "Error"
                    DocumentUploadState.SELECTED -> "Seleccionado"
                    else -> "Pendiente"
                }, fontSize = 12.sp, color = TextMuted)
            }
            document?.errorMessage?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
            Row {
                TextButton(onClick = onPick, enabled = enabled) {
                    Text(if (state == DocumentUploadState.UPLOADED) "Reemplazar" else if (state == DocumentUploadState.ERROR) "Reintentar" else "Adjuntar")
                }
                if (state == DocumentUploadState.UPLOADED) TextButton(onClick = onView, enabled = enabled) { Text("Ver archivo") }
            }
        }
    }
}

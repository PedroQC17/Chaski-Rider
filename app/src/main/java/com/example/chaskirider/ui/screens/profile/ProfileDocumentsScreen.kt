// HU03 - Parte 4: pantalla "Documentos" accesible desde Mi perfil.
// - Lista los documentos requeridos según el vehículo (requiredDocuments).
// - Estado por documento mapeado desde RegistrationStatus (Firestore no guarda
//   estado por documento): sin URL o PENDING_REVIEW/INCOMPLETE -> Pendiente;
//   APPROVED -> Aprobado; NEEDS_CORRECTION -> Observado.
// - Un documento Observado (o faltante) puede reemplazarse: "Tomar foto" o
//   "Elegir archivo". La cámara pide el permiso CAMERA en runtime solo al
//   tocar "Tomar foto" (HU03 criterio de permisos).
// - La foto se previsualiza antes de enviarse: "Usar fotografía" / "Tomar otra
//   foto". El envío final usa AuthViewModel.uploadDocument (acción "document").
// - Colores: DangerRed/SuccessGreen/WarningAmber ahora provienen del tema
//   (Color.kt) en lugar de valores privados locales.
package com.example.chaskirider.ui.screens.profile

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.DocumentFile
import com.example.chaskirider.domain.model.DocumentUploadState
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RegistrationValidation
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType
import com.example.chaskirider.ui.components.ChevronRightIcon
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.DangerRed
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted
import com.example.chaskirider.ui.theme.WarningAmber
import java.io.File

private enum class DocState { PENDING, APPROVED, OBSERVED }

private val docLabels = mapOf(
    "dniFront" to "DNI · Frente",
    "dniBack" to "DNI · Reverso",
    "driverLicense" to "Licencia de conducir",
    "soat" to "SOAT",
    "bankStatement" to "Estado de cuenta"
)

@Preview(name = "Perfil - Documentos", showBackground = true, showSystemUi = true)
@Composable
fun ProfileDocumentsScreenPreview() {
    ChaskiRiderTheme {
        ProfileDocumentsScreen(
            user = RiderUser(
                id = "123",
                name = "Pedro",
                lastName = "Quincho Cordova",
                vehicleType = VehicleType.MOTORCYCLE,
                status = RegistrationStatus.PENDING_REVIEW,
                dniFrontUrl = "riders/123/documents/dniFront/aaa",
                dniBackUrl = "riders/123/documents/dniBack/bbb",
                bankStatementUrl = "riders/123/documents/bankStatement/ccc",
                driverLicenseUrl = "riders/123/documents/driverLicense/ddd",
                soatUrl = ""
            )
        )
    }
}

@Composable
fun ProfileDocumentsScreen(
    user: RiderUser,
    onNavigateBack: () -> Unit = {},
    onDocumentPick: (docType: String, uri: Uri) -> Unit = { _, _ -> },
    onDocumentView: (docType: String) -> Unit = {},
    documentsMap: Map<String, DocumentFile> = emptyMap(),
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    var actionsDoc by remember { mutableStateOf<String?>(null) }
    var cameraDoc by remember { mutableStateOf<String?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var previewDoc by remember { mutableStateOf<String?>(null) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && captureUri != null) {
            previewDoc = cameraDoc
            previewUri = captureUri
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val type = cameraDoc
        if (uri != null && type != null) onDocumentPick(type, uri)
        cameraDoc = null
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            permissionMessage = null
            cameraDoc?.let { doc ->
                val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                captureUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            permissionMessage = "Permiso de cámara denegado. Puedes elegir un archivo en su lugar."
        }
    }

    val paths = RegistrationValidation.documentPaths(user)

    val preview = previewUri
    if (preview != null) {
        val doc = previewDoc
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Previsualización",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = docLabels[doc.orEmpty()] ?: "",
                fontSize = 14.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = preview,
                contentDescription = "Foto del documento",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Verifica que el documento se vea claro y completo antes de enviarlo.",
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (doc != null) onDocumentPick(doc, preview)
                    previewUri = null
                    previewDoc = null
                },
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Usar fotografía", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    cameraDoc = doc
                    previewUri = null
                    cameraPermission.launch(Manifest.permission.CAMERA)
                },
                enabled = !isLoading,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Tomar otra foto", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextDark
                )
            }
            Text(
                text = "Documentos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cada documento muestra su estado de revisión. Los documentos observados pueden reemplazarse.",
            fontSize = 13.sp,
            color = TextMuted,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        RegistrationValidation.requiredDocuments(user.vehicleType).forEach { doc ->
            val path = paths[doc].orEmpty()
            val uploading = documentsMap[doc]?.uploadState == DocumentUploadState.UPLOADING
            DocumentOptionRow(
                label = docLabels[doc] ?: doc,
                state = documentState(user, path),
                uploading = uploading,
                onClick = {
                    if (!isLoading && !uploading) {
                        permissionMessage = null
                        actionsDoc = doc
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        permissionMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    actionsDoc?.let { doc ->
        val path = paths[doc].orEmpty()
        val state = documentState(user, path)
        val canReplace = path.isBlank() || state == DocState.OBSERVED
        AlertDialog(
            onDismissRequest = { actionsDoc = null },
            title = { Text(docLabels[doc] ?: doc) },
            text = {
                Column {
                    DocumentStateChip(state = state, uploading = false)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stateDescription(user, path, state),
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                    if (canReplace) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            cameraDoc = doc
                            actionsDoc = null
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }) { Text("Tomar foto") }
                        TextButton(onClick = {
                            cameraDoc = doc
                            actionsDoc = null
                            picker.launch(RegistrationValidation.mimeTypes)
                        }) { Text("Elegir archivo") }
                    }
                    if (path.isNotBlank()) {
                        TextButton(onClick = {
                            actionsDoc = null
                            onDocumentView(doc)
                        }) { Text("Ver documento") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionsDoc = null }) { Text("Cerrar") }
            }
        )
    }
}

private fun documentState(user: RiderUser, path: String): DocState = when {
    path.isBlank() -> DocState.PENDING
    user.status == RegistrationStatus.APPROVED -> DocState.APPROVED
    user.status == RegistrationStatus.NEEDS_CORRECTION -> DocState.OBSERVED
    else -> DocState.PENDING
}

private fun stateDescription(user: RiderUser, path: String, state: DocState): String = when {
    state == DocState.OBSERVED -> "El revisor observó este documento. Reemplázalo para volver a enviarlo."
    path.isBlank() -> "Aún no lo has subido."
    state == DocState.APPROVED -> "Documento aprobado por el revisor."
    user.status == RegistrationStatus.PENDING_REVIEW -> "En revisión. Te avisaremos cuando sea aprobado."
    else -> "Listo para enviarse con tu registro."
}

@Composable
private fun DocumentOptionRow(
    label: String,
    state: DocState,
    uploading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable(enabled = !uploading) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu_document),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
        DocumentStateChip(state = state, uploading = uploading)
        Spacer(modifier = Modifier.width(8.dp))
        ChevronRightIcon(color = TextMuted)
    }
}

@Composable
private fun DocumentStateChip(state: DocState, uploading: Boolean) {
    val label: String
    val color: Color
    when {
        uploading -> {
            label = "Subiendo…"
            color = Orange
        }
        state == DocState.APPROVED -> {
            label = "Aprobado"
            color = SuccessGreen
        }
        state == DocState.OBSERVED -> {
            label = "Observado"
            color = DangerRed
        }
        else -> {
            label = "Pendiente"
            color = WarningAmber
        }
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

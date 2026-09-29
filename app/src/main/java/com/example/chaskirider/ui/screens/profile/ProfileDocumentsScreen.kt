
package com.example.chaskirider.ui.screens.profile

import androidx.compose.ui.res.stringResource
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private enum class DocState { PENDING, APPROVED, OBSERVED }

private val docLabelResources = mapOf(
    "dniFront" to R.string.text_dni_frente,
    "dniBack" to R.string.text_dni_reverso,
    "driverLicense" to R.string.text_licencia_de_conducir,
    "soat" to R.string.text_soat,
    "bankStatement" to R.string.text_estado_de_cuenta
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
    onPrepareCapture: ((Uri) -> Unit) -> Unit = {},
    documentsMap: Map<String, DocumentFile> = emptyMap(),
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val docLabels = docLabelResources.mapValues { (_, resource) -> stringResource(resource) }
    val text_permiso_de_camara_denegado_puedes_elegir_un = stringResource(R.string.text_permiso_de_camara_denegado_puedes_elegir_un)
    val text_previsualizacion = stringResource(R.string.text_previsualizacion)
    val text_foto_del_documento = stringResource(R.string.text_foto_del_documento)
    val text_verifica_que_el_documento_se_vea_claro = stringResource(R.string.text_verifica_que_el_documento_se_vea_claro)
    val text_usar_fotografia = stringResource(R.string.text_usar_fotografia)
    val text_tomar_otra_foto = stringResource(R.string.text_tomar_otra_foto)
    val text_volver = stringResource(R.string.text_volver)
    val text_documentos = stringResource(R.string.text_documentos)
    val text_cada_documento_muestra_su_estado_de_revision = stringResource(R.string.text_cada_documento_muestra_su_estado_de_revision)
    val text_tomar_foto = stringResource(R.string.text_tomar_foto)
    val text_elegir_archivo = stringResource(R.string.text_elegir_archivo)
    val text_ver_documento = stringResource(R.string.text_ver_documento)
    val text_cerrar = stringResource(R.string.text_cerrar)

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
            cameraDoc?.let {
                onPrepareCapture { uri ->
                    captureUri = uri
                    cameraLauncher.launch(uri)
                }
            }
        } else {
            permissionMessage = text_permiso_de_camara_denegado_puedes_elegir_un
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
                text = text_previsualizacion,
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
                contentDescription = text_foto_del_documento,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text_verifica_que_el_documento_se_vea_claro,
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
                Text(text_usar_fotografia, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                Text(text_tomar_otra_foto, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
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
                    contentDescription = text_volver,
                    tint = TextDark
                )
            }
            Text(
                text = text_documentos,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text_cada_documento_muestra_su_estado_de_revision,
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
                        }) { Text(text_tomar_foto) }
                        TextButton(onClick = {
                            cameraDoc = doc
                            actionsDoc = null
                            picker.launch(RegistrationValidation.mimeTypes)
                        }) { Text(text_elegir_archivo) }
                    }
                    if (path.isNotBlank()) {
                        TextButton(onClick = {
                            actionsDoc = null
                            onDocumentView(doc)
                        }) { Text(text_ver_documento) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionsDoc = null }) { Text(text_cerrar) }
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

@Composable
private fun stateDescription(user: RiderUser, path: String, state: DocState): String {
    val text_el_revisor_observo_este_documento_reemplazalo_para = stringResource(R.string.text_el_revisor_observo_este_documento_reemplazalo_para)
    val text_aun_no_lo_has_subido = stringResource(R.string.text_aun_no_lo_has_subido)
    val text_documento_aprobado_por_el_revisor = stringResource(R.string.text_documento_aprobado_por_el_revisor)
    val text_en_revision_te_avisaremos_cuando_sea_aprobado = stringResource(R.string.text_en_revision_te_avisaremos_cuando_sea_aprobado)
    val text_listo_para_enviarse_con_tu_registro = stringResource(R.string.text_listo_para_enviarse_con_tu_registro)

    return when {
    state == DocState.OBSERVED -> text_el_revisor_observo_este_documento_reemplazalo_para
    path.isBlank() -> text_aun_no_lo_has_subido
    state == DocState.APPROVED -> text_documento_aprobado_por_el_revisor
    user.status == RegistrationStatus.PENDING_REVIEW -> text_en_revision_te_avisaremos_cuando_sea_aprobado
    else -> text_listo_para_enviarse_con_tu_registro
}
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
    val text_subiendo = stringResource(R.string.text_subiendo)
    val text_aprobado = stringResource(R.string.text_aprobado)
    val text_observado = stringResource(R.string.text_observado)
    val text_pendiente = stringResource(R.string.text_pendiente)

    val label: String
    val color: Color
    when {
        uploading -> {
            label = text_subiendo
            color = Orange
        }
        state == DocState.APPROVED -> {
            label = text_aprobado
            color = SuccessGreen
        }
        state == DocState.OBSERVED -> {
            label = text_observado
            color = DangerRed
        }
        else -> {
            label = text_pendiente
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

package com.example.chaskirider.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.ui.theme.*

@Composable
fun RegistrationStatusScreen(
    user: RiderUser,
    onResumeRegistrationClick: () -> Unit = {},
    onGoToHomeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.app_name), color = Orange,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            if (user.status == RegistrationStatus.PENDING_REVIEW) {
                Image(painterResource(R.drawable.registration_review), null,
                    Modifier.fillMaxWidth().heightIn(max = 290.dp).aspectRatio(1f), contentScale = ContentScale.Fit)
                Surface(color = Color(0xFFFFF0E8), shape = RoundedCornerShape(50)) {
                    Text(stringResource(R.string.registration_review_badge), color = Orange,
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                Spacer(Modifier.height(20.dp))
                StatusMessage(stringResource(R.string.registration_received_title), stringResource(R.string.registration_received_body))
            } else {
                Spacer(Modifier.height(48.dp))
                Icon(when {
                    !user.isEnabled -> Icons.Default.Lock
                    user.status == RegistrationStatus.APPROVED -> Icons.Default.CheckCircle
                    user.status == RegistrationStatus.INCOMPLETE -> Icons.Default.Lock
                    else -> Icons.Default.Info
                }, null, Modifier.size(80.dp), tint = Orange)
                Spacer(Modifier.height(24.dp))
                when {
                    !user.isEnabled -> StatusMessage(stringResource(R.string.registration_disabled_title), stringResource(R.string.registration_disabled_body))
                    user.status == RegistrationStatus.APPROVED -> StatusMessage(stringResource(R.string.text_cuenta_aprobada), stringResource(R.string.text_felicidades_tu_perfil_ha_sido_verificado_ya))
                    user.status == RegistrationStatus.NEEDS_CORRECTION -> StatusMessage(stringResource(R.string.text_tu_registro_requiere_correcciones),
                        if (user.rejectionReason.isNotBlank()) user.rejectionReason else stringResource(R.string.text_un_revisor_ha_detectado_inconsistencias_en_tus))
                    else -> StatusMessage(stringResource(R.string.text_registro_incompleto), stringResource(R.string.text_aun_tienes_datos_o_documentos_pendientes_por))
                }
            }
            Spacer(Modifier.height(32.dp))
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = {
                when {
                    !user.isEnabled -> onRefresh()
                    user.status == RegistrationStatus.APPROVED -> onGoToHomeClick()
                    user.status == RegistrationStatus.NEEDS_CORRECTION || user.status == RegistrationStatus.INCOMPLETE -> onResumeRegistrationClick()
                    else -> onRefresh()
                }
            }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(when {
                    !user.isEnabled -> R.string.text_actualizar_estado
                    user.status == RegistrationStatus.APPROVED -> R.string.text_ir_al_panel_principal
                    user.status == RegistrationStatus.NEEDS_CORRECTION -> R.string.text_corregir_informacion
                    user.status == RegistrationStatus.INCOMPLETE -> R.string.text_continuar_registro
                    else -> R.string.text_actualizar_estado
                }))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onLogoutClick, enabled = !isLoading) {
                Text(stringResource(R.string.text_cerrar_sesion), color = TextMuted)
            }
        }
    }
}

@Composable
private fun StatusMessage(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        color = TextDark, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Text(description, fontSize = 15.sp, lineHeight = 23.sp, color = TextMuted, textAlign = TextAlign.Center)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistrationStatusPendingPreview() {
    ChaskiRiderTheme { RegistrationStatusScreen(RiderUser(status = RegistrationStatus.PENDING_REVIEW)) }
}

@Preview(showBackground = true)
@Composable
private fun RegistrationStatusApprovedPreview() {
    ChaskiRiderTheme { RegistrationStatusScreen(RiderUser(status = RegistrationStatus.APPROVED)) }
}

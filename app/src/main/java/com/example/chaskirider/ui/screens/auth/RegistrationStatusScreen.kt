package com.example.chaskirider.ui.screens.auth

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.components.registrationDocumentLabel
import com.example.chaskirider.ui.theme.*

@Composable
fun RegistrationStatusScreen(
    state: RegistrationReviewUiState,
    onResumeRegistrationClick: () -> Unit = {},
    onGoToHomeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val stage = state.stage
    val user = state.user ?: return
    val title = when (stage) {
        RegistrationReviewStage.INCOMPLETE -> R.string.text_registro_incompleto
        RegistrationReviewStage.PENDING -> R.string.registration_received_title
        RegistrationReviewStage.CORRECTION -> R.string.text_tu_registro_requiere_correcciones
        RegistrationReviewStage.REJECTED -> R.string.registration_rejected_title
        RegistrationReviewStage.APPROVED -> R.string.text_cuenta_aprobada
        RegistrationReviewStage.DISABLED -> R.string.registration_disabled_title
    }
    val body = when (stage) {
        RegistrationReviewStage.INCOMPLETE -> R.string.registration_incomplete_explanation
        RegistrationReviewStage.PENDING -> R.string.registration_pending_explanation
        RegistrationReviewStage.CORRECTION -> R.string.registration_correction_explanation
        RegistrationReviewStage.REJECTED -> R.string.registration_rejected_body
        RegistrationReviewStage.APPROVED -> R.string.registration_approved_explanation
        RegistrationReviewStage.DISABLED -> R.string.registration_disabled_body
    }
    Surface(color = BackgroundLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.app_name), color = Orange,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Icon(when (stage) {
                RegistrationReviewStage.APPROVED -> Icons.Default.CheckCircle
                RegistrationReviewStage.REJECTED, RegistrationReviewStage.DISABLED -> Icons.Default.Lock
                else -> Icons.Default.Info
            }, null, Modifier.size(48.dp), tint = Orange)
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            if (stage in listOf(RegistrationReviewStage.CORRECTION, RegistrationReviewStage.REJECTED)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(if (stage == RegistrationReviewStage.CORRECTION)
                            R.string.registration_review_instructions else R.string.registration_decision_reason),
                            fontWeight = FontWeight.SemiBold)
                        Text(user.rejectionReason.ifBlank { stringResource(R.string.registration_reason_unavailable) })
                        if (stage == RegistrationReviewStage.CORRECTION) {
                            user.documentReviews.filter { (type, review) ->
                                type in RegistrationValidation.requiredDocuments(user.vehicleType) && review.status == DocumentReviewStatus.OBSERVED
                            }.forEach { (type, review) ->
                                HorizontalDivider()
                                Text(stringResource(registrationDocumentLabel(type)), fontWeight = FontWeight.SemiBold)
                                Text(review.reason)
                            }
                        }
                    }
                }
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.registration_refresh_failed), style = MaterialTheme.typography.bodySmall)
            }
            if (state.canEdit || state.canWork || stage == RegistrationReviewStage.PENDING) {
                Button(onClick = {
                    when {
                        state.canEdit -> onResumeRegistrationClick()
                        state.canWork -> onGoToHomeClick()
                        else -> onRefresh()
                    }
                }, enabled = !state.isRefreshing, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                    if (state.isRefreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(stringResource(when {
                        stage == RegistrationReviewStage.CORRECTION -> R.string.text_corregir_informacion
                        state.canEdit -> R.string.text_continuar_registro
                        state.canWork -> R.string.text_ir_al_panel_principal
                        else -> R.string.text_actualizar_estado
                    }))
                }
            }
            if (stage != RegistrationReviewStage.PENDING) TextButton(onClick = onRefresh,
                enabled = !state.isRefreshing, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (state.isRefreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.text_actualizar_estado))
            }
            TextButton(onClick = onLogoutClick, enabled = !state.isRefreshing,
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.text_cerrar_sesion), color = TextMuted)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistrationStatusPreview() {
    ChaskiRiderTheme { RegistrationStatusScreen(RegistrationReviewUiState(user = RiderUser(status = RegistrationStatus.PENDING_REVIEW))) }
}

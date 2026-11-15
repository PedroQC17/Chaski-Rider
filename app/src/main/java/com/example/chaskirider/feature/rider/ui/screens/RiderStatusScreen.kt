package com.example.chaskirider.feature.rider.ui.screens

import com.example.chaskirider.feature.rider.domain.VehiclePolicy
import com.example.chaskirider.feature.rider.domain.VehicleType

import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewStage
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewUiState
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
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderStatus
import com.example.chaskirider.feature.rider.domain.RiderOnboardingService
import com.example.chaskirider.feature.documents.domain.DocumentReviewStatus
import com.example.chaskirider.feature.rider.ui.components.registrationDocumentLabel
import com.example.chaskirider.ui.theme.*

@Composable
fun RiderStatusScreen(
    state: RiderOnboardingReviewUiState,
    onResumeRiderOnboardingClick: () -> Unit = {},
    onGoToHomeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    val stage = state.stage
    val user = state.user ?: return
    val title = when (stage) {
        RiderOnboardingReviewStage.INCOMPLETE -> R.string.text_registro_incompleto
        RiderOnboardingReviewStage.PENDING -> R.string.registration_pending_title
        RiderOnboardingReviewStage.CORRECTION -> R.string.text_tu_registro_requiere_correcciones
        RiderOnboardingReviewStage.REJECTED -> R.string.registration_rejected_title
        RiderOnboardingReviewStage.APPROVED -> R.string.text_cuenta_aprobada
        RiderOnboardingReviewStage.DISABLED -> R.string.registration_disabled_title
    }
    val body = when (stage) {
        RiderOnboardingReviewStage.INCOMPLETE -> R.string.registration_incomplete_explanation
        RiderOnboardingReviewStage.PENDING -> R.string.registration_pending_explanation
        RiderOnboardingReviewStage.CORRECTION -> R.string.registration_correction_explanation
        RiderOnboardingReviewStage.REJECTED -> R.string.registration_rejected_body
        RiderOnboardingReviewStage.APPROVED -> R.string.registration_approved_explanation
        RiderOnboardingReviewStage.DISABLED -> R.string.registration_disabled_body
    }
    Surface(color = BackgroundLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.app_name), color = Orange,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Icon(when (stage) {
                RiderOnboardingReviewStage.APPROVED -> Icons.Default.CheckCircle
                RiderOnboardingReviewStage.REJECTED, RiderOnboardingReviewStage.DISABLED -> Icons.Default.Lock
                else -> Icons.Default.Info
            }, null, Modifier.size(48.dp), tint = Orange)
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            if (stage in listOf(RiderOnboardingReviewStage.CORRECTION, RiderOnboardingReviewStage.REJECTED)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(if (stage == RiderOnboardingReviewStage.CORRECTION)
                            R.string.registration_review_instructions else R.string.registration_decision_reason),
                            fontWeight = FontWeight.SemiBold)
                        Text(user.rejectionReason.ifBlank { stringResource(R.string.registration_reason_unavailable) })
                        if (stage == RiderOnboardingReviewStage.CORRECTION) {
                            state.documentReviews.filter { (type, review) ->
                                type in VehiclePolicy.requiredDocuments(VehicleType.fromId(user.vehicleTypeId)) &&
                                    review.status == DocumentReviewStatus.OBSERVED
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
            if (state.canEdit || state.canWork || stage == RiderOnboardingReviewStage.PENDING) {
                Button(onClick = {
                    when {
                        state.canEdit -> onResumeRiderOnboardingClick()
                        state.canWork -> onGoToHomeClick()
                        else -> onRefresh()
                    }
                }, enabled = !state.isRefreshing, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                    if (state.isRefreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(stringResource(when {
                        stage == RiderOnboardingReviewStage.CORRECTION -> R.string.text_corregir_informacion
                        state.canEdit -> R.string.text_continuar_registro
                        state.canWork -> R.string.text_ir_al_panel_principal
                        else -> R.string.text_actualizar_estado
                    }))
                }
            }
            if (stage != RiderOnboardingReviewStage.PENDING && (stage != RiderOnboardingReviewStage.REJECTED || state.errorMessage != null)) TextButton(onClick = onRefresh,
                enabled = !state.isRefreshing, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (state.isRefreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.text_actualizar_estado))
            }
            TextButton(onClick = onSupportClick, enabled = !state.isRefreshing,
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.text_ayuda_y_soporte))
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
private fun RiderStatusPreview() {
    ChaskiRiderTheme { RiderStatusScreen(RiderOnboardingReviewUiState(user = Rider(status = RiderStatus.PENDING_REVIEW))) }
}

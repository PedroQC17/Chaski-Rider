package com.example.chaskirider.ui.screens.auth

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Estado de Registro - En Revisión", showBackground = true, showSystemUi = true)
@Composable
fun RegistrationStatusPendingPreview() {
    ChaskiRiderTheme {
        RegistrationStatusScreen(
            user = RiderUser(
                id = "123",
                name = "Ana",
                lastName = "García",
                email = "ana.garcia@example.com",
                status = RegistrationStatus.PENDING_REVIEW,
                vehicleType = VehicleType.MOTORCYCLE
            )
        )
    }
}

@Preview(name = "Estado de Registro - Aprobado", showBackground = true, showSystemUi = true)
@Composable
fun RegistrationStatusApprovedPreview() {
    ChaskiRiderTheme {
        RegistrationStatusScreen(
            user = RiderUser(
                id = "123",
                name = "Ana",
                lastName = "García",
                email = "ana.garcia@example.com",
                status = RegistrationStatus.APPROVED,
                isEnabled = true,
                vehicleType = VehicleType.MOTORCYCLE
            )
        )
    }
}

@Composable
fun RegistrationStatusScreen(
    user: RiderUser,
    onResumeRegistrationClick: () -> Unit = {},
    onGoToHomeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onConfigurePassword: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val status_incomplete = stringResource(R.string.status_incomplete)

    val text_pendiente = stringResource(R.string.text_pendiente)
    val text_registro_en_revision = stringResource(R.string.text_registro_en_revision)
    val text_recibimos_tu_informacion_revisaremos_tus_documentos_y = stringResource(R.string.text_recibimos_tu_informacion_revisaremos_tus_documentos_y)
    val text_observado = stringResource(R.string.text_observado)
    val text_tu_registro_requiere_correcciones = stringResource(R.string.text_tu_registro_requiere_correcciones)
    val text_un_revisor_ha_detectado_inconsistencias_en_tus = stringResource(R.string.text_un_revisor_ha_detectado_inconsistencias_en_tus)
    val text_corregir_informacion = stringResource(R.string.text_corregir_informacion)
    val text_aprobado = stringResource(R.string.text_aprobado)
    val text_cuenta_aprobada = stringResource(R.string.text_cuenta_aprobada)
    val text_felicidades_tu_perfil_ha_sido_verificado_ya = stringResource(R.string.text_felicidades_tu_perfil_ha_sido_verificado_ya)
    val text_ir_al_panel_principal = stringResource(R.string.text_ir_al_panel_principal)
    val text_registro_incompleto = stringResource(R.string.text_registro_incompleto)
    val text_aun_tienes_datos_o_documentos_pendientes_por = stringResource(R.string.text_aun_tienes_datos_o_documentos_pendientes_por)
    val text_continuar_registro = stringResource(R.string.text_continuar_registro)
    val text_actualizar_estado = stringResource(R.string.text_actualizar_estado)
    val text_configurar_contrasena = stringResource(R.string.text_configurar_contrasena)
    val text_cerrar_sesion = stringResource(R.string.text_cerrar_sesion)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (user.status) {
                RegistrationStatus.PENDING_REVIEW -> {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = text_pendiente,
                        tint = Orange,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = text_registro_en_revision,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = text_recibimos_tu_informacion_revisaremos_tus_documentos_y,
                        fontSize = 15.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                RegistrationStatus.NEEDS_CORRECTION -> {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = text_observado,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = text_tu_registro_requiere_correcciones,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (user.rejectionReason.isNotBlank()) {
                            stringResource(R.string.text_motivo_value, user.rejectionReason)
                        } else {
                            text_un_revisor_ha_detectado_inconsistencias_en_tus
                        },
                        fontSize = 15.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onResumeRegistrationClick,
                        enabled = !isLoading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = text_corregir_informacion,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                RegistrationStatus.APPROVED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = text_aprobado,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = text_cuenta_aprobada,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = text_felicidades_tu_perfil_ha_sido_verificado_ya,
                        fontSize = 15.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onGoToHomeClick,
                        enabled = user.isEnabled && !isLoading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = text_ir_al_panel_principal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                RegistrationStatus.INCOMPLETE -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = status_incomplete,
                        tint = Orange,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = text_registro_incompleto,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = text_aun_tienes_datos_o_documentos_pendientes_por,
                        fontSize = 15.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onResumeRegistrationClick,
                        enabled = !isLoading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = text_continuar_registro,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let { Text(it, color = Color.Red) }
        androidx.compose.material3.TextButton(onClick = onRefresh, enabled = !isLoading) { Text(text_actualizar_estado) }
        androidx.compose.material3.TextButton(onClick = onConfigurePassword, enabled = !isLoading) { Text(text_configurar_contrasena) }

        OutlinedButton(
            onClick = onLogoutClick,
            enabled = !isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = text_cerrar_sesion,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

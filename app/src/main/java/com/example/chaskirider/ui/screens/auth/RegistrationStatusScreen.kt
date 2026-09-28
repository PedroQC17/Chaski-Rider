package com.example.chaskirider.ui.screens.auth

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
                        contentDescription = "Pendiente",
                        tint = Orange,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Registro en revisión",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Recibimos tu información. Revisaremos tus documentos y podrás consultar el resultado en la app.",
                        fontSize = 15.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                RegistrationStatus.NEEDS_CORRECTION -> {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Observado",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Tu registro requiere correcciones",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (user.rejectionReason.isNotBlank()) {
                            "Motivo: ${user.rejectionReason}"
                        } else {
                            "Un revisor ha detectado inconsistencias en tus documentos. Por favor actualiza la información solicitada."
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
                            text = "Corregir información",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                RegistrationStatus.APPROVED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Aprobado",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¡Cuenta aprobada!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Felicidades, tu perfil ha sido verificado. Ya puedes conectarte y comenzar a recibir pedidos.",
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
                            text = "Ir al panel principal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                RegistrationStatus.INCOMPLETE -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Incompleto",
                        tint = Orange,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Registro incompleto",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aún tienes datos o documentos pendientes por completar.",
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
                            text = "Continuar registro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let { Text(it, color = Color.Red) }
        androidx.compose.material3.TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Actualizar estado") }
        androidx.compose.material3.TextButton(onClick = onConfigurePassword, enabled = !isLoading) { Text("Configurar contraseña") }
        // Botón Cerrar Sesión
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
                text = "Cerrar sesión",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

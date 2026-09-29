package com.example.chaskirider.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RiderUser
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

@Composable
fun ProfileHeaderCard(user: RiderUser) {
    val fullName = "${user.name} ${user.lastName}".trim().ifBlank { "Repartidor" }
    val initial = user.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Orange, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = fullName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Repartidor", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun AccountStatusCard(user: RiderUser) {
    val chipLabel: String
    val chipColor: Color
    val subtitle: String
    when (user.status) {
        RegistrationStatus.APPROVED -> {
            chipLabel = "Aprobado"
            chipColor = SuccessGreen
            subtitle = if (user.isEnabled) "¡Ya puedes recibir pedidos!" else "Cuenta aprobada. Espera a que esté habilitada."
        }
        RegistrationStatus.PENDING_REVIEW -> {
            chipLabel = "En revisión"
            chipColor = Orange
            subtitle = "Estamos revisando tu registro. Te avisaremos cuando esté listo."
        }
        RegistrationStatus.NEEDS_CORRECTION -> {
            chipLabel = "Observado"
            chipColor = DangerRed
            subtitle = if (user.rejectionReason.isNotBlank()) "Motivo: ${user.rejectionReason}"
            else "Un revisor encontró inconsistencias. Corrige tu información."
        }
        RegistrationStatus.INCOMPLETE -> {
            chipLabel = "Incompleto"
            chipColor = WarningAmber
            subtitle = "Completa tus datos y documentos para continuar."
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Estado de cuenta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            StatusChip(label = chipLabel, color = chipColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, fontSize = 14.sp, color = TextMuted, lineHeight = 20.sp)
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun ProfileMenuRow(
    icon: @Composable () -> Unit,
    label: String,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val enabled = onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                danger -> DangerRed
                !enabled -> TextMuted
                else -> TextDark
            },
            modifier = Modifier.weight(1f)
        )
        if (enabled) {
            ChevronRightIcon(color = TextMuted)
        } else {
            Text(text = "Próximamente", fontSize = 11.sp, color = TextMuted)
        }
    }
}

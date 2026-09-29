
package com.example.chaskirider.ui.screens.home

import com.example.chaskirider.ui.screens.home.components.AvailabilitySlider
import com.example.chaskirider.ui.screens.home.components.NotificationsBell
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.DangerRed
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Preview(name = "Home - Conectado", showBackground = true, showSystemUi = true)
@Composable
fun HomeConnectedPreview() {
    ChaskiRiderTheme {
        HomeScreen(
            user = RiderUser(
                id = "123",
                name = "Pedro",
                lastName = "Quincho Cordova",
                status = RegistrationStatus.APPROVED,
                isEnabled = true,
                isAvailable = true
            )
        )
    }
}

@Composable
fun HomeScreen(
    user: RiderUser,
    onAvailabilityChange: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onConfigurePassword: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    unreadCount: Int = 0
) {
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onAvailabilityChange(true)
        else onError("Se requiere el permiso de ubicación para activarte como disponible.")
    }
    
    val notificationsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    fun toggleAvailability() {
        if (user.isAvailable) { onAvailabilityChange(false); return }
        val enabledRider = user.status == RegistrationStatus.APPROVED && user.isEnabled
        if (!enabledRider) { onAvailabilityChange(true); return }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) onAvailabilityChange(true)
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val fullName = "${user.name} ${user.lastName}".trim().ifBlank { "Repartidor" }
    val firstName = fullName.substringBefore(" ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Hola, $firstName!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (user.isAvailable) SuccessGreen else BorderLight, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (user.isAvailable) "Estás conectado" else "Estás desconectado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (user.isAvailable) SuccessGreen else TextMuted
                    )
                }
            }
            NotificationsBell(count = unreadCount, onClick = onNotificationsClick)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Orange.copy(alpha = 0.19f), RoundedCornerShape(16.dp))
                .background(Orange.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Conéctate para recibir pedidos en tu zona",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Activa tu disponibilidad y empieza a ganar.",
                fontSize = 14.sp,
                color = TextMuted,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            AvailabilitySlider(
                available = user.isAvailable,
                enabled = !isLoading,
                onToggle = { toggleAvailability() }
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }


        Spacer(modifier = Modifier.height(48.dp))

        TextButton(onClick = onConfigurePassword) { Text("Configurar contraseña") }
        TextButton(onClick = onLogout) { Text("Cerrar sesión", color = DangerRed) }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

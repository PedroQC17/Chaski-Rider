
package com.example.chaskirider.ui.screens.profile

import com.example.chaskirider.ui.components.AppHeader
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.res.stringResource
import com.example.chaskirider.ui.screens.profile.components.*
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

@Preview(name = "Perfil - En revisión", showBackground = true, showSystemUi = true)
@Composable
fun ProfilePendingPreview() {
    ChaskiRiderTheme {
        ProfileScreen(
            user = RiderUser(
                id = "123",
                name = "Pedro",
                lastName = "Quincho Cordova",
                email = "pedroquincho9@gmail.com",
                status = RegistrationStatus.PENDING_REVIEW
            )
        )
    }
}

@Preview(name = "Perfil - Aprobado", showBackground = true, showSystemUi = true)
@Composable
fun ProfileApprovedPreview() {
    ChaskiRiderTheme {
        ProfileScreen(
            user = RiderUser(
                id = "123",
                name = "Pedro",
                lastName = "Quincho Cordova",
                email = "pedroquincho9@gmail.com",
                status = RegistrationStatus.APPROVED,
                isEnabled = true
            )
        )
    }
}

@Composable
fun ProfileScreen(
    user: RiderUser,
    onOpenMenu: () -> Unit = {},
    onPersonalDataClick: (() -> Unit)? = null,
    onVehicleClick: (() -> Unit)? = null,
    onDocumentsClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        AppHeader(title = stringResource(R.string.text_mi_perfil), onNavigate = onOpenMenu,
            navigationIcon = Icons.Default.Menu, navigationDescription = stringResource(R.string.home_open_menu))

        Spacer(modifier = Modifier.height(20.dp))

        ProfileHeaderCard(user = user)

        Spacer(modifier = Modifier.height(16.dp))

        AccountStatusCard(user = user)

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
        ) {
            ProfileMenuRow(
                icon = { Icon(Icons.Default.Person, null, tint = TextDark, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_datos_personales),
                onClick = onPersonalDataClick
            )
            ProfileMenuRow(
                icon = { Icon(painterResource(R.drawable.ic_menu_vehicle), null, tint = TextDark, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_vehiculos),
                onClick = onVehicleClick
            )
            ProfileMenuRow(
                icon = { Icon(painterResource(R.drawable.ic_menu_document), null, tint = TextDark, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_documentos),
                onClick = onDocumentsClick
            )
            HorizontalDivider(color = BorderLight)
            ProfileMenuRow(
                icon = { Icon(painterResource(R.drawable.ic_menu_payment), null, tint = TextMuted, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_metodos_de_pago)
            )
            ProfileMenuRow(
                icon = { Icon(Icons.Default.Notifications, null, tint = if (onNotificationsClick != null) TextDark else TextMuted, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_notificaciones),
                onClick = onNotificationsClick
            )
            ProfileMenuRow(
                icon = { Icon(Icons.Default.Info, null, tint = TextMuted, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_ayuda_y_soporte)
            )
            HorizontalDivider(color = BorderLight)
            ProfileMenuRow(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = DangerRed, modifier = Modifier.size(22.dp)) },
                label = stringResource(R.string.text_cerrar_sesion),
                danger = true,
                onClick = onLogoutClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


package com.example.chaskirider.ui.screens.notifications

import androidx.compose.material.icons.filled.Menu
import com.example.chaskirider.ui.components.AppHeader
import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.domain.model.ChaskiNotification
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Preview(name = "Notificaciones - Vacío", showBackground = true, showSystemUi = true)
@Composable
fun NotificationsEmptyPreview() {
    ChaskiRiderTheme { NotificationsScreen() }
}

@Preview(name = "Notificaciones - Lista", showBackground = true, showSystemUi = true)
@Composable
fun NotificationsListPreview() {
    ChaskiRiderTheme {
        NotificationsScreen(
            state = NotificationsUiState(notifications = listOf(
                ChaskiNotification(stringResource(R.string.text_nuevo_pedido_disponible), stringResource(R.string.text_recogida_en_miraflores_a_1_2_km)),
                ChaskiNotification(stringResource(R.string.text_registro_aprobado), stringResource(R.string.text_ya_puedes_empezar_a_recibir_pedidos))
            ))
        )
    }
}

@Composable
fun NotificationsScreen(
    onOpenMenu: () -> Unit = {},
    state: NotificationsUiState = NotificationsUiState()
) {
    val notifications = state.notifications

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .statusBarsPadding()
    ) {
        AppHeader(title = stringResource(R.string.text_notificaciones), onNavigate = onOpenMenu, navigationIcon = Icons.Default.Menu,
            navigationDescription = stringResource(R.string.home_open_menu),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

        if (notifications.isEmpty()) {
            EmptyNotifications()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                notifications.forEach { item ->
                    NotificationItem(item)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: ChaskiNotification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Orange.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = notification.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = notification.body, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = rememberDateFormat().format(Date(notification.timestamp)),
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun rememberDateFormat(): DateFormat =
    remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()) }

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(Orange.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.text_aun_no_tienes_notificaciones),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.text_cuando_llegue_un_pedido_o_haya_una),
            fontSize = 14.sp,
            color = TextMuted,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

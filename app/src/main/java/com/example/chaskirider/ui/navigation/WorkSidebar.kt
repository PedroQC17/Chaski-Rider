package com.example.chaskirider.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark

@Composable
fun WorkSidebar(
    user: RiderUser?,
    route: String?,
    unread: Int,
    busy: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp)) {
        // Cabecera: Logo a la izquierda, Texto "Chaski Rider" con diseño a la derecha
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = TextDark,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Chaski ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Orange,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Rider")
                    }
                },
                fontSize = 22.sp
            )
        }

        HorizontalDivider()

        Spacer(Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.home_map_menu)) },
            selected = route == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(Icons.Default.Home, null) }
        )

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.text_perfil)) },
            selected = route == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.Person, null) }
        )

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.text_notificaciones)) },
            selected = route == Screen.Notifications.route,
            onClick = { onNavigate(Screen.Notifications.route) },
            icon = { Icon(Icons.Default.Notifications, null) },
            badge = { if (unread > 0) Text(unread.toString()) }
        )

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = onLogout,
            enabled = !busy,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Orange
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.text_cerrar_sesion), color = Orange)
        }

        Spacer(Modifier.height(16.dp))
    }
}

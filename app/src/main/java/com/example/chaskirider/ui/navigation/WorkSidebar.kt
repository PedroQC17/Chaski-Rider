package com.example.chaskirider.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.RiderUser

@Composable
fun WorkSidebar(user: RiderUser?, route: String?, unread: Int, busy: Boolean,
    onNavigate: (String) -> Unit, onPassword: () -> Unit, onLogout: () -> Unit) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp)) {
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        NavigationDrawerItem(label = { Text(stringResource(R.string.home_map_menu)) }, selected = route == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) }, icon = { Icon(Icons.Default.Home, null) })
        NavigationDrawerItem(label = { Text(stringResource(R.string.text_perfil)) }, selected = route == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) }, icon = { Icon(Icons.Default.Person, null) })
        NavigationDrawerItem(label = { Text(stringResource(R.string.text_notificaciones)) }, selected = route == Screen.Notifications.route,
            onClick = { onNavigate(Screen.Notifications.route) }, icon = { Icon(Icons.Default.Notifications, null) },
            badge = { if (unread > 0) Text(unread.toString()) })
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onPassword, enabled = !busy, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(Icons.Default.Lock, null); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.text_configurar_contrasena))
        }
        TextButton(onClick = onLogout, enabled = !busy, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.text_cerrar_sesion))
        }
        Spacer(Modifier.height(16.dp))
    }
}

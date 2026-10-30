package com.example.chaskirider.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.*

@Composable
fun WorkSidebar(route: String?, unread: Int, busy: Boolean, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    val width = (LocalConfiguration.current.screenWidthDp * .76f).coerceAtMost(280f).dp
    ModalDrawerSheet(
        modifier = Modifier.width(width),
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        // Cabecera: Logo a la izquierda, Texto "Chaski Rider" a la derecha
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

        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = BorderLight)
        Spacer(Modifier.height(20.dp))

        val colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFFFFEEE5),
            selectedIconColor = Orange,
            selectedTextColor = Orange,
            unselectedContainerColor = Color.White,
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextDark
        )

        listOf(
            Triple(Screen.Home.route, R.string.home_map_menu, Icons.Default.Home),
            Triple(Screen.Orders.route, R.string.orders_real_menu, Icons.Default.ShoppingCart),
            Triple(Screen.Earnings.route, R.string.text_mis_ganancias, Icons.Default.Star),
            Triple(Screen.Profile.route, R.string.text_perfil, Icons.Default.Person),
            Triple(Screen.Notifications.route, R.string.text_notificaciones, Icons.Default.Notifications),
            Triple(Screen.Support.route, R.string.text_ayuda_y_soporte, Icons.Default.Info)
        ).forEach { (target, label, icon) ->
            NavigationDrawerItem(
                label = { Text(stringResource(label), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                selected = route == target || (target == Screen.Profile.route && route?.startsWith("profile_") == true),
                onClick = { onNavigate(target) },
                icon = { Icon(icon, null, Modifier.size(21.dp)) },
                badge = { if (target == Screen.Notifications.route && unread > 0) Badge(containerColor = Orange) { Text(unread.toString()) } },
                colors = colors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp).height(52.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = BorderLight)

        TextButton(
            onClick = onLogout,
            enabled = !busy,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.text_cerrar_sesion), color = Orange)
        }
    }
}

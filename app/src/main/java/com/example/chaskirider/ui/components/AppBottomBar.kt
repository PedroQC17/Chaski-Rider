
package com.example.chaskirider.ui.components

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.chaskirider.ui.navigation.Screen
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextMuted

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val text_inicio = stringResource(R.string.text_inicio)
    val text_perfil = stringResource(R.string.text_perfil)

    NavigationBar(containerColor = Color.White) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Orange,
            selectedTextColor = Orange,
            indicatorColor = Orange.copy(alpha = 0.10f),
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextMuted
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = text_inicio) },
            label = { Text(text_inicio, fontSize = 12.sp) },
            alwaysShowLabel = true,
            colors = colors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.Person, contentDescription = text_perfil) },
            label = { Text(text_perfil, fontSize = 12.sp) },
            alwaysShowLabel = true,
            colors = colors
        )
    }
}


package com.example.chaskirider.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio", fontSize = 12.sp) },
            alwaysShowLabel = true,
            colors = colors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Orders.route,
            onClick = { onNavigate(Screen.Orders.route) },
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Pedidos") },
            label = { Text("Pedidos", fontSize = 12.sp) },
            alwaysShowLabel = true,
            colors = colors
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            enabled = false,
            icon = { Icon(Icons.Default.Star, contentDescription = "Ganancias") },
            label = { Text("Ganancias", fontSize = 12.sp) },
            alwaysShowLabel = true
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", fontSize = 12.sp) },
            alwaysShowLabel = true,
            colors = colors
        )
    }
}

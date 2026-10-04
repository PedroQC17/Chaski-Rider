package com.example.chaskirider.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.screens.home.components.ConnectionPanel
import com.example.chaskirider.ui.screens.home.components.WorkMap
import com.example.chaskirider.ui.theme.ChaskiRiderTheme

@Composable
fun HomeScreen(state: HomeUiState, onOpenMenu: () -> Unit,
    onToggleAvailability: () -> Unit, onLocate: () -> Unit, onDismissError: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        WorkMap(state, 200.dp)
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onOpenMenu, modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Icon(Icons.Default.Menu, stringResource(R.string.home_open_menu))
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, shadowElevation = 3.dp) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(if (state.user?.isAvailable == true) Color(0xFF208554) else Color(0xFF757575), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (state.user?.isAvailable == true) R.string.text_estas_conectado else R.string.text_estas_desconectado),
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).widthIn(max = 480.dp),
            horizontalAlignment = Alignment.End) {
            FilledIconButton(onClick = onLocate, enabled = !state.isLocating && state.user?.isAvailable == true, modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                if (state.isLocating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.LocationOn, stringResource(R.string.home_locate))
            }
            if (state.errorMessage != null || state.locationUnavailable) {
                Spacer(Modifier.height(8.dp))
                Snackbar(action = { TextButton(onClick = onDismissError) { Text(stringResource(R.string.text_cerrar)) } }) {
                    Text(state.errorMessage ?: stringResource(R.string.home_location_unavailable))
                }
            }
            Spacer(Modifier.height(12.dp))
            ConnectionPanel(state, onToggleAvailability)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    ChaskiRiderTheme { HomeScreen(HomeUiState(), {}, {}, {}, {}) }
}

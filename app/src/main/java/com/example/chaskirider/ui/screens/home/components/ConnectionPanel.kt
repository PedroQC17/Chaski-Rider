package com.example.chaskirider.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.screens.home.HomeUiState

@Composable
fun ConnectionPanel(state: HomeUiState, onExpand: () -> Unit, onToggle: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp, tonalElevation = 2.dp) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (state.user?.isAvailable == true) R.string.home_ready else R.string.text_conectate_para_recibir_pedidos_en_tu_zona),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (state.panelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    stringResource(if (state.panelExpanded) R.string.home_collapse else R.string.home_expand))
            }
            AnimatedVisibility(state.panelExpanded) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                    if (state.isUpdatingAvailability) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }
                    AvailabilitySlider(available = state.user?.isAvailable == true,
                        enabled = !state.isUpdatingAvailability && state.user != null, onToggle = onToggle)
                }
            }
        }
    }
}

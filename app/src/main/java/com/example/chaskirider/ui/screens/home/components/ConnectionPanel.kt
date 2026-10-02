package com.example.chaskirider.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.screens.home.HomeUiState

@Composable
fun ConnectionPanel(state: HomeUiState, onToggle: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp, color = Color.White) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (state.user?.isAvailable == true) R.string.home_ready else R.string.text_conectate_para_recibir_pedidos_en_tu_zona),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (state.isUpdatingAvailability) LinearProgressIndicator(Modifier.fillMaxWidth())
            AvailabilitySlider(available = state.user?.isAvailable == true,
                enabled = !state.isUpdatingAvailability && state.user != null, onToggle = onToggle)
        }
    }
}

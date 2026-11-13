package com.example.chaskirider.feature.orders.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.orders.domain.OrderSyncState

@Composable
internal fun OrderSyncNotice(state: OrderSyncState, onRetry: () -> Unit) {
    if (state.online && state.pendingActions == 0) return
    Surface(color = if (state.needsAttention) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(when {
                !state.online -> "Sin internet. Los datos y avances del pedido se conservan en este dispositivo."
                state.needsAttention -> "No se pudieron confirmar algunos cambios. Siguen guardados; reintenta o contacta con soporte."
                state.syncing -> "Enviando cambios guardados…"
                else -> "Hay cambios guardados pendientes de enviar."
            }, style = MaterialTheme.typography.bodySmall)
            if (state.pendingActions > 0) {
                Text("${state.pendingActions} cambios por enviar.", style = MaterialTheme.typography.labelMedium)
                if (!state.online) Text("Se enviarán automáticamente al recuperar conexión.", style = MaterialTheme.typography.bodySmall)
                else if (!state.syncing) TextButton(onClick = onRetry) { Text("Reintentar envío") }
            }
        }
    }
}

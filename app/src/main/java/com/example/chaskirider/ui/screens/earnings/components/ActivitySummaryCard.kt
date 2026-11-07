package com.example.chaskirider.ui.screens.earnings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.domain.earnings.ActivitySummary
import java.util.Locale

@Composable
fun ActivitySummaryCard(activity: ActivitySummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tu actividad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Metric("Entregas completadas", activity.delivered.toString(), Modifier.weight(1f))
                Metric("Pedidos cancelados", activity.cancelled.toString(), Modifier.weight(1f))
            }
            HorizontalDivider()
            val duration = activity.averageDurationMillis?.let {
                String.format(Locale.forLanguageTag("es-PE"), "%.1f min", it / 60_000)
            } ?: if (activity.delivered == 0) "0 min" else "No disponible"
            Metric("Tiempo promedio", duration)
            Text("Desde la aceptación hasta la entrega.", style = MaterialTheme.typography.bodySmall)
            if (activity.timedDeliveries < activity.delivered) {
                Text("${activity.timedDeliveries} de ${activity.delivered} entregas tienen tiempos válidos. Las demás no entran en el promedio.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

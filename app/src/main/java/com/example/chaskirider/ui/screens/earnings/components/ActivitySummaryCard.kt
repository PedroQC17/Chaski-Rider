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
fun ActivitySummaryCard(activity: ActivitySummary, connection: com.example.chaskirider.domain.earnings.ConnectionSummary, connectionWarning: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tu actividad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Metric("Entregas exitosas", activity.delivered.toString(), Modifier.weight(1f))
                Metric("Pedidos cancelados", activity.cancelled.toString(), Modifier.weight(1f))
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Metric("Entregas no exitosas", activity.unsuccessful.toString(), Modifier.weight(1f))
                Metric("Pedidos completados", (activity.delivered + activity.unsuccessful).toString(), Modifier.weight(1f))
            }
            val deliveryMinutes = activity.averageDurationMillis?.let { String.format(Locale.forLanguageTag("es-PE"), "%.1f min", it / 60_000) } ?: "Sin entregas"
            Metric("Tiempo promedio de entrega", deliveryMinutes)
        }
    }
    Spacer(Modifier.height(16.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Conexión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            fun hours(millis: Long): String {
                val minutes = millis / 60_000
                return "${minutes / 60} h ${minutes % 60} min"
            }
            Metric("Tiempo conectado", hours(connection.totalMillis))
            Metric("Promedio diario de conexión", if (connection.days == 0) "Sin conexiones" else hours(connection.averageDailyMillis))
            if (connection.days > 0) Text("${connection.days} días con conexión", style = MaterialTheme.typography.labelMedium)
            if (connectionWarning == "No se pudo consultar el tiempo conectado.") Text("Conexión pendiente de actualizar", style = MaterialTheme.typography.bodySmall)
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

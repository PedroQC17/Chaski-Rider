package com.example.chaskirider.feature.earnings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.earnings.domain.ConnectionDay
import com.example.chaskirider.ui.theme.Orange
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData

@Composable
fun ConnectionDashboard(days: List<ConnectionDay>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tiempo conectado por día", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (days.isEmpty()) {
                Text("Sin conexiones registradas en este periodo", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Promedio diario de conexión: Sin conexiones",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.semantics { contentDescription = "Promedio diario de conexión: Sin conexiones" }
                )
            } else {
                val total = days.sumOf { it.millis }
                val average = total / days.size
                val bars = days.map { day ->
                    BarChartData.Bar(
                        value = day.millis / 3_600_000f,
                        color = Orange,
                        label = day.label.takeLast(5)
                    )
                }
                BarChart(
                    barChartData = BarChartData(bars),
                    modifier = Modifier.fillMaxWidth().height(150.dp).semantics {
                        contentDescription = days.joinToString("; ") {
                            "${it.label}: ${hours(it.millis)}"
                        }
                    }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConnectionMetric("Tiempo conectado", hours(total), Modifier.weight(1f))
                    ConnectionMetric("Promedio diario de conexión", hours(average), Modifier.weight(1f))
                }
            }
        }
    }
}

private fun hours(millis: Long): String {
    val minutes = millis / 60_000
    return "${minutes / 60} h ${minutes % 60} min"
}

@Composable
private fun ConnectionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

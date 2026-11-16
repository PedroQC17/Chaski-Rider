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
import com.example.chaskirider.feature.earnings.domain.ActivityBreakdown
import com.example.chaskirider.ui.theme.DangerRed
import com.example.chaskirider.ui.theme.SuccessGreen
import com.example.chaskirider.ui.theme.WarningAmber
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData

@Composable
fun ActivityDashboard(activity: ActivityBreakdown) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Actividad del periodo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            val total = activity.successful + activity.unsuccessful + activity.cancelled
            if (total == 0) {
                Text("Sin pedidos finalizados en este periodo.", style = MaterialTheme.typography.bodySmall)
            } else {
                val bars = listOf(
                    BarChartData.Bar(value = activity.successful.toFloat(), color = SuccessGreen, label = "Exitosas"),
                    BarChartData.Bar(value = activity.unsuccessful.toFloat(), color = WarningAmber, label = "No exitosas"),
                    BarChartData.Bar(value = activity.cancelled.toFloat(), color = DangerRed, label = "Canceladas")
                )
                BarChart(
                    barChartData = BarChartData(bars),
                    modifier = Modifier.fillMaxWidth().height(150.dp).semantics {
                        contentDescription = "Entregas exitosas: ${activity.successful}; " +
                            "Entregas no exitosas: ${activity.unsuccessful}; " +
                            "Pedidos cancelados: ${activity.cancelled}"
                    }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActivityMetric("Entregas exitosas", activity.successful, Modifier.weight(1f))
                ActivityMetric("Entregas no exitosas", activity.unsuccessful, Modifier.weight(1f))
                ActivityMetric("Pedidos cancelados", activity.cancelled, Modifier.weight(1f))
            }
            ActivityMetric("Pedidos completados", activity.successful + activity.unsuccessful, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ActivityMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

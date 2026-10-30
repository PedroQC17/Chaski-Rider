package com.example.chaskirider.ui.screens.earnings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.domain.earnings.DeliveryEarningsRecord
import com.example.chaskirider.ui.theme.Orange
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer

@Composable
fun EarningsChartCard(records: List<DeliveryEarningsRecord>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ingresos por Entregas",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (records.isEmpty()) {
                Text(
                    text = "Sin entregas registradas en este periodo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                val bars = records.take(6).map { record ->
                    BarChartData.Bar(
                        value = record.totalCents / 100f,
                        color = Orange,
                        label = record.merchantName.take(6)
                    )
                }

                BarChart(
                    barChartData = BarChartData(bars = bars),
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    labelDrawer = SimpleValueDrawer(drawLocation = SimpleValueDrawer.DrawLocation.XAxis)
                )
            }
        }
    }
}

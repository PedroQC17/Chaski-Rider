package com.example.chaskirider.feature.earnings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.earnings.domain.ActivityChartPoint
import com.example.chaskirider.ui.theme.Orange
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import java.util.Locale

@Composable
fun EarningsChartCard(points: List<ActivityChartPoint>) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (points.firstOrNull()?.monthly == true) "Ganancias por mes" else "Ganancias por día",
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (points.isEmpty()) {
                Text("Sin ganancias registradas en este periodo", style = MaterialTheme.typography.bodySmall)
            } else key(points.first().periodStart, points.last().periodStart) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val pageWidth = maxWidth
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(points.chunked(7), key = { it.first().periodStart }) { page ->
                            Column(Modifier.width(pageWidth), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (page.size == 1) page.first().label else "${page.first().label} — ${page.last().label}",
                                    style = MaterialTheme.typography.labelMedium)
                                if (page.all { it.amountCents == 0L }) {
                                    Text("Sin ganancias registradas en estas fechas", Modifier.heightIn(min = 80.dp),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    val bars = page.map { point -> BarChartData.Bar(
                                        value = point.amountCents / 100f, color = Orange, label = point.label) }
                                    BarChart(barChartData = BarChartData(bars),
                                        modifier = Modifier.fillMaxWidth().height(160.dp).semantics {
                                            contentDescription = page.joinToString("; ") { "${it.label}: " +
                                                String.format(Locale.forLanguageTag("es-PE"), "S/ %.2f", it.amountCents / 100.0) }
                                        }, labelDrawer = SimpleValueDrawer(drawLocation = SimpleValueDrawer.DrawLocation.XAxis))
                                }
                            }
                        }
                    }
                }
                if (points.size > 7) Text("Desliza el gráfico para ver más fechas.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

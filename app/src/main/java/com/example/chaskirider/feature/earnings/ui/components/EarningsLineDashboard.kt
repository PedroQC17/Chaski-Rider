package com.example.chaskirider.feature.earnings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.earnings.domain.ActivityChartPoint
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextMuted
import com.github.tehras.charts.line.LineChart
import com.github.tehras.charts.line.LineChartData
import com.github.tehras.charts.line.renderer.line.SolidLineDrawer
import com.github.tehras.charts.line.renderer.point.FilledCircularPointDrawer
import com.github.tehras.charts.line.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.line.renderer.yaxis.SimpleYAxisDrawer
import java.util.Locale

@Composable
fun EarningsLineDashboard(points: List<ActivityChartPoint>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (points.firstOrNull()?.monthly == true) "Ganancias por mes" else "Ganancias por día",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            if (points.isEmpty()) {
                Text("Sin ganancias registradas en este periodo", style = MaterialTheme.typography.bodySmall)
            } else key(points.first().periodStart, points.last().periodStart) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val pageWidth = maxWidth
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(points.chunked(7), key = { it.first().periodStart }) { page ->
                            Column(Modifier.width(pageWidth), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (page.size == 1) page.first().label else "${page.first().label} — ${page.last().label}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                when {
                                    page.all { it.amountCents == 0L } -> Text(
                                        "Sin ganancias registradas en estas fechas",
                                        Modifier.heightIn(min = 80.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    page.size == 1 -> Text(
                                        "${page.first().label}: ${money(page.first().amountCents)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    else -> LineChart(
                                        linesChartData = listOf(
                                            LineChartData(
                                                points = page.map { LineChartData.Point(it.amountCents / 100f, it.label) },
                                                startAtZero = true,
                                                lineDrawer = SolidLineDrawer(color = Orange)
                                            )
                                        ),
                                        labels = page.map { it.label },
                                        modifier = Modifier.fillMaxWidth().height(160.dp).semantics {
                                            contentDescription = page.joinToString("; ") {
                                                "${it.label}: ${money(it.amountCents)}"
                                            }
                                        },
                                        pointDrawer = FilledCircularPointDrawer(color = Orange),
                                        xAxisDrawer = SimpleXAxisDrawer(labelTextColor = TextMuted, axisLineColor = BorderLight),
                                        yAxisDrawer = SimpleYAxisDrawer()
                                    )
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

private fun money(cents: Long): String =
    String.format(Locale.forLanguageTag("es-PE"), "S/ %.2f", cents / 100.0)

package com.example.chaskirider.feature.earnings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.earnings.domain.IncomeSplit
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer
import java.util.Locale

@Composable
fun IncomeSplitDashboard(split: IncomeSplit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Distribución de ingresos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (split.chaskiCents == 0L && split.tipsCents == 0L) {
                Text("Sin ingresos registrados en este periodo", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PieChart(
                        pieChartData = PieChartData(
                            listOf(
                                PieChartData.Slice(value = split.chaskiCents.toFloat(), color = Orange),
                                PieChartData.Slice(value = split.tipsCents.toFloat(), color = SuccessGreen)
                            )
                        ),
                        modifier = Modifier.size(160.dp).semantics {
                            contentDescription = String.format(
                                Locale.forLanguageTag("es-PE"),
                                "Pago Chaski: S/ %.2f; Propinas: S/ %.2f",
                                split.chaskiCents / 100.0,
                                split.tipsCents / 100.0
                            )
                        },
                        sliceDrawer = SimpleSliceDrawer()
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendItem("Chaski", Orange, split.chaskiCents)
                        LegendItem("Propinas", SuccessGreen, split.tipsCents)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, cents: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                String.format(Locale.forLanguageTag("es-PE"), "S/ %.2f", cents / 100.0),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

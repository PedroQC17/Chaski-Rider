package com.example.chaskirider.feature.earnings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.chaskirider.feature.earnings.domain.IncomeSplit
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
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
                val bars = listOf(
                    BarChartData.Bar(value = split.chaskiCents / 100f, color = Orange, label = "Chaski"),
                    BarChartData.Bar(value = split.tipsCents / 100f, color = SuccessGreen, label = "Propinas")
                )
                BarChart(
                    barChartData = BarChartData(bars),
                    modifier = Modifier.fillMaxWidth().height(150.dp).semantics {
                        contentDescription = String.format(
                            Locale.forLanguageTag("es-PE"),
                            "Pago Chaski: S/ %.2f; Propinas: S/ %.2f",
                            split.chaskiCents / 100.0,
                            split.tipsCents / 100.0
                        )
                    },
                    labelDrawer = SimpleValueDrawer(drawLocation = SimpleValueDrawer.DrawLocation.XAxis)
                )
            }
        }
    }
}

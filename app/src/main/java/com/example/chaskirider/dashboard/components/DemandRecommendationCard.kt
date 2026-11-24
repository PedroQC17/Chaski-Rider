package com.example.chaskirider.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.orders.domain.DemandRecommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DemandRecommendationCard(recommendation: DemandRecommendation?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Predicción de demanda",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (recommendation != null) {
                Text(
                    "Zona recomendada: ${recommendation.zone.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Horario: ${hour(recommendation.periodStart)}–${hour(recommendation.periodEnd)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Nivel: ${level(recommendation.level)}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "Aún no hay datos suficientes para recomendar una zona.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "Predicción basada en datos disponibles. No garantiza pedidos.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun hour(time: Long): String =
    SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-PE"))
        .apply { timeZone = TimeZone.getTimeZone("America/Lima") }
        .format(Date(time))

private fun level(value: String): String = when (value) {
    "HIGH" -> "alta"
    "MEDIUM" -> "media"
    "LOW" -> "baja"
    else -> value.lowercase(Locale.forLanguageTag("es-PE"))
}

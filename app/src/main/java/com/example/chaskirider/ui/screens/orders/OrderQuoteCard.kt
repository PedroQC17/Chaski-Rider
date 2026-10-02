package com.example.chaskirider.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.domain.orders.DeliveryQuote
import java.util.Locale

@Composable
internal fun OrderQuoteCard(quote: DeliveryQuote, additional: Boolean) {
    Text(stringResource(R.string.orders_store), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.orders_route_summary, (quote.approachMeters + quote.deliveryMeters) / 1000.0,
        (quote.durationSeconds + 59) / 60), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.orders_route_order, quote.stops.joinToString(" → ") { it.id.uppercase(Locale.ROOT) }),
        style = MaterialTheme.typography.bodySmall)
    HorizontalDivider(Modifier.padding(vertical = 6.dp))
    PaymentLine(stringResource(R.string.orders_approach, quote.approachMeters / 1000.0), quote.approachCents)
    PaymentLine(stringResource(R.string.orders_deliveries, quote.deliveryMeters / 1000.0), quote.deliveryCents)
    if (quote.guaranteeCents > 0) PaymentLine(stringResource(R.string.orders_guarantee), quote.guaranteeCents)
    PaymentLine(stringResource(R.string.orders_total), quote.totalCents, true)
    if (additional) Text(stringResource(R.string.orders_additional, quote.additionalCents / 100.0),
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun PaymentLine(label: String, cents: Int, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.orders_money, cents / 100.0), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

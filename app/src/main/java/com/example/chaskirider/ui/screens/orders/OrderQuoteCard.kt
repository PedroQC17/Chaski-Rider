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
internal fun OrderQuoteCard(
    quote: DeliveryQuote,
    additional: Boolean,
    merchantName: String? = null,
    merchantZone: String? = null,
    offerReason: String? = null,
    reasonMeters: Int? = null,
    waitingCompensationCents: Int = 0
) {
    // HU07: recojo identificado, destino aproximado, base + extras y propina separada.
    val storeTitle = merchantName?.let { name -> merchantZone?.let { zone -> "$name · $zone" } ?: name }
        ?: stringResource(R.string.orders_store)
    Text(storeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (offerReason == "PROXIMITY" && reasonMeters != null) Text(
        stringResource(R.string.orders_reason_proximity, reasonMeters / 1000.0),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    else if (offerReason != null) Text(stringResource(R.string.orders_reason_generic),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.orders_route_summary, (quote.approachMeters + quote.deliveryMeters) / 1000.0,
        (quote.durationSeconds + 59) / 60), style = MaterialTheme.typography.bodySmall)
    val zones = quote.stops.mapNotNull { it.zone?.takeIf(String::isNotBlank) }.distinct()
    if (zones.isNotEmpty()) Text(stringResource(R.string.orders_destinations, zones.joinToString(", ")),
        style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.orders_route_order, quote.stops.joinToString(" → ") { it.id.uppercase(Locale.ROOT) }),
        style = MaterialTheme.typography.bodySmall)
    HorizontalDivider(Modifier.padding(vertical = 6.dp))
    if (quote.baseCents > 0) PaymentLine(stringResource(R.string.orders_base), quote.baseCents)
    if (quote.approachCents > 0) PaymentLine(stringResource(R.string.orders_approach, quote.approachMeters / 1000.0), quote.approachCents)
    if (quote.deliveryCents > 0) PaymentLine(stringResource(R.string.orders_deliveries, quote.deliveryMeters / 1000.0), quote.deliveryCents)
    if (quote.tipCents > 0) PaymentLine(stringResource(R.string.orders_tip), quote.tipCents)
    if (quote.guaranteeCents > 0) PaymentLine(stringResource(R.string.orders_guarantee), quote.guaranteeCents)
    // HU08: Compensación por espera
    if (waitingCompensationCents > 0) {
        PaymentLine("Compensación por espera", waitingCompensationCents)
    }
    PaymentLine(stringResource(R.string.orders_total), quote.totalCents + waitingCompensationCents, true)
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

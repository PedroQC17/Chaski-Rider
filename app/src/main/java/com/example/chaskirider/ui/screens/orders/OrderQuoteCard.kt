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

@Composable
internal fun OrderQuoteCard(
    quote: DeliveryQuote,
    additional: Boolean,
    merchantName: String? = null,
    merchantZone: String? = null,
    merchantAddress: String? = null,
    offerReason: String? = null,
    reasonMeters: Int? = null,
    waitingCompensationCents: Int = 0,
    showPaymentDetails: Boolean = true
) {
    val storeTitle = merchantName ?: stringResource(R.string.orders_store)
    LocationSection("1", "RECOJO", storeTitle, merchantAddress, merchantZone, MaterialTheme.colorScheme.primary)
    quote.stops.forEach { stop ->
        LocationSection("2", "ENTREGA", "Dirección de entrega", stop.address, stop.zone, androidx.compose.ui.graphics.Color(0xFF263238))
    }
    if (offerReason == "PROXIMITY" && reasonMeters != null) Text(
        stringResource(R.string.orders_reason_proximity, reasonMeters / 1000.0),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    else if (offerReason != null) Text(stringResource(R.string.orders_reason_generic),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.orders_route_summary, (quote.approachMeters + quote.deliveryMeters) / 1000.0,
        (quote.durationSeconds + 59) / 60), style = MaterialTheme.typography.bodySmall)
    HorizontalDivider(Modifier.padding(vertical = 6.dp))
    if (showPaymentDetails) {
        if (quote.baseCents > 0) PaymentLine(stringResource(R.string.orders_base), quote.baseCents)
        if (quote.approachCents > 0) PaymentLine(stringResource(R.string.orders_approach, quote.approachMeters / 1000.0), quote.approachCents)
        if (quote.deliveryCents > 0) PaymentLine(stringResource(R.string.orders_deliveries, quote.deliveryMeters / 1000.0), quote.deliveryCents)
        if (quote.tipCents > 0) PaymentLine(stringResource(R.string.orders_tip), quote.tipCents)
        if (quote.guaranteeCents > 0) PaymentLine(stringResource(R.string.orders_guarantee), quote.guaranteeCents)
        // HU08: Compensación por espera
        if (waitingCompensationCents > 0) {
            PaymentLine("Compensación por espera", waitingCompensationCents)
        }
    }
    PaymentLine(stringResource(R.string.orders_total), quote.totalCents + waitingCompensationCents, true)
    if (additional) Text(stringResource(R.string.orders_additional, quote.additionalCents / 100.0),
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun LocationSection(number: String, label: String, title: String, address: String?, zone: String?, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = color, contentColor = androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(number, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                zone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun PaymentLine(label: String, cents: Int, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.orders_money, cents / 100.0), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

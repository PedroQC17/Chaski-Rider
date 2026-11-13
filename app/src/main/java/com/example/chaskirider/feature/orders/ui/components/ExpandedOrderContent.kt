package com.example.chaskirider.feature.orders.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersUiState

@Composable
internal fun ExpandedOrderContent(
    state: OrdersUiState,
    actionsEnabled: Boolean,
    onRefresh: () -> Unit,
    onRetrySync: () -> Unit,
    onSupport: () -> Unit,
    actions: OrderActions,
) {
    val snapshot = state.snapshot
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch
    val scroll = rememberScrollState()
    LaunchedEffect(offer?.id, snapshot?.batch?.stops?.firstOrNull()?.orderId,
        snapshot?.arrivedAt, snapshot?.pickedUp, snapshot?.isCompleted) { scroll.scrollTo(0) }

    if (offer != null) {
        Column(Modifier.height(LocalConfiguration.current.screenHeightDp.dp * 0.25f).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nueva oferta", fontWeight = FontWeight.Bold)
                Text("${state.secondsLeft} s", color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.orders_money, offer.quote.totalCents / 100.0), fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RECOJO · ${snapshot.merchantName.orEmpty()}", fontWeight = FontWeight.SemiBold)
                snapshot.merchantAddress?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("ENTREGA", fontWeight = FontWeight.SemiBold)
                Text(offer.quote.stops.firstOrNull()?.address.orEmpty(), style = MaterialTheme.typography.bodySmall)
                state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = actions.onReject, enabled = actionsEnabled && state.secondsLeft > 0 && state.isAvailable, modifier = Modifier.weight(1f)) { Text("Rechazar") }
                Button(onClick = actions.onAccept, enabled = actionsEnabled && state.secondsLeft > 0 && state.isAvailable, modifier = Modifier.weight(1f)) { Text("Aceptar") }
            }
        }
        return
    }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.25f
    Column(Modifier.height(maxHeight)) {
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            OrderSyncNotice(state.sync, onRetrySync)


            state.error?.let { errorRes ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(errorRes),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onRefresh, enabled = !state.busy) {
                            Text(stringResource(R.string.orders_refresh))
                        }
                    }
                }
            }

            if (offer != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.orders_new_offer), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.orders_seconds, state.secondsLeft), color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(progress = { (state.secondsLeft / 45f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            } else {
                val title = when {
                    snapshot?.completedUnsuccessfullyAt != null -> "Completado · Entrega no exitosa"
                    snapshot?.isDelivered == true -> "Entrega completada con éxito"
                    snapshot?.pickedUp == true -> " En camino al cliente"
                    snapshot?.arrivedAt != null -> " En establecimiento"
                    quote != null -> stringResource(R.string.orders_accepted)
                    else -> stringResource(R.string.orders_empty)
                }
                Text(title, fontWeight = FontWeight.Bold)
            }

            snapshot?.problem?.let { problem ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Problema registrado", fontWeight = FontWeight.Bold)
                        Text(problemLabel(problem.reason), style = MaterialTheme.typography.bodyMedium)
                        Text(problem.detail, style = MaterialTheme.typography.bodySmall)
                        Text("Consulta Ayuda para resolver esta incidencia.",
                            style = MaterialTheme.typography.bodySmall)

                    }
                }
            }

            quote?.let {
                OrderQuoteCard(
                    quote = it,
                    additional = offer != null && snapshot?.batch != null,
                    merchantName = snapshot?.merchantName,
                    merchantZone = snapshot?.merchantZone,
                    merchantAddress = snapshot?.merchantAddress,
                    offerReason = offer?.reason,
                    reasonMeters = offer?.reasonMeters,
                    waitingCompensationCents = snapshot?.waitingCompensationCents ?: 0,
                    showPaymentDetails = offer != null || snapshot?.isCompleted == true
                )
            }

            if (offer != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ganancia del pedido", fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.orders_money, offer.quote.totalCents / 100.0), fontWeight = FontWeight.Bold)
            }
            OrderActionControls(
                state = state,
                actionsEnabled = actionsEnabled,
                actions = actions,
            )
        }
    }
}

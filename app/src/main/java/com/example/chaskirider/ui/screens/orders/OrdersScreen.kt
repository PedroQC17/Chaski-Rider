package com.example.chaskirider.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chaskirider.R

@Composable
fun OrdersRoute(viewModel: OrdersViewModel, onMenu: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    OrdersScreen(state, onMenu, viewModel::createOffer, viewModel::accept, viewModel::reject,
        viewModel::reset, viewModel::pickup, viewModel::refresh, viewModel::scheduleOffer)
}

@Composable
private fun OrdersScreen(state: OrdersUiState, onMenu: () -> Unit, onOffer: () -> Unit,
    onAccept: () -> Unit, onReject: () -> Unit, onReset: () -> Unit, onPickup: () -> Unit,
    onRefresh: () -> Unit, onSchedule: () -> Unit) {
    val snapshot = state.snapshot
    val actionsEnabled = !state.busy && state.error == null
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch
    Box(Modifier.fillMaxSize()) {
        OrdersMap(snapshot)
        Surface(Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopStart),
            shape = RoundedCornerShape(22.dp), shadowElevation = 4.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, stringResource(R.string.orders_menu)) }
                Text(stringResource(R.string.orders_demo), Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold)
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.orders_demo_note), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh, enabled = !state.busy) { Text(stringResource(R.string.orders_refresh)) }
                }
                if (offer != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.orders_new_offer), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.orders_seconds, state.secondsLeft), color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress = { state.secondsLeft / 45f }, modifier = Modifier.fillMaxWidth())
                } else Text(stringResource(if (snapshot?.pickedUp == true) R.string.orders_picked_up
                    else if (quote != null) R.string.orders_accepted else R.string.orders_empty), fontWeight = FontWeight.Bold)
                quote?.let { OrderQuoteCard(it, offer != null && snapshot.batch != null,
                    snapshot?.merchantName, snapshot?.merchantZone, offer?.reason, offer?.reasonMeters) }
                if (offer != null) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onReject, enabled = actionsEnabled && state.secondsLeft > 0,
                        modifier = Modifier.weight(1f)) { Text(stringResource(R.string.orders_reject)) }
                    Button(onClick = onAccept, enabled = actionsEnabled && state.secondsLeft > 0,
                        modifier = Modifier.weight(1f)) { Text(stringResource(R.string.orders_accept)) }
                } else {
                    val canOffer = snapshot != null && !snapshot.pickedUp && (snapshot.batch?.stops?.size ?: 0) < 3
                    if (canOffer) {
                        Button(onClick = onOffer, enabled = actionsEnabled && !state.scheduling, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.orders_generate))
                        }
                        TextButton(onClick = onSchedule, enabled = actionsEnabled && !state.scheduling, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (state.scheduling) R.string.orders_scheduled else R.string.orders_schedule))
                        }
                    }
                    if (quote != null && snapshot?.pickedUp == false) OutlinedButton(onClick = onPickup,
                        enabled = actionsEnabled && !state.scheduling, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.orders_pickup_demo)) }
                    if (quote != null) TextButton(onClick = onReset, enabled = actionsEnabled && !state.scheduling,
                        modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.orders_reset)) }
                }
            }
        }
    }
}

package com.example.chaskirider.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chaskirider.R
import com.example.chaskirider.domain.orders.DeliveryOffer
import com.example.chaskirider.domain.orders.DeliveryQuote
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.example.chaskirider.ui.screens.chat.ChatDialog
import com.example.chaskirider.ui.screens.chat.ChatUiState
import com.example.chaskirider.ui.screens.chat.ChatViewModel
import com.example.chaskirider.ui.theme.Orange
import java.util.Locale

@Composable
fun OrdersRoute(viewModel: OrdersViewModel, onMenu: () -> Unit, isDemo: Boolean = false) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatViewModel: ChatViewModel = viewModel()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                viewModel.fetchCurrentLocation()
            }
        }
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        viewModel.fetchCurrentLocation()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.snapshot) {
        chatViewModel.syncOrderState(state.snapshot)
    }

    OrdersScreen(
        state = state,
        chatState = chatState,
        chatViewModel = chatViewModel,
        isDemo = isDemo,
        onMenu = onMenu,
        onOffer = viewModel::createOffer,
        onAccept = viewModel::accept,
        onReject = viewModel::reject,
        onArriveMerchant = viewModel::arriveAtMerchant,
        onReportNotReady = viewModel::reportNotReady,
        onPickup = viewModel::pickup,
        onDeliver = viewModel::deliver,
        onReset = viewModel::reset,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
        onSchedule = viewModel::scheduleOffer
    )
}

@Composable
private fun OrdersScreen(
    state: OrdersUiState,
    chatState: ChatUiState,
    chatViewModel: ChatViewModel,
    isDemo: Boolean,
    onMenu: () -> Unit,
    onOffer: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onArriveMerchant: () -> Unit,
    onReportNotReady: () -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    onSchedule: () -> Unit
) {
    val snapshot = state.snapshot
    val actionsEnabled = !state.busy
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch
    var isBottomCardMinimized by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        OrdersMap(snapshot)

        OrdersTopHeader(isDemo = isDemo, onMenu = onMenu)

        if (quote != null) {
            ChatFloatingActionButton(
                onClick = { chatViewModel.openChat() }
            )
        }

        Surface(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            if (isBottomCardMinimized) {
                MinimizedOrderSummary(
                    snapshot = snapshot,
                    quote = quote,
                    offer = offer,
                    state = state,
                    isDemo = isDemo,
                    onExpand = { isBottomCardMinimized = false }
                )
            } else {
                ExpandedOrderContent(
                    state = state,
                    isDemo = isDemo,
                    actionsEnabled = actionsEnabled,
                    onMinimize = { isBottomCardMinimized = true },
                    onRefresh = onRefresh,
                    onClearError = onClearError,
                    onReject = onReject,
                    onAccept = onAccept,
                    onArriveMerchant = onArriveMerchant,
                    onReportNotReady = onReportNotReady,
                    onPickup = onPickup,
                    onDeliver = onDeliver,
                    onReset = onReset,
                    onOffer = onOffer,
                    onSchedule = onSchedule
                )
            }
        }

        ChatDialog(
            state = chatState,
            onClose = chatViewModel::closeChat,
            onSelectChannel = chatViewModel::selectChannel,
            onUpdateInputText = chatViewModel::updateInputText,
            onSpeechResult = chatViewModel::onSpeechRecognized,
            onSendMessage = chatViewModel::sendMessage
        )
    }
}

@Composable
private fun OrdersTopHeader(isDemo: Boolean, onMenu: () -> Unit) {
    Surface(
        Modifier.statusBarsPadding().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, stringResource(R.string.orders_menu)) }
            Text(
                if (isDemo) stringResource(R.string.orders_demo) else stringResource(R.string.text_pedidos),
                Modifier.padding(end = 12.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BoxScope.ChatFloatingActionButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 16.dp, end = 16.dp),
        containerColor = Orange,
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        elevation = FloatingActionButtonDefaults.elevation(4.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Chat del Pedido",
                modifier = Modifier.size(20.dp)
            )
        },
        text = {
            Text(
                text = "Chat",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    )
}

@Composable
private fun MinimizedOrderSummary(
    snapshot: OfferSnapshot?,
    quote: DeliveryQuote?,
    offer: DeliveryOffer?,
    state: OrdersUiState,
    isDemo: Boolean,
    onExpand: () -> Unit
) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = when {
                    snapshot?.isDelivered == true -> " ¡Entrega completada!"
                    snapshot?.pickedUp == true -> " En camino al cliente"
                    snapshot?.arrivedAt != null -> " En establecimiento"
                    quote != null -> " Pedido activo (Toca para expandir)"
                    offer != null -> " ¡Nueva oferta de pedido!"
                    else -> if (isDemo) " Demo de pedidos" else " Pedidos"
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            if (quote != null) {
                val distText = if (snapshot?.pickedUp == true) state.distanceToCustomerMeters?.let { "Cliente a ${it.toInt()}m" }
                else state.distanceToMerchantMeters?.let { "Local a ${it}m" }
                distText?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        IconButton(onClick = onExpand) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Expandir")
        }
    }
}

@Composable
private fun ExpandedOrderContent(
    state: OrdersUiState,
    isDemo: Boolean,
    actionsEnabled: Boolean,
    onMinimize: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onArriveMerchant: () -> Unit,
    onReportNotReady: () -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onReset: () -> Unit,
    onOffer: () -> Unit,
    onSchedule: () -> Unit
) {
    val snapshot = state.snapshot
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch

    Column(
        Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDemo) {
                Text(
                    stringResource(R.string.orders_demo_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    "Pedidos",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar")
            }
        }

        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onRefresh, enabled = !state.busy) {
                            Text(stringResource(R.string.orders_refresh))
                        }
                        if (isDemo) {
                            Button(
                                onClick = onClearError,
                                enabled = !state.busy,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Saltar error (Demo)")
                            }
                        }
                    }
                }
            }
        }

        if (offer != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.orders_new_offer), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.orders_seconds, state.secondsLeft), color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { state.secondsLeft / 45f }, modifier = Modifier.fillMaxWidth())
        } else {
            val title = when {
                snapshot?.isDelivered == true -> " ¡Entrega completada con éxito!"
                snapshot?.pickedUp == true -> " En camino al cliente"
                snapshot?.arrivedAt != null -> " En establecimiento"
                quote != null -> stringResource(R.string.orders_accepted)
                else -> stringResource(R.string.orders_empty)
            }
            Text(title, fontWeight = FontWeight.Bold)
        }

        quote?.let {
            OrderQuoteCard(
                quote = it,
                additional = offer != null && snapshot.batch != null,
                merchantName = snapshot?.merchantName,
                merchantZone = snapshot?.merchantZone,
                offerReason = offer?.reason,
                reasonMeters = offer?.reasonMeters,
                waitingCompensationCents = snapshot?.waitingCompensationCents ?: 0
            )
        }

        OrderActionControls(
            state = state,
            isDemo = isDemo,
            actionsEnabled = actionsEnabled,
            onReject = onReject,
            onAccept = onAccept,
            onArriveMerchant = onArriveMerchant,
            onReportNotReady = onReportNotReady,
            onPickup = onPickup,
            onDeliver = onDeliver,
            onReset = onReset,
            onOffer = onOffer,
            onSchedule = onSchedule
        )
    }
}

@Composable
private fun OrderActionControls(
    state: OrdersUiState,
    isDemo: Boolean,
    actionsEnabled: Boolean,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onArriveMerchant: () -> Unit,
    onReportNotReady: () -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onReset: () -> Unit,
    onOffer: () -> Unit,
    onSchedule: () -> Unit
) {
    val snapshot = state.snapshot
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch

    if (offer != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onReject,
                enabled = actionsEnabled && state.secondsLeft > 0,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.orders_reject)) }

            Button(
                onClick = onAccept,
                enabled = actionsEnabled && state.secondsLeft > 0,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.orders_accept)) }
        }
    } else if (quote != null) {
        val isDelivered = snapshot?.isDelivered == true
        val isPickedUp = snapshot?.pickedUp == true

        if (isDelivered) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        " El pedido fue entregado al cliente.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Tracking de ruta finalizado correctamente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = onReset,
                enabled = actionsEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente Pedido")
            }
        } else if (isPickedUp) {
            val distCustomer = state.distanceToCustomerMeters
            if (distCustomer != null) {
                Text(
                    text = String.format(Locale.getDefault(), "Distancia al cliente: %.1f km", distCustomer / 1000.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(
                onClick = onDeliver,
                enabled = actionsEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmar Entrega al Cliente")
            }

            if (isDemo) {
                TextButton(
                    onClick = onReset,
                    enabled = actionsEnabled && !state.scheduling,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.orders_reset)) }
            }
        } else {
            val arrived = snapshot?.arrivedAt != null

            if (!arrived) {
                val dist = state.distanceToMerchantMeters
                if (dist != null) {
                    Text(
                        text = "Distancia al establecimiento: $dist m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                val canConfirmArrival = state.isWithinGeofence || dist == null
                Button(
                    onClick = onArriveMerchant,
                    enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (canConfirmArrival || !isDemo) "Confirmar llegada al establecimiento"
                        else "Continuar (Saltar demo - a $dist m)"
                    )
                }
            } else {
                val notReady = snapshot.isNotReadyReported
                if (!notReady) {
                    OutlinedButton(
                        onClick = onReportNotReady,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reportar: Pedido no está listo")
                    }
                } else {
                    val mins = state.elapsedWaitSeconds / 60
                    val secs = state.elapsedWaitSeconds % 60
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "⏱ Tiempo de espera: $timeStr min",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (state.elapsedWaitSeconds > 300) {
                                val extraMins = (state.elapsedWaitSeconds - 300 + 59) / 60
                                val compSol = (extraMins * 20) / 100.0
                                Text(
                                    text = String.format(Locale.getDefault(), "Compensación acumulada: S/ %.2f", compSol),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onPickup,
                    enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar Recojo de Pedido")
                }
            }

            if (isDemo) {
                TextButton(
                    onClick = onReset,
                    enabled = actionsEnabled && !state.scheduling,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.orders_reset)) }
            }
        }
    } else {
        if (isDemo) {
            val canOffer = snapshot != null && !snapshot.pickedUp && (snapshot.batch?.stops?.size ?: 0) < 3
            if (canOffer) {
                Button(
                    onClick = onOffer,
                    enabled = actionsEnabled && !state.scheduling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.orders_generate))
                }
                TextButton(
                    onClick = onSchedule,
                    enabled = actionsEnabled && !state.scheduling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (state.scheduling) R.string.orders_scheduled else R.string.orders_schedule))
                }
            }
        } else {
            Text(
                "Esperando ofertas de pedidos en tiempo real...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

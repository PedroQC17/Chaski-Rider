package com.example.chaskirider.ui.screens.orders

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chaskirider.domain.orders.OrderProblemReason
import com.example.chaskirider.R
import com.example.chaskirider.ui.screens.chat.ChatDialog
import com.example.chaskirider.ui.screens.chat.ChatUiState
import com.example.chaskirider.ui.screens.chat.ChatViewModel
import com.example.chaskirider.ui.theme.Orange
import java.util.Locale

@Composable
fun OrdersRoute(viewModel: OrdersViewModel, onMenu: () -> Unit, isDemo: Boolean = false, onSupport: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel {
        ChatViewModel(com.example.chaskirider.di.AppContainer.orderChatRepository,
            com.example.chaskirider.di.AppContainer.chatDraftStore,
            com.example.chaskirider.di.AppContainer.riderSession)
    }) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ success->
        if (success && state.evidencePhotoUri != null){
            viewModel.setEvidencePhoto(state.evidencePhotoUri!!)
        }
    }

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
        onClearEvidencePhoto = viewModel::clearEvidencePhoto,
        onTakeEvidencePhoto = { uri -> viewModel.setEvidencePhoto(uri); cameraLauncher.launch(uri) },
        onUpdateDeliveryNote = viewModel::updateDeliveryNote,
        onReset = { if (state.snapshot?.batch != null && state.snapshot?.isDelivered != true) viewModel.openProblem() else viewModel.reset() },
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
        onSupport = onSupport,
    )
    if (state.problemDialogOpen) OrderProblemDialog(state, viewModel::closeProblem,
        viewModel::selectProblem, viewModel::updateProblemDetail, viewModel::confirmProblem)
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
    onClearEvidencePhoto: () -> Unit,
    onTakeEvidencePhoto: (Uri) -> Unit,
    onUpdateDeliveryNote: (String) -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    onSupport: () -> Unit,
) {
    val snapshot = state.snapshot
    val actionsEnabled = !state.busy
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch
    var bottomCardHeight by remember { mutableStateOf(300.dp) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        OrdersMap(snapshot, state.riderLocation, bottomCardHeight)

        OrdersTopHeader(onMenu = onMenu)

        if (snapshot?.batch != null) {
            ChatFloatingActionButton(
                onClick = { chatViewModel.openChat() }
            )
        }

        Surface(
            Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomCardHeight = with(density) { it.height.toDp() } }
                .navigationBarsPadding().padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            ExpandedOrderContent(
                state = state,
                isDemo = isDemo,
                actionsEnabled = actionsEnabled,
                onRefresh = onRefresh,
                onClearError = onClearError,
                onReject = onReject,
                onAccept = onAccept,
                onArriveMerchant = onArriveMerchant,
                onReportNotReady = onReportNotReady,
                onPickup = onPickup,
                onDeliver = onDeliver,
                onClearEvidencePhoto = onClearEvidencePhoto,
                onTakeEvidencePhoto = onTakeEvidencePhoto,
                onUpdateDeliveryNote = onUpdateDeliveryNote,
                onReset = onReset,
                onOffer = onOffer,
                onSupport = onSupport,
            )
        }

        ChatDialog(
            state = chatState,
            onClose = chatViewModel::closeChat,
            onSelectChannel = chatViewModel::selectChannel,
            onUpdateInputText = chatViewModel::updateInputText,
            onSpeechResult = chatViewModel::onSpeechRecognized,
            onSendMessage = chatViewModel::sendMessage,
            onRetry = chatViewModel::retry,
            onSpeechError = chatViewModel::setSpeechError
        )
    }
}

@Composable
private fun OrdersTopHeader(onMenu: () -> Unit) {
    Surface(
        Modifier.statusBarsPadding().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, stringResource(R.string.orders_menu)) }
            Text(
                stringResource(R.string.text_pedidos),
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
private fun ExpandedOrderContent(
    state: OrdersUiState,
    isDemo: Boolean,
    actionsEnabled: Boolean,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onArriveMerchant: () -> Unit,
    onReportNotReady: () -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onClearEvidencePhoto: () -> Unit,
    onTakeEvidencePhoto: (Uri) -> Unit,
    onUpdateDeliveryNote: (String) -> Unit,
    onReset: () -> Unit,
    onOffer: () -> Unit,
    onSupport: () -> Unit,
) {
    val snapshot = state.snapshot
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch
    val scroll = rememberScrollState()
    LaunchedEffect(offer?.id, snapshot?.batch?.stops?.firstOrNull()?.orderId,
        snapshot?.arrivedAt, snapshot?.pickedUp, snapshot?.isDelivered) { scroll.scrollTo(0) }

    val maxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.55f).coerceAtMost(480.dp)
    Column(Modifier.heightIn(max = maxHeight)) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                                    Text("Cerrar aviso")
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
                LinearProgressIndicator(progress = { (state.secondsLeft / 45f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
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

            snapshot?.problem?.let { problem ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Problema registrado", fontWeight = FontWeight.Bold)
                        Text(problemLabel(problem.reason), style = MaterialTheme.typography.bodyMedium)
                        Text(problem.detail, style = MaterialTheme.typography.bodySmall)
                        Text("Incidencia guardada en este dispositivo. Abre un ticket para enviarla a soporte. El pedido sigue activo; confirma la entrega solo si entregaste el producto.",
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onSupport, enabled = !state.busy) { Text("Abrir ticket de soporte") }
                    }
                }
            }

            quote?.let {
                OrderQuoteCard(
                    quote = it,
                    additional = offer != null && snapshot.batch != null,
                    merchantName = snapshot?.merchantName,
                    merchantZone = snapshot?.merchantZone,
                    merchantAddress = snapshot?.merchantAddress,
                    offerReason = offer?.reason,
                    reasonMeters = offer?.reasonMeters,
                    waitingCompensationCents = snapshot?.waitingCompensationCents ?: 0,
                    showPaymentDetails = offer != null || snapshot?.isDelivered == true
                )
            }

        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (offer != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ganancia del pedido", fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.orders_money, offer.quote.totalCents / 100.0), fontWeight = FontWeight.Bold)
            }
            OrderNavigationButton(snapshot, enabled = actionsEnabled)
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
                onClearEvidencePhoto = onClearEvidencePhoto,
                onTakeEvidencePhoto = onTakeEvidencePhoto,
                onUpdateDeliveryNote = onUpdateDeliveryNote,
                onReset = onReset,
                onOffer = onOffer,
            )
        }
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
    onClearEvidencePhoto: () -> Unit,
    onTakeEvidencePhoto: (Uri) -> Unit,
    onUpdateDeliveryNote: (String) -> Unit,
    onReset: () -> Unit,
    onOffer: () -> Unit,
) {
    val snapshot = state.snapshot
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch

    if (offer != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onReject,
                enabled = actionsEnabled && state.secondsLeft > 0 && state.isAvailable,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.orders_reject)) }

            Button(
                onClick = onAccept,
                enabled = actionsEnabled && state.secondsLeft > 0 && state.isAvailable,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.orders_accept)) }
        }
    } else if (quote != null) {
        val isDelivered = snapshot?.isDelivered == true
        val isPickedUp = snapshot?.pickedUp == true

        if (isDelivered) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
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
                        "Tu entrega quedó registrada.",
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

            if (!state.isNearCustomer) Text(
                stringResource(if (distCustomer == null) R.string.orders_checking_location else R.string.orders_near_delivery),
                style = MaterialTheme.typography.bodySmall)

            // 📷 FOTO DE EVIDENCIA DE ENTREGA
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Foto de evidencia de entrega",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (state.evidencePhotoUri != null) {
                        AsyncImage(
                            model = state.evidencePhotoUri,
                            contentDescription = "Evidencia",
                            modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        OutlinedButton(
                            onClick = onClearEvidencePhoto,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Volver a tomar foto")
                        }
                    } else {
                        Button(
                            onClick = {
                                val tempUri = Uri.parse("file:///data/user/0/com.example.chaskirider/cache/temp_evidence.jpg")
                                onTakeEvidencePhoto(tempUri)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tomar foto de entrega")
                        }
                    }
                }
            }

            // 📝 CAMPO OPCIONAL PARA NOTA DE ENTREGA
            OutlinedTextField(
                value = state.deliveryNote ?: "",
                onValueChange = onUpdateDeliveryNote,
                label = { Text("Nota de entrega (opcional)") },
                placeholder = { Text("Ej: Dejado en recepción") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = onDeliver,
                enabled = actionsEnabled && (state.isNearCustomer || isDemo) && state.evidencePhotoUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmar entrega")
            }

            if (isDemo) {
                TextButton(
                    onClick = onReset,
                    enabled = actionsEnabled && snapshot?.problem == null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (snapshot?.problem != null) "Problema registrado" else "Tengo un problema") }
            }
        } else {
            val arrived = snapshot?.arrivedAt != null
            if (!state.isWithinGeofence) Text(
                stringResource(if (state.distanceToMerchantMeters == null) R.string.orders_checking_location else R.string.orders_near_pickup),
                style = MaterialTheme.typography.bodySmall)

            if (!arrived) {
                val dist = state.distanceToMerchantMeters
                if (dist != null) {
                    Text(
                        text = "Distancia al establecimiento: $dist m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Button(
                    onClick = onArriveMerchant,
                    enabled = actionsEnabled && state.isWithinGeofence,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Llegué al local"
                    )
                }
            } else {
                val notReady = snapshot.isNotReadyReported
                if (!notReady) {
                    OutlinedButton(
                        onClick = onReportNotReady,
                        enabled = actionsEnabled && state.isWithinGeofence,
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
                    enabled = actionsEnabled && state.isWithinGeofence,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar recojo")
                }
            }

            if (isDemo) {
                TextButton(
                    onClick = onReset,
                    enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar pedido") }
            }
        }
    } else {
        if (isDemo) {
            val canOffer = snapshot != null && state.isAvailable
            Text(stringResource(if (!state.isAvailable) R.string.orders_connect_to_search else if (state.searched) R.string.orders_wait_nearby else R.string.orders_searching_nearby), style = MaterialTheme.typography.bodySmall)
            if (canOffer) {
                Button(
                    onClick = onOffer,
                    enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.orders_generate))
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

private fun problemLabel(reason: OrderProblemReason): String = when (reason) {
    OrderProblemReason.LOCAL_CLOSED -> "El local está cerrado"
    OrderProblemReason.ORDER_UNAVAILABLE -> "El local no tiene el pedido"
    OrderProblemReason.VEHICLE_PROBLEM -> "Problema con mi vehículo"
    OrderProblemReason.CUSTOMER_UNREACHABLE -> "El cliente no responde"
    OrderProblemReason.ADDRESS_PROBLEM -> "Problema con la dirección"
    OrderProblemReason.OTHER -> "Otro problema"
}

@Composable
private fun OrderProblemDialog(state: OrdersUiState, onClose: () -> Unit,
    onReason: (OrderProblemReason) -> Unit, onDetail: (String) -> Unit, onConfirm: () -> Unit) {
    val pickedUp = state.snapshot?.pickedUp == true
    val reasons = if (pickedUp) listOf(OrderProblemReason.CUSTOMER_UNREACHABLE, OrderProblemReason.ADDRESS_PROBLEM,
        OrderProblemReason.VEHICLE_PROBLEM, OrderProblemReason.OTHER)
        else listOf(OrderProblemReason.LOCAL_CLOSED, OrderProblemReason.ORDER_UNAVAILABLE,
            OrderProblemReason.VEHICLE_PROBLEM, OrderProblemReason.OTHER)
    AlertDialog(onDismissRequest = onClose,
        title = { Text(if (pickedUp) "Problema con la entrega" else "Cancelar pedido") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (pickedUp) "El pedido seguirá activo. La incidencia se guardará y se sincronizará al tener conexión. Después solicita ayuda a soporte para resolverla."
                    else "Si confirmas, dejarás de atender este pedido y no podrás continuar su recojo. El motivo quedará registrado y se sincronizará al tener conexión.")
                reasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.problemReason == reason, onClick = { onReason(reason) }, enabled = !state.busy)
                        TextButton(onClick = { onReason(reason) }, enabled = !state.busy) { Text(problemLabel(reason)) }
                    }
                }
                OutlinedTextField(value = state.problemDetail, onValueChange = onDetail, enabled = !state.busy,
                    label = { Text("Describe lo ocurrido") }, supportingText = { Text("Entre 10 y 500 caracteres") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm,
            enabled = !state.busy && state.problemReason != null && state.problemDetail.trim().length >= 10) {
            Text(if (pickedUp) "Registrar problema" else "Confirmar cancelación")
        } },
        dismissButton = { TextButton(onClick = onClose, enabled = !state.busy) { Text("Volver al pedido") } })
}

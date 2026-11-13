package com.example.chaskirider.feature.orders.ui.routes

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.feature.chat.ui.viewmodel.ChatViewModel
import com.example.chaskirider.feature.orders.domain.OrderProblemReason
import com.example.chaskirider.feature.orders.ui.components.OrderProblemDialog
import com.example.chaskirider.feature.orders.ui.screens.CustomerContactDialog
import com.example.chaskirider.feature.orders.ui.screens.OrdersScreen
import com.example.chaskirider.feature.orders.ui.viewmodel.CustomerContactViewModel
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersViewModel
import com.example.chaskirider.feature.support.domain.SupportCategory
import com.example.chaskirider.feature.support.domain.SupportTopic
import com.example.chaskirider.feature.support.ui.screens.AssistantDialog
import com.example.chaskirider.feature.support.ui.screens.OrderHelpPage
import com.example.chaskirider.feature.support.ui.viewmodel.AssistantViewModel

private enum class HelpDestination { ORDER, HELP, ASSISTANT, ASSISTANT_FROM_CONTACT, CONTACT }

@Composable
fun OrdersRoute(viewModel: OrdersViewModel, onMenu: () -> Unit, onSupport: () -> Unit = {},
    onOrderHelp: (OrderProblemReason) -> Unit = { onSupport() },
    onTicket: (String) -> Unit = { onSupport() },
    chatViewModel: ChatViewModel = viewModel {
        ChatViewModel(AppContainer.orderChatRepository,
            AppContainer.chatDraftStore,
            AppContainer.riderSession)
    }) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val contactOwner by AppContainer.riderSession.user.collectAsStateWithLifecycle()
    val activeOrderId = state.snapshot?.batch?.stops?.singleOrNull()?.orderId
    var helpPage by rememberSaveable(contactOwner?.id, activeOrderId) { mutableStateOf(HelpDestination.ORDER) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.confirmEvidenceCapture(success)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && state.pendingCaptureUri != null) {
            try { cameraLauncher.launch(state.pendingCaptureUri!!) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val launchCamera = {
        val uri = viewModel.prepareEvidenceCapture(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try { cameraLauncher.launch(uri) } catch (e: Exception) { e.printStackTrace() }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

    if (helpPage == HelpDestination.ORDER || activeOrderId == null || state.snapshot?.isCompleted == true) OrdersScreen(
        state = state,
        chatState = chatState,
        chatViewModel = chatViewModel,
        onMenu = onMenu,
        onAccept = viewModel::accept,
        onReject = viewModel::reject,
        onArriveMerchant = viewModel::arriveAtMerchant,
        onReportNotReady = viewModel::reportNotReady,
        onPickup = viewModel::pickup,
        onDeliver = viewModel::deliver,
        onClearEvidencePhoto = viewModel::clearEvidencePhoto,
        onTakeEvidencePhoto = { launchCamera() },
        onUpdateDeliveryNote = viewModel::updateDeliveryNote,
        onUpdatePin = viewModel::updateDeliveryPin,
        onReset = { if (state.snapshot?.batch != null && state.snapshot?.isCompleted != true) viewModel.openProblem() else viewModel.reset() },
        onRefresh = viewModel::refresh,
        onRetrySync = viewModel::retrySync,
        onSupport = onSupport,
        onOrderHelp = { helpPage = HelpDestination.HELP },
    )
    if (state.problemDialogOpen) OrderProblemDialog(state, viewModel::closeProblem,
        viewModel::selectProblem, viewModel::updateProblemDetail, viewModel::confirmProblem)
    if (helpPage == HelpDestination.HELP && activeOrderId != null && state.snapshot?.isCompleted != true)
        OrderHelpPage("Ayuda con este pedido", { helpPage = HelpDestination.ORDER }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { helpPage = HelpDestination.ASSISTANT }, modifier = Modifier.fillMaxWidth()) { Text("Consultar al asistente Chaski") }
                SupportTopic.entries.filter {
                    it.category == SupportCategory.ORDER && when (it) {
                        SupportTopic.LOCAL_CLOSED,
                        SupportTopic.ORDER_NOT_READY -> state.snapshot?.pickedUp != true
                        SupportTopic.CUSTOMER_UNREACHABLE -> state.snapshot?.pickedUp == true
                        else -> true
                    }
                }.forEach { topic ->
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        helpPage = HelpDestination.ORDER
                        if (topic == SupportTopic.CUSTOMER_UNREACHABLE &&
                            state.snapshot?.pickedUp == true && state.snapshot?.isCompleted != true) helpPage = HelpDestination.CONTACT
                        else onOrderHelp(when (topic) {
                            SupportTopic.CUSTOMER_UNREACHABLE -> OrderProblemReason.CUSTOMER_UNREACHABLE
                            SupportTopic.ORDER_NOT_READY -> OrderProblemReason.ORDER_UNAVAILABLE
                            SupportTopic.LOCAL_CLOSED -> OrderProblemReason.LOCAL_CLOSED
                            SupportTopic.ADDRESS_PROBLEM -> OrderProblemReason.ADDRESS_PROBLEM
                            else -> OrderProblemReason.OTHER
                        })
                    }) { Text(topic.title) }
                }
            }
        }
    val contactStop = state.snapshot?.batch?.stops?.singleOrNull()
    if (helpPage == HelpDestination.CONTACT && contactStop != null && state.snapshot?.pickedUp == true && state.snapshot?.isCompleted != true) {
        val application = context.applicationContext
        val contactViewModel: CustomerContactViewModel = viewModel {
            CustomerContactViewModel(
                AppContainer.customerContactStore,
                AppContainer.riderSession,
                AppContainer.locationRepository,
                boot = { Settings.Global.getInt(application.contentResolver, Settings.Global.BOOT_COUNT, -1) },
                service = AppContainer.customerAbsenceService)
        }
        LaunchedEffect(state.snapshot, contactOwner?.id) { contactViewModel.bind(state.snapshot) }
        CustomerContactDialog(contactViewModel, contactStop.orderId,
            contactStop.customerPhone, onClose = { helpPage = HelpDestination.HELP }, onSupport = {
                helpPage = HelpDestination.ASSISTANT_FROM_CONTACT
            })
    }
    if ((helpPage == HelpDestination.ASSISTANT || helpPage == HelpDestination.ASSISTANT_FROM_CONTACT) &&
        contactStop != null && state.snapshot?.isCompleted != true) {
        val assistant: AssistantViewModel = viewModel {
            AssistantViewModel(
                AppContainer.assistantRepository,
                AppContainer.chatDraftStore,
                AppContainer.riderSession,
                finishOrder = { AppContainer.offerRepository.resolveCustomerAbsence(it) })
        }
        LaunchedEffect(contactStop.orderId) { assistant.bind(contactStop.orderId) }
        val assistantState by assistant.uiState.collectAsStateWithLifecycle()
        AssistantDialog(assistantState, assistant::changeBody,
            onSend = { assistant.send() }, onHuman = { assistant.send(human = true) },
            onResolve = { assistant.resolve { helpPage = HelpDestination.ORDER; viewModel.refresh() } },
            onTicket = { helpPage = HelpDestination.ORDER; onTicket(it) },
            onClose = { helpPage = if (helpPage == HelpDestination.ASSISTANT_FROM_CONTACT) HelpDestination.CONTACT else HelpDestination.HELP })
    }
}

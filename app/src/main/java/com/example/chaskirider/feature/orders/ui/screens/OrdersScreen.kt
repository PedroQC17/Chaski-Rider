package com.example.chaskirider.feature.orders.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.chat.ui.components.ChatDialog
import com.example.chaskirider.feature.chat.ui.viewmodel.ChatUiState
import com.example.chaskirider.feature.chat.ui.viewmodel.ChatViewModel
import com.example.chaskirider.feature.orders.ui.components.ChatFloatingActionButton
import com.example.chaskirider.feature.orders.ui.components.ExpandedOrderContent
import com.example.chaskirider.feature.orders.ui.components.OrderActions
import com.example.chaskirider.feature.orders.ui.components.OrderNavigationButton
import com.example.chaskirider.feature.orders.ui.components.OrdersMap
import com.example.chaskirider.feature.orders.ui.components.OrdersTopHeader
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersUiState

@Composable
internal fun OrdersScreen(
    state: OrdersUiState,
    chatState: ChatUiState,
    chatViewModel: ChatViewModel,
    onMenu: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onArriveMerchant: () -> Unit,
    onReportNotReady: () -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onClearEvidencePhoto: () -> Unit,
    onTakeEvidencePhoto: () -> Unit,
    onUpdateDeliveryNote: (String) -> Unit,
    onUpdatePin: (String) -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onRetrySync: () -> Unit,
    onSupport: () -> Unit,
    onOrderHelp: () -> Unit,
) {
    val snapshot = state.snapshot
    val actionsEnabled = !state.busy
    var bottomCardHeight by remember { mutableStateOf(300.dp) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        OrdersMap(snapshot, state.riderLocation, bottomCardHeight)

        OrdersTopHeader(onMenu = onMenu)
        if (snapshot?.batch != null && !snapshot.isCompleted) {
            FilledTonalButton(onClick = onOrderHelp, modifier = Modifier.align(Alignment.TopEnd)
                .statusBarsPadding().padding(top = 16.dp, end = 16.dp)) { Text("Ayuda") }
        }

        if (snapshot?.batch != null) {
            ChatFloatingActionButton(
                onClick = { chatViewModel.openChat() }
            )
        }

        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
            .padding(end = 16.dp, bottom = bottomCardHeight + 12.dp)) {
            OrderNavigationButton(snapshot, enabled = actionsEnabled)
        }

        Surface(
            Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomCardHeight = with(density) { it.height.toDp() } }
                .navigationBarsPadding().padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            ExpandedOrderContent(
                state = state,
                actionsEnabled = actionsEnabled,
                onRefresh = onRefresh,
                onRetrySync = onRetrySync,
                onSupport = onSupport,
                actions = OrderActions(
                    onAccept = onAccept,
                    onReject = onReject,
                    onArriveMerchant = onArriveMerchant,
                    onReportNotReady = onReportNotReady,
                    onPickup = onPickup,
                    onDeliver = onDeliver,
                    onClearEvidencePhoto = onClearEvidencePhoto,
                    onTakeEvidencePhoto = onTakeEvidencePhoto,
                    onUpdateDeliveryNote = onUpdateDeliveryNote,
                    onUpdatePin = onUpdatePin,
                    onReset = onReset,
                ),
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

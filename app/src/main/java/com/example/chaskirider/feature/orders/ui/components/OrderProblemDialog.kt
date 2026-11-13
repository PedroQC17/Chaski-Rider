package com.example.chaskirider.feature.orders.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.feature.orders.domain.OrderProblemReason
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersUiState

internal fun problemLabel(reason: OrderProblemReason): String = when (reason) {
    OrderProblemReason.LOCAL_CLOSED -> "El local está cerrado"
    OrderProblemReason.ORDER_UNAVAILABLE -> "El local no tiene el pedido"
    OrderProblemReason.VEHICLE_PROBLEM -> "Problema con mi vehículo"
    OrderProblemReason.CUSTOMER_UNREACHABLE -> "El cliente no responde"
    OrderProblemReason.ADDRESS_PROBLEM -> "Problema con la dirección"
    OrderProblemReason.OTHER -> "Otro problema"
}

@Composable
internal fun OrderProblemDialog(state: OrdersUiState, onClose: () -> Unit,
    onReason: (OrderProblemReason) -> Unit, onDetail: (String) -> Unit, onConfirm: () -> Unit) {
    val pickedUp = state.snapshot?.pickedUp == true
    val reasons = if (pickedUp) listOf(OrderProblemReason.CUSTOMER_UNREACHABLE, OrderProblemReason.ADDRESS_PROBLEM,
        OrderProblemReason.VEHICLE_PROBLEM, OrderProblemReason.OTHER)
        else listOf(OrderProblemReason.LOCAL_CLOSED, OrderProblemReason.ORDER_UNAVAILABLE,
            OrderProblemReason.VEHICLE_PROBLEM, OrderProblemReason.OTHER)
    AlertDialog(onDismissRequest = onClose,
        title = { Text(if (pickedUp) "Problema con la entrega" else "Cancelar pedido") },
        text = {
            Column(Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.55f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (pickedUp) "El pedido seguirá activo mientras soporte revisa el problema."
                    else "Indica el motivo para cancelar el pedido.")
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

package com.example.chaskirider.feature.orders.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersUiState
import java.util.Locale

data class OrderActions(
    val onAccept: () -> Unit,
    val onReject: () -> Unit,
    val onArriveMerchant: () -> Unit,
    val onReportNotReady: () -> Unit,
    val onPickup: () -> Unit,
    val onDeliver: () -> Unit,
    val onClearEvidencePhoto: () -> Unit,
    val onTakeEvidencePhoto: () -> Unit,
    val onUpdateDeliveryNote: (String) -> Unit,
    val onUpdatePin: (String) -> Unit,
    val onReset: () -> Unit,
)

@Composable
internal fun OrderActionControls(
    state: OrdersUiState,
    actionsEnabled: Boolean,
    actions: OrderActions,
) {
    val snapshot = state.snapshot
    val offer = snapshot?.offer
    val quote = offer?.quote ?: snapshot?.batch

    if (offer != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = actions.onReject,
                enabled = actionsEnabled && state.secondsLeft > 0 && state.isAvailable,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.orders_reject)) }

            Button(
                onClick = actions.onAccept,
                enabled = actionsEnabled && state.secondsLeft > 0 && state.isAvailable,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.orders_accept)) }
        }
    } else if (quote != null) {
        val isCompleted = snapshot?.isCompleted == true
        val isPickedUp = snapshot?.pickedUp == true

        if (isCompleted) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        if (snapshot?.completedUnsuccessfullyAt != null) "Completado · Entrega no exitosa · Cliente ausente"
                        else "El pedido fue entregado al cliente.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (snapshot?.completedUnsuccessfullyAt != null)
                            "Recibes el pago de la ruta sin propina. Puedes continuar repartiendo."
                        else "Listo para recibir otro pedido.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = actions.onReset,
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

            if (snapshot?.paidByCustomer == true) {
                Text("Pedido pagado · Solicita al cliente su PIN de 4 dígitos")
                if (!state.sync.online) Text(stringResource(R.string.orders_pin_connection), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = state.deliveryPin, onValueChange = actions.onUpdatePin,
                    label = { Text("PIN del cliente") }, singleLine = true, enabled = actionsEnabled,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
            } else Text("")

            // nota opcional para verificacion de entrega
            OutlinedTextField(
                value = state.deliveryNote ?: "",
                onValueChange = actions.onUpdateDeliveryNote,
                label = { Text("Nota de entrega (opcional)") },
                placeholder = { Text("Ej: Dejado en recepción") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = actions.onDeliver,
                enabled = actionsEnabled && state.isNearCustomer && (snapshot?.paidByCustomer != true || (state.deliveryPin.length == 4 && state.sync.online)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmar entrega")
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
                    onClick = actions.onArriveMerchant,
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
                        onClick = actions.onReportNotReady,
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
                    onClick = actions.onPickup,
                    enabled = actionsEnabled && state.isWithinGeofence,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar recojo")
                }
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

// HU06 - Parte 3: pantalla "Nueva oferta" (estilo profile: tarjetas blancas).
// Muestra recogida, entrega, ganancia, distancia, la razón real de asignación
// (criterio HU06) y la cuenta regresiva de expiración. Acciones: Aceptar
// (naranja) y Rechazar (outlined); sin botón de volver: hay que decidir.
// Al aceptar/rechazar o expirar, AppNavigation regresa a la pantalla anterior.
package com.example.chaskirider.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.domain.model.RideOffer
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.DangerRed
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Oferta de pedido", showBackground = true, showSystemUi = true)
@Composable
fun OfferScreenPreview() {
    ChaskiRiderTheme {
        OfferScreen(
            state = OrdersUiState(
                status = OrderUiStatus.OFFER_ACTIVE,
                offer = RideOffer(
                    id = "1",
                    pickupAddress = "Av. Larco 120, Miraflores",
                    destinationAddress = "Av. La Marina 2000, San Miguel",
                    fareSoles = 14.5,
                    distanceKm = 3.4,
                    reason = "Estás a 1.2 km de la recogida y tu motocicleta es compatible con este pedido."
                ),
                secondsLeft = 18
            )
        )
    }
}

@Composable
fun OfferScreen(
    state: OrdersUiState,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    val offer = state.offer ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Nueva oferta de pedido",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Revisa los detalles antes de decidir.",
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ruta: recogida y entrega
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            AddressRow(label = "Recogida", address = offer.pickupAddress, color = Orange)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(12.dp))
            AddressRow(label = "Entrega", address = offer.destinationAddress, color = TextDark)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ganancia y distancia
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(title = "Ganancia", value = "S/ ${"%.2f".format(offer.fareSoles)}",
                valueColor = Orange, modifier = Modifier.weight(1f))
            InfoCard(title = "Distancia", value = "${offer.distanceKm} km",
                valueColor = TextDark, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Razón real de asignación (criterio HU06)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Orange.copy(alpha = 0.19f), RoundedCornerShape(16.dp))
                .background(Orange.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(text = "¿Por qué te la ofrecemos?", fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = offer.reason, fontSize = 14.sp, color = TextMuted, lineHeight = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cuenta regresiva
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "La oferta expira en ${state.secondsLeft} s",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.secondsLeft <= 10) DangerRed else TextDark
            )
        }

        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAccept,
            enabled = !state.isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Aceptar pedido", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReject,
            enabled = !state.isLoading,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Rechazar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AddressRow(label: String, address: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Place, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = TextMuted)
            Text(text = address, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(text = title, fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

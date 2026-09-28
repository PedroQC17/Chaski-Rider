
package com.example.chaskirider.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
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
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Pedidos - Vacío", showBackground = true, showSystemUi = true)
@Composable
fun OrdersEmptyPreview() {
    ChaskiRiderTheme { OrdersScreen(state = OrdersUiState()) }
}

@Preview(name = "Pedidos - Aceptado", showBackground = true, showSystemUi = true)
@Composable
fun OrdersAcceptedPreview() {
    ChaskiRiderTheme {
        OrdersScreen(
            state = OrdersUiState(
                status = OrderUiStatus.ACCEPTED,
                acceptedOffer = RideOffer(
                    id = "1",
                    pickupAddress = "Av. Larco 120, Miraflores",
                    destinationAddress = "Av. La Marina 2000, San Miguel",
                    fareSoles = 14.5,
                    distanceKm = 3.4
                )
            )
        )
    }
}

@Composable
fun OrdersScreen(state: OrdersUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Pedidos",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        val accepted = state.acceptedOffer
        if (state.status == OrderUiStatus.ACCEPTED && accepted != null) {
            AcceptedOrderCard(offer = accepted, message = state.message)
        } else {
            EmptyOrders()
        }
    }
}

@Composable
private fun AcceptedOrderCard(offer: RideOffer, message: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(SuccessGreen.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = "Aceptado", fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = SuccessGreen)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "S/ ${"%.2f".format(offer.fareSoles)}",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
        Spacer(modifier = Modifier.height(14.dp))
        RouteLine(label = "Recogida", address = offer.pickupAddress)
        Spacer(modifier = Modifier.height(10.dp))
        RouteLine(label = "Entrega", address = offer.destinationAddress)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "${offer.distanceKm} km", fontSize = 13.sp, color = TextMuted)
        message?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = it, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun RouteLine(label: String, address: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Place, contentDescription = label,
            tint = Orange, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextMuted)
            Text(text = address, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
        }
    }
}

@Composable
private fun EmptyOrders() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(Orange.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null,
                tint = Orange, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Aún no tienes pedidos", fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Actívate como disponible en el inicio y acepta una oferta para comenzar a ganar.",
            fontSize = 14.sp, color = TextMuted, lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

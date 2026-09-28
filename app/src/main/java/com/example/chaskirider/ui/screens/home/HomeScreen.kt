// HU04 - Parte 2: pantalla Home rediseñada (reemplaza el placeholder inline).
// - Saludo "¡Hola, {nombre}!" con chip de estado Conectado/Desconectado.
// - Tarjeta de disponibilidad con slider personalizado "Desliza para estar
//   disponible" (umbral 85%; si no llega, la base regresa animada).
// - Criterio 2 (permiso de ubicación): al intentar activarse se pide
//   ACCESS_FINE_LOCATION en runtime solo si el repartidor está habilitado;
//   si se niega, se muestra el error y no se activa.
// - Se conservan los accesos a Configurar contraseña y Cerrar sesión.
// - Colores: se reutilizan los del tema (Color.kt); el tono claro de la
//   tarjeta naranja se deriva con Orange.copy(alpha) en vez de hex nuevos.
// - Permisos: también se pide POST_NOTIFICATIONS (SDK 33+) al entrar al Home
//   para que los pushes de FCM se muestren en la barra de estado (Parte 3).
// HU04 - Parte 4: campana con badge de no leídos en la esquina superior
// derecha; al tocarla abre la pantalla Notificaciones (badge se limpia).
// HU06 - Parte 4: debajo de la tarjeta de disponibilidad se muestra el estado
// del módulo de pedidos (buscando pedido con botón "Simular oferta" para el
// mock, o el pedido ya aceptado). Requiere ordersState y onSimulateOffer.
package com.example.chaskirider.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.chaskirider.data.notifications.NotificationsStore
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.ui.screens.orders.OrderUiStatus
import com.example.chaskirider.ui.screens.orders.OrdersUiState
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.DangerRed
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted
import kotlinx.coroutines.launch
import kotlin.math.roundToInt



@Preview(name = "Home - Conectado", showBackground = true, showSystemUi = true)
@Composable
fun HomeConnectedPreview() {
    ChaskiRiderTheme {
        HomeScreen(
            user = RiderUser(
                id = "123",
                name = "Pedro",
                lastName = "Quincho Cordova",
                status = RegistrationStatus.APPROVED,
                isEnabled = true,
                isAvailable = true
            )
        )
    }
}

@Composable
fun HomeScreen(
    user: RiderUser,
    onAvailabilityChange: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
    onConfigurePassword: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    ordersState: OrdersUiState = OrdersUiState(),
    onSimulateOffer: () -> Unit = {}
) {
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onAvailabilityChange(true)
        else onError("Se requiere el permiso de ubicación para activarte como disponible.")
    }
    // HU04 - Parte 3: permiso de notificaciones para mostrar los pushes de FCM.
    val notificationsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Criterio HU04: al activarse se pide ubicación; quien no esté habilitado
    // recibe el error del validador sin llegar a pedir el permiso.
    fun toggleAvailability() {
        if (user.isAvailable) { onAvailabilityChange(false); return }
        val enabledRider = user.status == RegistrationStatus.APPROVED && user.isEnabled
        if (!enabledRider) { onAvailabilityChange(true); return }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) onAvailabilityChange(true)
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val fullName = "${user.name} ${user.lastName}".trim().ifBlank { "Repartidor" }
    val firstName = fullName.substringBefore(" ")
    val unreadCount by NotificationsStore.unreadCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Hola, $firstName!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (user.isAvailable) SuccessGreen else BorderLight, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (user.isAvailable) "Estás conectado" else "Estás desconectado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (user.isAvailable) SuccessGreen else TextMuted
                    )
                }
            }
            NotificationsBell(count = unreadCount, onClick = onNotificationsClick)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Orange.copy(alpha = 0.19f), RoundedCornerShape(16.dp))
                .background(Orange.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Conéctate para recibir pedidos en tu zona",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Activa tu disponibilidad y empieza a ganar.",
                fontSize = 14.sp,
                color = TextMuted,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            AvailabilitySlider(
                available = user.isAvailable,
                enabled = !isLoading,
                onToggle = { toggleAvailability() }
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        // HU06 - Parte 4: estado del módulo de pedidos.
        if (user.isAvailable || ordersState.status == OrderUiStatus.ACCEPTED) {
            Spacer(modifier = Modifier.height(16.dp))
            OrdersHomeCard(state = ordersState, onSimulateOffer = onSimulateOffer)
        }

        Spacer(modifier = Modifier.height(48.dp))

        TextButton(onClick = onConfigurePassword) { Text("Configurar contraseña") }
        TextButton(onClick = onLogout) { Text("Cerrar sesión", color = DangerRed) }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AvailabilitySlider(
    available: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val thumbSize = 44.dp
    val scope = rememberCoroutineScope()
    var innerWidth by remember { mutableIntStateOf(0) }
    val maxOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
        (innerWidth - thumbSize.toPx()).coerceAtLeast(0f)
    }
    // onDrag no es suspend: la posición se guarda en un float normal y el
    // regreso animado a la base se ejecuta en un Job cancelable.
    var dragPx by remember { mutableFloatStateOf(0f) }
    var snapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val trackColor = if (available) SuccessGreen else Orange

    LaunchedEffect(available) { snapJob?.cancel(); dragPx = 0f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .onSizeChanged { innerWidth = it.width }
                .pointerInput(available, enabled, maxOffset) {
                    detectDragGestures(
                        onDragStart = { snapJob?.cancel() },
                        onDrag = { change, amount ->
                            change.consume()
                            dragPx = (dragPx + amount.x).coerceIn(0f, maxOffset)
                        },
                        onDragEnd = {
                            if (enabled && maxOffset > 0f && dragPx >= maxOffset * 0.85f) {
                                onToggle()
                            }
                            snapJob = scope.launch {
                                animate(
                                    initialValue = dragPx,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { value, _ -> dragPx = value }
                            }
                        }
                    )
                }
        ) {
            Text(
                text = if (available) "Desliza para desconectar" else "Desliza para estar disponible",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 56.dp, end = 12.dp)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(dragPx.roundToInt(), 0) }
                    .size(thumbSize)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (available) "Desconectar" else "Conectar",
                    tint = trackColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// HU04 - Parte 4: campana con badge de no leídos (9+ si hay más de 9).
@Composable
private fun NotificationsBell(count: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notificaciones",
            tint = TextDark,
            modifier = Modifier
                .size(26.dp)
                .clickable { onClick() }
        )
        if (count > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 12.dp, y = (-6).dp)
                    .size(16.dp)
                    .background(DangerRed, CircleShape)
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// HU06 - Parte 4: tarjeta de pedidos en Home (buscando con mock, o aceptado).
@Composable
private fun OrdersHomeCard(state: OrdersUiState, onSimulateOffer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (state.status == OrderUiStatus.ACCEPTED && state.acceptedOffer != null) {
            val offer = state.acceptedOffer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(SuccessGreen.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(text = "Pedido aceptado", fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "S/ ${"%.2f".format(offer.fareSoles)}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${offer.pickupAddress} → ${offer.destinationAddress}",
                fontSize = 14.sp, color = TextDark, lineHeight = 19.sp
            )
            state.message?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = Orange,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Buscando pedidos cerca de ti…",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            }
            state.message?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
            }
            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DangerRed)
            }
            TextButton(
                onClick = onSimulateOffer,
                enabled = state.status == OrderUiStatus.SEARCHING
            ) {
                Text(text = "Simular oferta", color = Orange, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

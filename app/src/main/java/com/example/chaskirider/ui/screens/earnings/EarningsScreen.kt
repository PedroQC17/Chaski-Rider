package com.example.chaskirider.ui.screens.earnings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.chaskirider.domain.earnings.*
import com.example.chaskirider.ui.screens.earnings.components.EarningsChartCard
import com.example.chaskirider.ui.screens.earnings.components.ActivitySummaryCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun money(cents: Number?) = cents?.let { String.format(Locale.forLanguageTag("es-PE"), "S/ %.2f", it.toDouble() / 100) } ?: "No disponible"
private fun date(time: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE")).apply {
    timeZone = EarningsCalendar.zone
}.format(Date(time))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsRoute(viewModel: EarningsViewModel, onMenu: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRange by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Mis ganancias", fontWeight = FontWeight.Bold) }, navigationIcon = {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menú") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.selectedTab) {
                listOf("Ingresos", "Liquidaciones").forEachIndexed { index, label ->
                    Tab(selected = state.selectedTab == index, onClick = { viewModel.selectTab(index) }, text = { Text(label) })
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.warning?.let { message ->
                Text(message, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = viewModel::refresh, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Actualizar") }
            if (state.selectedTab == 0) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("earnings-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(EarningsDateFilter.TODAY to "Hoy", EarningsDateFilter.THIS_WEEK to "Esta semana", EarningsDateFilter.THIS_MONTH to "Este mes").forEach { (filter, label) ->
                                FilterChip(selected = state.selectedFilter == filter, onClick = { viewModel.selectFilter(filter) }, label = { Text(label) })
                            }
                            FilterChip(selected = state.selectedFilter == EarningsDateFilter.CUSTOM, onClick = { showRange = true }, label = { Text("Elegir fechas") })
                        }
                        if (state.selectedFilter == EarningsDateFilter.CUSTOM) state.range?.let { (from, until) ->
                            Text("${EarningsCalendar.day(from)} — ${EarningsCalendar.day(until)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Total generado", style = MaterialTheme.typography.labelLarge)
                                Text(money(state.grandTotalCents), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                HorizontalDivider()
                                AmountLine("Pago Chaski", money(state.chaskiTotalCents))
                                AmountLine("Propinas completas", money(state.tipsTotalCents))
                                if (state.unknownCount > 0) Text("${state.unknownCount} pedidos antiguos sin importe verificable; no se incluyen en el total.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { ActivitySummaryCard(state.activity) }
                    item { EarningsChartCard(state.chartPoints) }
                    item { Text("Historial de pedidos (${state.records.size})", fontWeight = FontWeight.Bold) }
                    if (state.records.isEmpty() && !state.isLoading) item { Text("Aún no hay pedidos finalizados en estas fechas.") }
                    items(state.records, key = { it.orderId }) { record ->
                        Card(Modifier.fillMaxWidth().clickable { viewModel.selectOrder(record) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(record.merchantName, fontWeight = FontWeight.Bold)
                                Text("${if (record.cancelled) "Cancelado" else "Entregado"} · ${date(record.completedAt)}", style = MaterialTheme.typography.bodySmall)
                                Text(money(record.totalCents), style = MaterialTheme.typography.titleMedium)
                                if ((record.tipCents ?: 0) > 0) Text("Incluye ${money(record.tipCents)} de propina", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("Periodos de lunes a domingo · hora de Lima", style = MaterialTheme.typography.bodySmall) }
                    item { Text("Un periodo pendiente no confirma una transferencia. El pago se confirma cuando el administrador lo registra.", style = MaterialTheme.typography.bodySmall) }
                    if (state.settlements.isEmpty() && !state.isLoading) item { Text("Aún no hay liquidaciones registradas.") }
                    items(state.settlements, key = { it.id }) { settlement ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(settlement.periodLabel, fontWeight = FontWeight.Bold)
                                Text(money(settlement.amountCents), style = MaterialTheme.typography.titleLarge)
                                Text(if (settlement.status == SettlementStatus.PAID) "Pagado" else "Pendiente de confirmación", fontWeight = FontWeight.Bold)
                                settlement.paidAt?.let { Text("Confirmado: ${date(it)}", style = MaterialTheme.typography.bodySmall) }
                                Text("${settlement.orderIds.size} pedidos", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
    state.selectedOrder?.let { order ->
        AlertDialog(onDismissRequest = { viewModel.selectOrder(null) }, title = { Text("Detalle de ganancias") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(order.merchantName, fontWeight = FontWeight.Bold)
                Text(date(order.completedAt))
                if (order.cancelled) Text("Pedido cancelado: no genera pago ni propina.")
                else order.quote?.let { quote ->
                    Text("Distancias estimadas", fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.forLanguageTag("es-PE"), "Hasta el local: %.2f km", quote.approachMeters / 1000.0))
                    Text(String.format(Locale.forLanguageTag("es-PE"), "Local al cliente: %.2f km", quote.deliveryMeters / 1000.0))
                    Text(if (quote.pricingVersion == "PEN_1_KM_V1") "S/1 por km entre ambos trayectos. Sin bonos adicionales." else "Cotización histórica conservada.")
                }
                if (order.totalCents == null) Text("Este pedido antiguo no tiene una cotización guardada. Su importe requiere revisión; no se ha estimado nuevamente.")
                HorizontalDivider()
                AmountLine("Pago Chaski", money(order.baseFareCents))
                AmountLine("Propina del cliente", money(order.tipCents))
                AmountLine("Total", money(order.totalCents))
            }
        }, confirmButton = { TextButton(onClick = { viewModel.selectOrder(null) }) { Text("Cerrar") } })
    }
    if (showRange) {
        val picker = rememberDateRangePickerState()
        DatePickerDialog(onDismissRequest = { showRange = false }, confirmButton = {
            TextButton(enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null, onClick = {
                val utcDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                viewModel.selectRange(utcDate.format(Date(picker.selectedStartDateMillis!!)), utcDate.format(Date(picker.selectedEndDateMillis!!)))
                showRange = false
            }) { Text("Aplicar") }
        }, dismissButton = { TextButton(onClick = { showRange = false }) { Text("Cancelar") } }) {
            DateRangePicker(state = picker, modifier = Modifier.height(480.dp), title = { Text("Selecciona el periodo", Modifier.padding(16.dp)) })
        }
    }
}

@Composable
private fun AmountLine(label: String, amount: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Text(amount, fontWeight = FontWeight.Bold)
    }
}

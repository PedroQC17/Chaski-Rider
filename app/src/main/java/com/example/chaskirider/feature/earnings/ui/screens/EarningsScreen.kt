package com.example.chaskirider.feature.earnings.ui.screens

import com.example.chaskirider.feature.earnings.ui.viewmodel.EarningsViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.chaskirider.feature.earnings.domain.*
import com.example.chaskirider.feature.earnings.ui.components.EarningsChartCard
import com.example.chaskirider.feature.earnings.ui.components.ActivitySummaryCard
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
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        while (true) {
            viewModel.updateTime()
            kotlinx.coroutines.delay(30_000)
        }
    }
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

            if (state.selectedTab == 0) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("earnings-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(EarningsDateFilter.TODAY to "Hoy", EarningsDateFilter.THIS_WEEK to "Esta semana", EarningsDateFilter.THIS_MONTH to "Este mes").forEach { (filter, label) ->
                                FilterChip(selected = state.selectedFilter == filter, onClick = { viewModel.selectFilter(filter) }, label = { Text(label) })
                            }
                        }
                        OutlinedButton(onClick = { showRange = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.DateRange, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Filtrar por fechas")
                        }
                        if (state.selectedFilter == EarningsDateFilter.CUSTOM) state.range?.let { (from, until) ->
                            Text("${EarningsCalendar.day(from)} — ${EarningsCalendar.day(until)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (state.unknownCount > 0) "Total confirmado" else "Total generado", style = MaterialTheme.typography.labelLarge)
                                Text(money(state.grandTotalCents), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                HorizontalDivider()
                                AmountLine("Pago Chaski", money(state.chaskiTotalCents))
                                AmountLine("Propinas", money(state.tipsTotalCents))
                            }
                        }
                    }
                    item { ActivitySummaryCard(state.activity, state.connection, state.connectionWarning) }
                    item { EarningsChartCard(state.chartPoints) }
                    item { Text("Historial de pedidos (${state.records.size})", fontWeight = FontWeight.Bold) }
                    if (state.records.isEmpty() && !state.isLoading) item { Text("Aún no hay pedidos finalizados en estas fechas.") }
                    items(state.records, key = { it.orderId }) { record ->
                        Card(Modifier.fillMaxWidth().clickable { viewModel.selectOrder(record) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(record.merchantName, fontWeight = FontWeight.Bold)
                                Text("${if (record.cancelled) "Cancelado" else if (record.unsuccessful) "Completado · Entrega no exitosa" else "Entrega exitosa"} · ${date(record.completedAt)}", style = MaterialTheme.typography.bodySmall)
                                Text(money(record.totalCents), style = MaterialTheme.typography.titleMedium)
                                if ((record.tipCents ?: 0) > 0) Text("Incluye ${money(record.tipCents)} de propina", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("Liquidaciones semanales", style = MaterialTheme.typography.bodySmall) }
                    
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
            Column(Modifier.heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.6f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(order.merchantName, fontWeight = FontWeight.Bold)
                Text(date(order.completedAt))
                if (order.cancelled) Text("Pedido cancelado: no genera pago ni propina.")
                if (order.unsuccessful) Text("Completado con entrega no exitosa · Cliente ausente. Recibes el pago de la ruta sin propina.")
                if (!order.cancelled) order.quote?.let { quote ->
                    Text("Distancias estimadas", fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.forLanguageTag("es-PE"), "Hasta el local: %.2f km", quote.approachMeters / 1000.0))
                    Text(String.format(Locale.forLanguageTag("es-PE"), "Local al cliente: %.2f km", quote.deliveryMeters / 1000.0))
                    
                }
                if (order.totalCents == null) Text("Importe pendiente de revisión.")
                HorizontalDivider()
                AmountLine("Pago Chaski", money(order.baseFareCents))
                AmountLine("Propina del cliente", money(order.tipCents))
                AmountLine("Total", money(order.totalCents))
            }
        }, confirmButton = { TextButton(onClick = { viewModel.selectOrder(null) }) { Text("Cerrar") } })
    }
    if (showRange) EarningsDateFilterDialog(state.range, { showRange = false }) { start, end ->
        viewModel.selectRange(start, end)
        showRange = false
    }
}

@Composable
private fun AmountLine(label: String, amount: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Text(amount, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun EarningsDateFilterDialog(range: Pair<Long, Long>?, onClose: () -> Unit, onApply: (String, String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var start by remember { mutableStateOf(range?.first ?: EarningsCalendar.bounds(EarningsDateFilter.THIS_WEEK, System.currentTimeMillis()).first) }
    var end by remember { mutableStateOf(range?.second ?: System.currentTimeMillis()) }
    fun choose(value: Long, change: (Long) -> Unit) {
        val calendar = java.util.Calendar.getInstance(EarningsCalendar.zone).apply { timeInMillis = value }
        android.app.DatePickerDialog(context, { _, year, month, day ->
            val selected = java.util.Calendar.getInstance(EarningsCalendar.zone).apply {
                clear()
                set(year, month, day)
            }
            change(selected.timeInMillis)
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }
    AlertDialog(onDismissRequest = onClose, title = { Text("Filtrar por fechas") }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { choose(start) { start = it } }, modifier = Modifier.fillMaxWidth()) { Text("Desde · " + EarningsCalendar.day(start)) }
            OutlinedButton(onClick = { choose(end) { end = it } }, modifier = Modifier.fillMaxWidth()) { Text("Hasta · " + EarningsCalendar.day(end)) }
            if (start > end) Text("La fecha final debe ser posterior a la inicial.", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = { onApply(EarningsCalendar.day(start), EarningsCalendar.day(end)) }, enabled = start <= end) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } })
}

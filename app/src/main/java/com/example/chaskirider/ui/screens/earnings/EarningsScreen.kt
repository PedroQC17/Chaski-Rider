package com.example.chaskirider.ui.screens.earnings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chaskirider.domain.earnings.*
import com.example.chaskirider.ui.screens.earnings.components.EarningsChartCard
import com.example.chaskirider.ui.theme.Orange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EarningsRoute(
    viewModel: EarningsViewModel,
    onMenu: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    EarningsScreen(
        state = state,
        onMenu = onMenu,
        onSelectTab = viewModel::selectTab,
        onSelectFilter = viewModel::selectFilter,
        onSelectOrder = viewModel::selectOrder
    )

    state.selectedOrder?.let { order ->
        OrderEarningsDetailDialog(
            order = order,
            onDismiss = { viewModel.selectOrder(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EarningsScreen(
    state: EarningsUiState,
    onMenu: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectFilter: (EarningsDateFilter) -> Unit,
    onSelectOrder: (DeliveryEarningsRecord) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Ganancias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text("Resumen de Ingresos", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    text = { Text("Liquidaciones", fontWeight = FontWeight.Bold) }
                )
            }

            if (state.selectedTab == 0) {
                EarningsTabContent(
                    state = state,
                    onSelectFilter = onSelectFilter,
                    onSelectOrder = onSelectOrder
                )
            } else {
                SettlementsTabContent(settlements = state.settlements)
            }
        }
    }
}

@Composable
private fun EarningsTabContent(
    state: EarningsUiState,
    onSelectFilter: (EarningsDateFilter) -> Unit,
    onSelectOrder: (DeliveryEarningsRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == EarningsDateFilter.TODAY,
                    onClick = { onSelectFilter(EarningsDateFilter.TODAY) },
                    label = { Text("Hoy") }
                )
                FilterChip(
                    selected = state.selectedFilter == EarningsDateFilter.THIS_WEEK,
                    onClick = { onSelectFilter(EarningsDateFilter.THIS_WEEK) },
                    label = { Text("Esta Semana") }
                )
                FilterChip(
                    selected = state.selectedFilter == EarningsDateFilter.THIS_MONTH,
                    onClick = { onSelectFilter(EarningsDateFilter.THIS_MONTH) },
                    label = { Text("Este Mes") }
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total Generado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "S/ %.2f", state.grandTotalCents / 100.0),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pago Base Chaski", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = String.format(Locale.getDefault(), "S/ %.2f", state.chaskiTotalCents / 100.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Propinas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = String.format(Locale.getDefault(), "S/ %.2f", state.tipsTotalCents / 100.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            EarningsChartCard(records = state.records)
        }

        item {
            Text(
                text = "Pedidos Entregados (${state.records.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(state.records) { record ->
            DeliveryRecordCard(record = record, onClick = { onSelectOrder(record) })
        }
    }
}

@Composable
private fun DeliveryRecordCard(
    record: DeliveryEarningsRecord,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val shortOrderId = if (record.orderId.length > 12) record.orderId.take(8).uppercase() else record.orderId

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.merchantName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$shortOrderId • ${dateFormat.format(Date(record.completedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.getDefault(), "S/ %.2f", record.totalCents / 100.0),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (record.tipCents > 0) {
                    Text(
                        text = String.format(Locale.getDefault(), "+ S/ %.2f propina", record.tipCents / 100.0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementsTabContent(settlements: List<SettlementRecord>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Historial de Liquidaciones Semanales",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(settlements) { settlement ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(settlement.periodLabel, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (settlement.status == SettlementStatus.PAID) "Pagado a cuenta final 4321" else "Pendiente de transferencia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.getDefault(), "S/ %.2f", settlement.amountCents / 100.0),
                            fontWeight = FontWeight.Bold
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = if (settlement.status == SettlementStatus.PAID) "PAGADO" else "PENDIENTE",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderEarningsDetailDialog(
    order: DeliveryEarningsRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Desglose de Pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(order.merchantName, fontWeight = FontWeight.Bold)
                Text("Código de pedido: ${order.orderId}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tarifa Base Chaski:")
                    Text(String.format(Locale.getDefault(), "S/ %.2f", order.baseFareCents / 100.0))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Propina del Cliente:", color = MaterialTheme.colorScheme.primary)
                    Text(String.format(Locale.getDefault(), "S/ %.2f", order.tipCents / 100.0), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Generado:", fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.getDefault(), "S/ %.2f", order.totalCents / 100.0), fontWeight = FontWeight.Bold, color = Orange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

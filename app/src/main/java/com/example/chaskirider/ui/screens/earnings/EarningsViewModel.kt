package com.example.chaskirider.ui.screens.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.earnings.*
import com.example.chaskirider.domain.repository.RiderSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EarningsViewModel(
    private val repository: EarningsRepository,
    private val session: RiderSession,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val mutableState = MutableStateFlow(EarningsUiState())
    val uiState = mutableState.asStateFlow()
    private val refreshes = MutableStateFlow(0)
    private var snapshot = EarningsSnapshot()

    init {
        viewModelScope.launch {
            session.user.map { it?.id }.distinctUntilChanged().collectLatest { uid ->
                snapshot = EarningsSnapshot()
                mutableState.value = EarningsUiState(isLoading = uid != null)
                if (uid != null) refreshes.collectLatest {
                    try {
                        repository.observe(uid).collect { data ->
                            snapshot = data
                            render()
                        }
                    } catch (error: CancellationException) { throw error }
                    catch (_: Exception) { mutableState.update { it.copy(isLoading = false, warning = "No se pudo cargar el historial. Reintenta.") } }
                }
            }
        }
    }
    fun selectTab(tabIndex: Int) { mutableState.update { it.copy(selectedTab = tabIndex.coerceIn(0, 1)) } }
    fun selectFilter(filter: EarningsDateFilter) {
        if (filter == EarningsDateFilter.CUSTOM && mutableState.value.range == null) return
        mutableState.update { it.copy(selectedFilter = filter, selectedOrder = null) }
        render()
    }
    fun selectRange(start: String, end: String) {
        val from = EarningsCalendar.parseDay(start) ?: return
        val until = EarningsCalendar.parseDay(end) ?: return
        if (from > until) return
        mutableState.update { it.copy(range = from to until, selectedFilter = EarningsDateFilter.CUSTOM, selectedOrder = null) }
        render()
    }
    fun selectOrder(order: DeliveryEarningsRecord?) { mutableState.update { it.copy(selectedOrder = order) } }
    fun refresh() { refreshes.update { it + 1 }; render() }
    private fun render() {
        mutableState.update { state ->
            val (from, until) = EarningsCalendar.bounds(state.selectedFilter, clock(), state.range)
            val records = snapshot.records.filter { it.completedAt >= from && it.completedAt < until && it.completedAt <= clock() }
            state.copy(records = records, settlements = snapshot.settlements, warning = snapshot.warning,
                selectedOrder = state.selectedOrder?.let { selected -> records.find { it.orderId == selected.orderId } }, isLoading = false)
        }
    }
}

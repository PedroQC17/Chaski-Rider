package com.example.chaskirider.ui.screens.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.earnings.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para controlar pestañas, filtros y desglose de pedidos.
 */
class EarningsViewModel(
    private val repository: EarningsRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(EarningsUiState())
    val uiState = mutableState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tabIndex: Int) {
        mutableState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectFilter(filter: EarningsDateFilter) {
        mutableState.update { it.copy(selectedFilter = filter) }
        loadEarnings()
    }

    fun selectOrder(order: DeliveryEarningsRecord?) {
        mutableState.update { it.copy(selectedOrder = order) }
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            val settlements = repository.getSettlements()
            mutableState.update { it.copy(settlements = settlements) }
            loadEarnings()
        }
    }

    private fun loadEarnings() {
        viewModelScope.launch {
            val records = repository.getEarnings(mutableState.value.selectedFilter)
            mutableState.update { it.copy(records = records, isLoading = false) }
        }
    }
}

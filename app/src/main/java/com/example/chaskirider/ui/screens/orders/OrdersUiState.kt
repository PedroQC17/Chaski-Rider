package com.example.chaskirider.ui.screens.orders

import com.example.chaskirider.domain.orders.OfferSnapshot

data class OrdersUiState(val snapshot: OfferSnapshot? = null, val busy: Boolean = false,
    val secondsLeft: Int = 0, val error: Int? = null, val scheduling: Boolean = false)

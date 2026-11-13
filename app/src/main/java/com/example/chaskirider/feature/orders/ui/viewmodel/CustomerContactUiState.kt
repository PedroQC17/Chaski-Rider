package com.example.chaskirider.feature.orders.ui.viewmodel

import com.example.chaskirider.feature.orders.domain.CustomerContact

data class CustomerContactUiState(
    val contact: CustomerContact? = null,
    val waitedMillis: Long = 0,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null
)

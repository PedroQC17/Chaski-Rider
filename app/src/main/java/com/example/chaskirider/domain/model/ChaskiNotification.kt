package com.example.chaskirider.domain.model

data class ChaskiNotification(
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

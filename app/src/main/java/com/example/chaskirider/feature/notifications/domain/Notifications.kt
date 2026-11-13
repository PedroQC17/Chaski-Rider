package com.example.chaskirider.feature.notifications.domain

data class ChaskiNotification(
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

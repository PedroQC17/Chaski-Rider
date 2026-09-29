package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.RiderUser
import kotlinx.coroutines.flow.StateFlow

interface RiderSession {
    val user: StateFlow<RiderUser?>
}

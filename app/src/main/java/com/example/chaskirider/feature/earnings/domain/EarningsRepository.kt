package com.example.chaskirider.feature.earnings.domain

import kotlinx.coroutines.flow.Flow

interface EarningsRepository {
    fun observe(uid: String): Flow<EarningsSnapshot>
}

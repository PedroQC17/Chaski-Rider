package com.example.chaskirider.domain.earnings

import kotlinx.coroutines.flow.Flow

interface EarningsRepository {
    fun observe(uid: String): Flow<EarningsSnapshot>
}

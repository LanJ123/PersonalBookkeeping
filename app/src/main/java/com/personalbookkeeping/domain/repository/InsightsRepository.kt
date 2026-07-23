package com.personalbookkeeping.domain.repository

import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlyInsights
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun observeInsights(period: MonthPeriod): Flow<MonthlyInsights>
    suspend fun setBudget(period: MonthPeriod, categoryId: String?, amount: Money)
    suspend fun clearBudget(period: MonthPeriod, categoryId: String?)
}

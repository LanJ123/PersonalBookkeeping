package com.personalbookkeeping.domain.repository

import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.HomeOverview
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlyInsights
import com.personalbookkeeping.domain.model.StatisticsInsights
import com.personalbookkeeping.domain.model.StatisticsPeriod
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun observeHome(period: MonthPeriod, todayEpochDay: Long): Flow<HomeOverview>
    fun observeInsights(period: MonthPeriod): Flow<MonthlyInsights>
    fun observeStatistics(period: StatisticsPeriod): Flow<StatisticsInsights>
    suspend fun setBudget(period: MonthPeriod, categoryId: String?, amount: Money)
    suspend fun clearBudget(period: MonthPeriod, categoryId: String?)
}

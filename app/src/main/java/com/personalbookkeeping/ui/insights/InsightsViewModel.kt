package com.personalbookkeeping.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.common.MoneyParseResult
import com.personalbookkeeping.common.MoneyParser
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlyInsights
import com.personalbookkeeping.domain.model.MonthlySummary
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.StatisticsGranularity
import com.personalbookkeeping.domain.model.StatisticsInsights
import com.personalbookkeeping.domain.model.StatisticsPeriod
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.InsightsRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val period: MonthPeriod,
    val insights: MonthlyInsights = MonthlyInsights.empty(period),
    val isLoading: Boolean = true,
    val message: String? = null,
)

data class HomeUiState(
    val period: MonthPeriod,
    val summary: MonthlySummary = MonthlyInsights.empty(period).summary,
    val todayExpense: Money = Money.fromMinor(0),
    val transactions: List<RecentTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

data class StatisticsUiState(
    val period: StatisticsPeriod,
    val insights: StatisticsInsights = StatisticsInsights.empty(period),
    val type: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = true,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val repository: InsightsRepository,
    initialDate: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val homeDate = initialDate
    private val homePeriod = MonthPeriod.from(initialDate)
    private val selectedPeriod = MutableStateFlow(homePeriod)
    private val selectedStatisticsPeriod = MutableStateFlow(
        StatisticsPeriod.from(StatisticsGranularity.MONTH, initialDate),
    )
    private val mutableState = MutableStateFlow(InsightsUiState(homePeriod))
    val state: StateFlow<InsightsUiState> = mutableState.asStateFlow()
    private val mutableHomeState = MutableStateFlow(HomeUiState(homePeriod))
    val homeState: StateFlow<HomeUiState> = mutableHomeState.asStateFlow()
    private val mutableStatisticsState = MutableStateFlow(
        StatisticsUiState(selectedStatisticsPeriod.value),
    )
    val statisticsState: StateFlow<StatisticsUiState> = mutableStatisticsState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedPeriod.flatMapLatest { period ->
                mutableState.update { it.copy(period = period, isLoading = true) }
                repository.observeInsights(period).catch {
                    mutableState.update { state ->
                        state.copy(isLoading = false, message = "月度数据加载失败，请重试")
                    }
                }
            }.collect { insights ->
                mutableState.update { it.copy(period = insights.period, insights = insights, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.observeHome(homePeriod, homeDate.toEpochDay()).catch {
                mutableHomeState.update {
                    it.copy(isLoading = false, message = "首页数据加载失败，请重试")
                }
            }.collect { overview ->
                mutableHomeState.update {
                    it.copy(
                        period = homePeriod,
                        summary = overview.summary,
                        todayExpense = overview.todayExpense,
                        transactions = overview.transactions,
                        isLoading = false,
                        message = null,
                    )
                }
            }
        }
        viewModelScope.launch {
            selectedStatisticsPeriod.flatMapLatest { period ->
                mutableStatisticsState.update { it.copy(period = period, isLoading = true, message = null) }
                repository.observeStatistics(period).catch {
                    mutableStatisticsState.update {
                        it.copy(isLoading = false, message = "统计数据加载失败，请重试")
                    }
                }
            }.collect { insights ->
                mutableStatisticsState.update {
                    it.copy(
                        period = insights.period,
                        insights = insights,
                        isLoading = false,
                        message = null,
                    )
                }
            }
        }
    }

    fun previousMonth() = select(selectedPeriod.value.previous())
    fun nextMonth() = select(selectedPeriod.value.next())
    fun select(period: MonthPeriod) {
        selectedPeriod.value = period
    }

    fun selectStatisticsGranularity(granularity: StatisticsGranularity) {
        selectedStatisticsPeriod.value = selectedStatisticsPeriod.value.withGranularity(granularity)
    }

    fun selectStatisticsType(type: TransactionType) {
        if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
            mutableStatisticsState.update { it.copy(type = type) }
        }
    }

    fun previousStatisticsPeriod() {
        selectedStatisticsPeriod.value = selectedStatisticsPeriod.value.previous()
    }

    fun nextStatisticsPeriod() {
        selectedStatisticsPeriod.value = selectedStatisticsPeriod.value.next()
    }

    fun saveBudget(categoryId: String?, amountText: String): Boolean {
        val parsed = MoneyParser.parsePositive(amountText)
        if (parsed is MoneyParseResult.Failure) {
            mutableState.update { it.copy(message = parsed.reason.budgetMessage()) }
            return false
        }
        val amount = (parsed as MoneyParseResult.Success).money
        val period = selectedPeriod.value
        viewModelScope.launch {
            runCatching { repository.setBudget(period, categoryId, amount) }
                .onSuccess { mutableState.update { it.copy(message = "预算已保存") } }
                .onFailure { mutableState.update { it.copy(message = "预算保存失败，请重试") } }
        }
        return true
    }

    fun clearBudget(categoryId: String?) {
        val period = selectedPeriod.value
        viewModelScope.launch {
            runCatching { repository.clearBudget(period, categoryId) }
                .onSuccess { mutableState.update { it.copy(message = "预算已清除") } }
                .onFailure { mutableState.update { it.copy(message = "预算清除失败，请重试") } }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun MoneyParseFailure.budgetMessage(): String = when (this) {
        MoneyParseFailure.EMPTY -> "请输入预算金额"
        MoneyParseFailure.NON_POSITIVE -> "预算必须大于 0"
        MoneyParseFailure.TOO_MANY_FRACTION_DIGITS -> "预算最多保留两位小数"
        MoneyParseFailure.ABOVE_MAXIMUM -> "预算金额超出上限"
        MoneyParseFailure.INVALID_FORMAT -> "预算金额格式不正确"
    }
}

class InsightsViewModelFactory(
    private val repository: InsightsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InsightsViewModel::class.java))
        return InsightsViewModel(repository) as T
    }
}

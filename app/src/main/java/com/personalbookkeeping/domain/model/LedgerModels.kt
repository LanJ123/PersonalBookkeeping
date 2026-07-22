package com.personalbookkeeping.domain.model

import com.personalbookkeeping.common.Money
import java.time.Instant
import java.time.ZoneId

data class TransactionFilter(
    val noteQuery: String = "",
    val type: TransactionType? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val fromEpochDay: Long? = null,
    val toEpochDay: Long? = null,
) {
    init {
        require(fromEpochDay == null || toEpochDay == null || fromEpochDay <= toEpochDay) {
            "开始日期不能晚于结束日期"
        }
    }

    val isActive: Boolean
        get() = noteQuery.isNotBlank() || type != null || accountId != null ||
            categoryId != null || fromEpochDay != null || toEpochDay != null
}

data class LedgerTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val categoryId: String?,
    val categoryName: String?,
    val accountId: String,
    val accountName: String,
    val targetAccountId: String?,
    val targetAccountName: String?,
    val occurredAt: Instant,
    val zoneId: ZoneId,
    val localDateEpochDay: Long,
    val note: String?,
    val dailyExpense: Money,
    val dailyIncome: Money,
)

data class TransactionRecord(
    val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amount: Money,
    val categoryId: String?,
    val categoryName: String?,
    val accountId: String,
    val accountName: String,
    val targetAccountId: String?,
    val targetAccountName: String?,
    val occurredAt: Instant,
    val zoneId: ZoneId,
    val localDateEpochDay: Long,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LedgerFilterOptions(
    val accounts: List<AccountOption>,
    val categories: List<CategoryOption>,
)

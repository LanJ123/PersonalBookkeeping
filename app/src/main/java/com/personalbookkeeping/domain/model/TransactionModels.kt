package com.personalbookkeeping.domain.model

import com.personalbookkeeping.common.Money
import java.time.Instant
import java.time.ZoneId

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
}

enum class CategoryKind {
    EXPENSE,
    INCOME,
}

data class AccountOption(
    val id: String,
    val name: String,
)

data class CategoryOption(
    val id: String,
    val name: String,
    val kind: CategoryKind,
)

data class EditorOptions(
    val accounts: List<AccountOption>,
    val categories: List<CategoryOption>,
)

data class NewTransaction(
    val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amount: Money,
    val categoryId: String?,
    val accountId: String,
    val targetAccountId: String?,
    val occurredAt: Instant,
    val zoneId: ZoneId,
    val localDateEpochDay: Long,
    val note: String?,
    val createdAt: Instant,
)

data class RecentTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val categoryName: String?,
    val accountName: String,
    val targetAccountName: String?,
    val occurredAt: Instant,
)

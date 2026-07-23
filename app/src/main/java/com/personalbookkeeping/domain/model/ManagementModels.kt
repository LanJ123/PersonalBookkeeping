package com.personalbookkeeping.domain.model

import com.personalbookkeeping.common.Money

enum class ItemStatus { ACTIVE, INACTIVE }

enum class AccountType {
    CASH,
    BANK,
    E_WALLET,
    STORED_VALUE,
    CREDIT_CARD,
    OTHER;

    companion object {
        fun fromStoredValue(value: String): AccountType = when (value) {
            "CREDIT" -> CREDIT_CARD
            "INVESTMENT" -> OTHER
            else -> valueOf(value)
        }
    }
}

data class ManagedAccount(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
    val balance: Money,
    val includeInAssets: Boolean,
    val status: ItemStatus,
    val sortOrder: Int,
    val transactionCount: Int,
)

data class ManagedCategory(
    val id: String,
    val kind: CategoryKind,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val status: ItemStatus,
    val sortOrder: Int,
    val transactionCount: Int,
)

enum class MoveDirection { UP, DOWN }

sealed interface ManagementResult {
    data class Success(val id: String) : ManagementResult
    data object EmptyName : ManagementResult
    data object DuplicateName : ManagementResult
    data object LastActiveItem : ManagementResult
    data object InvalidBalance : ManagementResult
    data object NotFound : ManagementResult
}

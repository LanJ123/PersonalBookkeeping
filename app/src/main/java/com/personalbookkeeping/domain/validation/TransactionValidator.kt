package com.personalbookkeeping.domain.validation

import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.TransactionType

data class TransactionDraft(
    val type: TransactionType,
    val amount: Money,
    val categoryId: String?,
    val accountId: String?,
    val targetAccountId: String?,
    val note: String?,
)

enum class TransactionValidationError {
    NON_POSITIVE_AMOUNT,
    ACCOUNT_REQUIRED,
    CATEGORY_REQUIRED,
    CATEGORY_NOT_ALLOWED,
    TARGET_ACCOUNT_REQUIRED,
    TARGET_ACCOUNT_NOT_ALLOWED,
    ACCOUNTS_MUST_DIFFER,
    NOTE_TOO_LONG,
}

object TransactionValidator {
    fun validate(draft: TransactionDraft): Set<TransactionValidationError> = buildSet {
        if (draft.amount.minorUnits <= 0) add(TransactionValidationError.NON_POSITIVE_AMOUNT)
        if (draft.accountId.isNullOrBlank()) add(TransactionValidationError.ACCOUNT_REQUIRED)
        if ((draft.note?.length ?: 0) > MAX_NOTE_LENGTH) {
            add(TransactionValidationError.NOTE_TOO_LONG)
        }

        when (draft.type) {
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            -> {
                if (draft.categoryId.isNullOrBlank()) {
                    add(TransactionValidationError.CATEGORY_REQUIRED)
                }
                if (draft.targetAccountId != null) {
                    add(TransactionValidationError.TARGET_ACCOUNT_NOT_ALLOWED)
                }
            }

            TransactionType.TRANSFER -> {
                if (draft.categoryId != null) {
                    add(TransactionValidationError.CATEGORY_NOT_ALLOWED)
                }
                if (draft.targetAccountId.isNullOrBlank()) {
                    add(TransactionValidationError.TARGET_ACCOUNT_REQUIRED)
                } else if (draft.targetAccountId == draft.accountId) {
                    add(TransactionValidationError.ACCOUNTS_MUST_DIFFER)
                }
            }
        }
    }

    const val MAX_NOTE_LENGTH = 500
}

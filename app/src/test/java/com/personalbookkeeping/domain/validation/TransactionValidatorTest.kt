package com.personalbookkeeping.domain.validation

import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidatorTest {
    @Test
    fun `expense requires category and rejects target account`() {
        val errors = TransactionValidator.validate(
            validDraft(TransactionType.EXPENSE).copy(
                categoryId = null,
                targetAccountId = "target",
            ),
        )

        assertEquals(
            setOf(
                TransactionValidationError.CATEGORY_REQUIRED,
                TransactionValidationError.TARGET_ACCOUNT_NOT_ALLOWED,
            ),
            errors,
        )
    }

    @Test
    fun `transfer requires distinct source and target and no category`() {
        val errors = TransactionValidator.validate(
            validDraft(TransactionType.TRANSFER).copy(
                categoryId = "category",
                targetAccountId = "account",
            ),
        )

        assertEquals(
            setOf(
                TransactionValidationError.CATEGORY_NOT_ALLOWED,
                TransactionValidationError.ACCOUNTS_MUST_DIFFER,
            ),
            errors,
        )
    }

    @Test
    fun `valid income expense and transfer pass`() {
        assertTrue(TransactionValidator.validate(validDraft(TransactionType.EXPENSE)).isEmpty())
        assertTrue(TransactionValidator.validate(validDraft(TransactionType.INCOME)).isEmpty())
        assertTrue(TransactionValidator.validate(validDraft(TransactionType.TRANSFER)).isEmpty())
    }

    private fun validDraft(type: TransactionType) = TransactionDraft(
        type = type,
        amount = Money.fromMinor(1),
        categoryId = if (type == TransactionType.TRANSFER) null else "category",
        accountId = "account",
        targetAccountId = if (type == TransactionType.TRANSFER) "target" else null,
        note = null,
    )
}

package com.personalbookkeeping.domain.usecase

import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.common.MoneyParseResult
import com.personalbookkeeping.common.MoneyParser
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.LedgerRepository
import com.personalbookkeeping.domain.validation.TransactionDraft
import com.personalbookkeeping.domain.validation.TransactionValidationError
import com.personalbookkeeping.domain.validation.TransactionValidator

data class UpdateTransactionCommand(
    val id: String,
    val amountText: String,
    val type: TransactionType,
    val categoryId: String?,
    val accountId: String?,
    val targetAccountId: String?,
    val note: String,
)

sealed interface UpdateTransactionResult {
    data object Success : UpdateTransactionResult
    data object NotFound : UpdateTransactionResult
    data class InvalidAmount(val reason: MoneyParseFailure) : UpdateTransactionResult
    data class InvalidTransaction(val errors: Set<TransactionValidationError>) : UpdateTransactionResult
}

class UpdateTransactionUseCase(
    private val repository: LedgerRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(command: UpdateTransactionCommand): UpdateTransactionResult {
        val current = repository.getTransaction(command.id) ?: return UpdateTransactionResult.NotFound
        val amount = when (val parsed = MoneyParser.parsePositive(command.amountText)) {
            is MoneyParseResult.Failure -> return UpdateTransactionResult.InvalidAmount(parsed.reason)
            is MoneyParseResult.Success -> parsed.money
        }
        val categoryId = command.categoryId.takeUnless { command.type == TransactionType.TRANSFER }
        val targetId = command.targetAccountId.takeIf { command.type == TransactionType.TRANSFER }
        val note = command.note.trim().ifEmpty { null }
        val errors = TransactionValidator.validate(
            TransactionDraft(command.type, amount, categoryId, command.accountId, targetId, note),
        )
        if (errors.isNotEmpty()) return UpdateTransactionResult.InvalidTransaction(errors)
        repository.updateTransaction(
            current.copy(
                type = command.type,
                amount = amount,
                categoryId = categoryId,
                accountId = requireNotNull(command.accountId),
                targetAccountId = targetId,
                note = note,
                updatedAt = clock.now(),
            ),
        )
        return UpdateTransactionResult.Success
    }
}

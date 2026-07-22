package com.personalbookkeeping.domain.usecase

import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.IdGenerator
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.common.MoneyParseResult
import com.personalbookkeeping.common.MoneyParser
import com.personalbookkeeping.domain.model.NewTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.repository.TransactionRepository
import com.personalbookkeeping.domain.validation.TransactionDraft
import com.personalbookkeeping.domain.validation.TransactionValidationError
import com.personalbookkeeping.domain.validation.TransactionValidator
import java.time.ZoneId

data class CreateTransactionCommand(
    val amountText: String,
    val type: TransactionType,
    val categoryId: String?,
    val accountId: String?,
    val targetAccountId: String?,
    val note: String,
)

sealed interface CreateTransactionResult {
    data class Success(val transactionId: String) : CreateTransactionResult
    data class InvalidAmount(val reason: MoneyParseFailure) : CreateTransactionResult
    data class InvalidTransaction(
        val errors: Set<TransactionValidationError>,
    ) : CreateTransactionResult
}

class CreateTransactionUseCase(
    private val repository: TransactionRepository,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
    private val zoneIdProvider: () -> ZoneId,
) {
    suspend operator fun invoke(command: CreateTransactionCommand): CreateTransactionResult {
        val amount = when (val parsed = MoneyParser.parsePositive(command.amountText)) {
            is MoneyParseResult.Failure -> return CreateTransactionResult.InvalidAmount(parsed.reason)
            is MoneyParseResult.Success -> parsed.money
        }
        val normalizedNote = command.note.trim().ifEmpty { null }
        val categoryId = command.categoryId.takeUnless { command.type == TransactionType.TRANSFER }
        val targetAccountId = command.targetAccountId.takeIf {
            command.type == TransactionType.TRANSFER
        }
        val draft = TransactionDraft(
            type = command.type,
            amount = amount,
            categoryId = categoryId,
            accountId = command.accountId,
            targetAccountId = targetAccountId,
            note = normalizedNote,
        )
        val errors = TransactionValidator.validate(draft)
        if (errors.isNotEmpty()) return CreateTransactionResult.InvalidTransaction(errors)

        val occurredAt = clock.now()
        val zoneId = zoneIdProvider()
        val transaction = NewTransaction(
            id = idGenerator.newId(),
            ledgerId = DEFAULT_LEDGER_ID,
            type = command.type,
            amount = amount,
            categoryId = categoryId,
            accountId = requireNotNull(command.accountId),
            targetAccountId = targetAccountId,
            occurredAt = occurredAt,
            zoneId = zoneId,
            localDateEpochDay = occurredAt.atZone(zoneId).toLocalDate().toEpochDay(),
            note = normalizedNote,
            createdAt = occurredAt,
        )
        return CreateTransactionResult.Success(repository.create(transaction))
    }

    companion object {
        const val DEFAULT_LEDGER_ID = "00000000-0000-0000-0000-000000000001"
    }
}

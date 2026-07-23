package com.personalbookkeeping.data.repository

import androidx.room.withTransaction
import com.personalbookkeeping.common.AppClock
import com.personalbookkeeping.common.IdGenerator
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.common.MoneyParseResult
import com.personalbookkeeping.common.MoneyParser
import com.personalbookkeeping.common.NameNormalizer
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.entity.AccountEntity
import com.personalbookkeeping.database.entity.CategoryEntity
import com.personalbookkeeping.domain.model.AccountType
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.ItemStatus
import com.personalbookkeeping.domain.model.ManagedAccount
import com.personalbookkeeping.domain.model.ManagedCategory
import com.personalbookkeeping.domain.model.ManagementResult
import com.personalbookkeeping.domain.model.MoveDirection
import com.personalbookkeeping.domain.repository.ManagementRepository
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineManagementRepository(
    private val database: AppDatabase,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
) : ManagementRepository {
    private val dao get() = database.managementDao()
    private val ledgerId get() = CreateTransactionUseCase.DEFAULT_LEDGER_ID

    override fun observeAccounts(): Flow<List<ManagedAccount>> =
        dao.observeAccounts(ledgerId).map { rows ->
            rows.map {
                ManagedAccount(
                    id = it.id,
                    name = it.name,
                    type = AccountType.fromStoredValue(it.type),
                    openingBalance = Money.fromMinor(it.openingBalanceMinor),
                    balance = Money.fromMinor(it.balanceMinor),
                    includeInAssets = it.includeInAssets,
                    status = ItemStatus.valueOf(it.status),
                    sortOrder = it.sortOrder,
                    transactionCount = it.transactionCount,
                )
            }
        }

    override fun observeCategories(kind: CategoryKind): Flow<List<ManagedCategory>> =
        dao.observeCategories(ledgerId, kind.name).map { rows ->
            rows.map {
                ManagedCategory(
                    id = it.id,
                    kind = CategoryKind.valueOf(it.kind),
                    name = it.name,
                    iconKey = it.iconKey,
                    colorKey = it.colorKey,
                    status = ItemStatus.valueOf(it.status),
                    sortOrder = it.sortOrder,
                    transactionCount = it.transactionCount,
                )
            }
        }

    override suspend fun saveAccount(
        id: String?,
        name: String,
        type: AccountType,
        openingBalanceText: String,
        includeInAssets: Boolean,
    ): ManagementResult = database.withTransaction {
        val displayName = NameNormalizer.displayName(name)
        val key = NameNormalizer.activeKey(name)
        if (displayName.isEmpty() || key.isEmpty()) return@withTransaction ManagementResult.EmptyName
        val openingBalance = when (val parsed = MoneyParser.parseSigned(openingBalanceText)) {
            is MoneyParseResult.Failure -> return@withTransaction ManagementResult.InvalidBalance
            is MoneyParseResult.Success -> parsed.money
        }
        val excludingId = id.orEmpty()
        if (dao.countAccountName(ledgerId, key, excludingId) > 0) {
            return@withTransaction ManagementResult.DuplicateName
        }
        val now = clock.now().toEpochMilli()
        val existing = id?.let { dao.getAccount(it) }
        if (id != null && existing == null) return@withTransaction ManagementResult.NotFound
        val entity = existing?.copy(
            name = displayName,
            activeNameKey = key,
            type = type.name,
            openingBalanceMinor = openingBalance.minorUnits,
            includeInAssets = includeInAssets,
            updatedAtMs = now,
        ) ?: AccountEntity(
            id = idGenerator.newId(),
            ledgerId = ledgerId,
            name = displayName,
            activeNameKey = key,
            type = type.name,
            openingBalanceMinor = openingBalance.minorUnits,
            includeInAssets = includeInAssets,
            status = ItemStatus.ACTIVE.name,
            sortOrder = dao.maxAccountSort(ledgerId) + 1,
            createdAtMs = now,
            updatedAtMs = now,
        )
        if (existing == null) dao.insertAccount(entity) else dao.updateAccount(entity)
        ManagementResult.Success(entity.id)
    }

    override suspend fun deactivateAccount(id: String): ManagementResult = database.withTransaction {
        val existing = dao.getAccount(id) ?: return@withTransaction ManagementResult.NotFound
        if (existing.status != ItemStatus.ACTIVE.name) return@withTransaction ManagementResult.Success(id)
        if (dao.activeAccounts(ledgerId).size <= 1) return@withTransaction ManagementResult.LastActiveItem
        dao.updateAccount(
            existing.copy(
                activeNameKey = null,
                status = ItemStatus.INACTIVE.name,
                updatedAtMs = clock.now().toEpochMilli(),
            ),
        )
        ManagementResult.Success(id)
    }

    override suspend fun moveAccount(id: String, direction: MoveDirection): ManagementResult =
        database.withTransaction {
            val items = dao.activeAccounts(ledgerId)
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) return@withTransaction ManagementResult.NotFound
            val targetIndex = index + if (direction == MoveDirection.UP) -1 else 1
            if (targetIndex !in items.indices) return@withTransaction ManagementResult.Success(id)
            swapAccounts(items[index], items[targetIndex])
            ManagementResult.Success(id)
        }

    override suspend fun saveCategory(
        id: String?,
        kind: CategoryKind,
        name: String,
    ): ManagementResult = database.withTransaction {
        val displayName = NameNormalizer.displayName(name)
        val key = NameNormalizer.activeKey(name)
        if (displayName.isEmpty() || key.isEmpty()) return@withTransaction ManagementResult.EmptyName
        val excludingId = id.orEmpty()
        if (dao.countCategoryName(ledgerId, kind.name, key, excludingId) > 0) {
            return@withTransaction ManagementResult.DuplicateName
        }
        val now = clock.now().toEpochMilli()
        val existing = id?.let { dao.getCategory(it) }
        if (id != null && existing == null) return@withTransaction ManagementResult.NotFound
        val entity = existing?.copy(
            name = displayName,
            activeNameKey = key,
            updatedAtMs = now,
        ) ?: CategoryEntity(
            id = idGenerator.newId(),
            ledgerId = ledgerId,
            kind = kind.name,
            name = displayName,
            activeNameKey = key,
            iconKey = "custom",
            colorKey = "DEFAULT_${(dao.maxCategorySort(ledgerId, kind.name) + 1) % 6}",
            status = ItemStatus.ACTIVE.name,
            sortOrder = dao.maxCategorySort(ledgerId, kind.name) + 1,
            createdAtMs = now,
            updatedAtMs = now,
        )
        if (existing == null) dao.insertCategory(entity) else dao.updateCategory(entity)
        ManagementResult.Success(entity.id)
    }

    override suspend fun deactivateCategory(id: String): ManagementResult = database.withTransaction {
        val existing = dao.getCategory(id) ?: return@withTransaction ManagementResult.NotFound
        if (existing.status != ItemStatus.ACTIVE.name) return@withTransaction ManagementResult.Success(id)
        if (dao.activeCategories(ledgerId, existing.kind).size <= 1) {
            return@withTransaction ManagementResult.LastActiveItem
        }
        dao.updateCategory(
            existing.copy(
                activeNameKey = null,
                status = ItemStatus.INACTIVE.name,
                updatedAtMs = clock.now().toEpochMilli(),
            ),
        )
        ManagementResult.Success(id)
    }

    override suspend fun moveCategory(id: String, direction: MoveDirection): ManagementResult =
        database.withTransaction {
            val current = dao.getCategory(id) ?: return@withTransaction ManagementResult.NotFound
            val items = dao.activeCategories(ledgerId, current.kind)
            val index = items.indexOfFirst { it.id == id }
            val targetIndex = index + if (direction == MoveDirection.UP) -1 else 1
            if (index < 0) return@withTransaction ManagementResult.NotFound
            if (targetIndex !in items.indices) return@withTransaction ManagementResult.Success(id)
            swapCategories(items[index], items[targetIndex])
            ManagementResult.Success(id)
        }

    private suspend fun swapAccounts(first: AccountEntity, second: AccountEntity) {
        val now = clock.now().toEpochMilli()
        dao.updateAccount(first.copy(sortOrder = second.sortOrder, updatedAtMs = now))
        dao.updateAccount(second.copy(sortOrder = first.sortOrder, updatedAtMs = now))
    }

    private suspend fun swapCategories(first: CategoryEntity, second: CategoryEntity) {
        val now = clock.now().toEpochMilli()
        dao.updateCategory(first.copy(sortOrder = second.sortOrder, updatedAtMs = now))
        dao.updateCategory(second.copy(sortOrder = first.sortOrder, updatedAtMs = now))
    }
}

package com.personalbookkeeping.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personalbookkeeping.database.entity.AccountEntity
import com.personalbookkeeping.database.entity.AppPreferencesEntity
import com.personalbookkeeping.database.entity.BudgetEntity
import com.personalbookkeeping.database.entity.CategoryEntity
import com.personalbookkeeping.database.entity.LedgerEntity
import com.personalbookkeeping.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class AccountOptionRow(
    val id: String,
    val name: String,
)

data class CategoryOptionRow(
    val id: String,
    val name: String,
    val kind: String,
)

data class RecentTransactionRow(
    val id: String,
    val type: String,
    @androidx.room.ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @androidx.room.ColumnInfo(name = "category_name") val categoryName: String?,
    @androidx.room.ColumnInfo(name = "account_name") val accountName: String,
    @androidx.room.ColumnInfo(name = "target_account_name") val targetAccountName: String?,
    @androidx.room.ColumnInfo(name = "occurred_at_ms") val occurredAtMs: Long,
)

@Dao
interface SeedDao {
    @Query("SELECT COUNT(*) FROM ledgers")
    suspend fun ledgerCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLedger(ledger: LedgerEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPreferences(preferences: AppPreferencesEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)
}

@Dao
interface OptionDao {
    @Query("SELECT id, name FROM accounts WHERE status = 'ACTIVE' ORDER BY sort_order, name")
    fun observeActiveAccounts(): Flow<List<AccountOptionRow>>

    @Query("SELECT id, name, kind FROM categories WHERE status = 'ACTIVE' ORDER BY kind, sort_order, name")
    fun observeActiveCategories(): Flow<List<CategoryOptionRow>>
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Query(
        """
        SELECT t.id,
               t.type,
               t.amount_minor,
               c.name AS category_name,
               a.name AS account_name,
               ta.name AS target_account_name,
               t.occurred_at_ms
        FROM transactions AS t
        LEFT JOIN categories AS c ON c.id = t.category_id
        JOIN accounts AS a ON a.id = t.account_id
        LEFT JOIN accounts AS ta ON ta.id = t.target_account_id
        ORDER BY t.occurred_at_ms DESC, t.created_at_ms DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<RecentTransactionRow>>

    @Query("SELECT balance_minor FROM account_balances WHERE account_id = :accountId")
    suspend fun getAccountBalanceMinor(accountId: String): Long?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

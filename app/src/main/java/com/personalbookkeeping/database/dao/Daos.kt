package com.personalbookkeeping.database.dao

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.paging.PagingSource
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

data class LedgerTransactionRow(
    val id: String,
    val type: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "target_account_id") val targetAccountId: String?,
    @ColumnInfo(name = "target_account_name") val targetAccountName: String?,
    @ColumnInfo(name = "occurred_at_ms") val occurredAtMs: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "local_date_epoch_day") val localDateEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "daily_expense_minor") val dailyExpenseMinor: Long,
    @ColumnInfo(name = "daily_income_minor") val dailyIncomeMinor: Long,
)

data class TransactionDetailRow(
    val id: String,
    @ColumnInfo(name = "ledger_id") val ledgerId: String,
    val type: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "target_account_id") val targetAccountId: String?,
    @ColumnInfo(name = "target_account_name") val targetAccountName: String?,
    @ColumnInfo(name = "occurred_at_ms") val occurredAtMs: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "local_date_epoch_day") val localDateEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

data class ManagedAccountRow(
    val id: String,
    val name: String,
    val type: String,
    @ColumnInfo(name = "opening_balance_minor") val openingBalanceMinor: Long,
    @ColumnInfo(name = "balance_minor") val balanceMinor: Long,
    @ColumnInfo(name = "include_in_assets") val includeInAssets: Boolean,
    val status: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
)

data class ManagedCategoryRow(
    val id: String,
    val kind: String,
    val name: String,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_key") val colorKey: String,
    val status: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
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

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        WITH filtered_transactions AS (
            SELECT t.*
            FROM transactions AS t
            WHERE t.ledger_id = :ledgerId
              AND (:notePattern = '' OR COALESCE(t.note, '') LIKE '%' || :notePattern || '%' ESCAPE '\')
              AND (:type IS NULL OR t.type = :type)
              AND (:accountId IS NULL OR t.account_id = :accountId OR t.target_account_id = :accountId)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:fromEpochDay IS NULL OR t.local_date_epoch_day >= :fromEpochDay)
              AND (:toEpochDay IS NULL OR t.local_date_epoch_day <= :toEpochDay)
        ),
        daily_totals AS (
            SELECT local_date_epoch_day,
                   COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_minor ELSE 0 END), 0) AS daily_expense_minor,
                   COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount_minor ELSE 0 END), 0) AS daily_income_minor
            FROM filtered_transactions
            GROUP BY local_date_epoch_day
        )
        SELECT t.id,
               t.type,
               t.amount_minor,
               t.category_id,
               c.name AS category_name,
               t.account_id,
               a.name AS account_name,
               t.target_account_id,
               ta.name AS target_account_name,
               t.occurred_at_ms,
               t.zone_id,
               t.local_date_epoch_day,
               t.note,
               totals.daily_expense_minor,
               totals.daily_income_minor
        FROM filtered_transactions AS t
        LEFT JOIN categories AS c ON c.id = t.category_id
        JOIN accounts AS a ON a.id = t.account_id
        LEFT JOIN accounts AS ta ON ta.id = t.target_account_id
        JOIN daily_totals AS totals ON totals.local_date_epoch_day = t.local_date_epoch_day
        ORDER BY t.local_date_epoch_day DESC, t.occurred_at_ms DESC, t.created_at_ms DESC
        """,
    )
    fun pagingSource(
        ledgerId: String,
        notePattern: String,
        type: String?,
        accountId: String?,
        categoryId: String?,
        fromEpochDay: Long?,
        toEpochDay: Long?,
    ): PagingSource<Int, LedgerTransactionRow>

    @Query(
        """
        SELECT t.id,
               t.ledger_id,
               t.type,
               t.amount_minor,
               t.category_id,
               c.name AS category_name,
               t.account_id,
               a.name AS account_name,
               t.target_account_id,
               ta.name AS target_account_name,
               t.occurred_at_ms,
               t.zone_id,
               t.local_date_epoch_day,
               t.note,
               t.created_at_ms,
               t.updated_at_ms
        FROM transactions AS t
        LEFT JOIN categories AS c ON c.id = t.category_id
        JOIN accounts AS a ON a.id = t.account_id
        LEFT JOIN accounts AS ta ON ta.id = t.target_account_id
        WHERE t.id = :id
        """,
    )
    fun observeById(id: String): Flow<TransactionDetailRow?>

    @Query(
        """
        SELECT t.id,
               t.ledger_id,
               t.type,
               t.amount_minor,
               t.category_id,
               c.name AS category_name,
               t.account_id,
               a.name AS account_name,
               t.target_account_id,
               ta.name AS target_account_name,
               t.occurred_at_ms,
               t.zone_id,
               t.local_date_epoch_day,
               t.note,
               t.created_at_ms,
               t.updated_at_ms
        FROM transactions AS t
        LEFT JOIN categories AS c ON c.id = t.category_id
        JOIN accounts AS a ON a.id = t.account_id
        LEFT JOIN accounts AS ta ON ta.id = t.target_account_id
        WHERE t.id = :id
        """,
    )
    suspend fun getById(id: String): TransactionDetailRow?

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

@Dao
interface ManagementDao {
    @Query(
        """
        SELECT a.id,
               a.name,
               a.type,
               a.opening_balance_minor,
               b.balance_minor,
               a.include_in_assets,
               a.status,
               a.sort_order,
               (SELECT COUNT(*) FROM transactions t
                WHERE t.account_id = a.id OR t.target_account_id = a.id) AS transaction_count
        FROM accounts a
        JOIN account_balances b ON b.account_id = a.id
        WHERE a.ledger_id = :ledgerId
        ORDER BY CASE a.status WHEN 'ACTIVE' THEN 0 ELSE 1 END, a.sort_order, a.name
        """,
    )
    fun observeAccounts(ledgerId: String): Flow<List<ManagedAccountRow>>

    @Query(
        """
        SELECT c.id,
               c.kind,
               c.name,
               c.icon_key,
               c.color_key,
               c.status,
               c.sort_order,
               (SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.id) AS transaction_count
        FROM categories c
        WHERE c.ledger_id = :ledgerId AND c.kind = :kind
        ORDER BY CASE c.status WHEN 'ACTIVE' THEN 0 ELSE 1 END, c.sort_order, c.name
        """,
    )
    fun observeCategories(ledgerId: String, kind: String): Flow<List<ManagedCategoryRow>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): AccountEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: String): CategoryEntity?

    @Query("SELECT * FROM accounts WHERE ledger_id = :ledgerId AND status = 'ACTIVE' ORDER BY sort_order, name")
    suspend fun activeAccounts(ledgerId: String): List<AccountEntity>

    @Query("SELECT * FROM categories WHERE ledger_id = :ledgerId AND kind = :kind AND status = 'ACTIVE' ORDER BY sort_order, name")
    suspend fun activeCategories(ledgerId: String, kind: String): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM accounts WHERE ledger_id = :ledgerId AND status = 'ACTIVE' AND active_name_key = :key AND id != :excludingId")
    suspend fun countAccountName(ledgerId: String, key: String, excludingId: String): Int

    @Query("SELECT COUNT(*) FROM categories WHERE ledger_id = :ledgerId AND kind = :kind AND status = 'ACTIVE' AND active_name_key = :key AND id != :excludingId")
    suspend fun countCategoryName(ledgerId: String, kind: String, key: String, excludingId: String): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM accounts WHERE ledger_id = :ledgerId AND status = 'ACTIVE'")
    suspend fun maxAccountSort(ledgerId: String): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM categories WHERE ledger_id = :ledgerId AND kind = :kind AND status = 'ACTIVE'")
    suspend fun maxCategorySort(ledgerId: String, kind: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)
}

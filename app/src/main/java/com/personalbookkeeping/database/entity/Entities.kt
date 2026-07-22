package com.personalbookkeeping.database.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ledgers")
data class LedgerEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "month_start_day") val monthStartDay: Int,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["ledger_id", "active_name_key"],
            unique = true,
            name = "index_accounts_active_name",
        ),
    ],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ledger_id") val ledgerId: String,
    val name: String,
    @ColumnInfo(name = "active_name_key") val activeNameKey: String?,
    val type: String,
    @ColumnInfo(name = "opening_balance_minor") val openingBalanceMinor: Long,
    @ColumnInfo(name = "include_in_assets") val includeInAssets: Boolean,
    val status: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["ledger_id", "kind", "active_name_key"],
            unique = true,
            name = "index_categories_active_name",
        ),
    ],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ledger_id") val ledgerId: String,
    val kind: String,
    val name: String,
    @ColumnInfo(name = "active_name_key") val activeNameKey: String?,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_key") val colorKey: String,
    val status: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["target_account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(
            value = ["ledger_id", "local_date_epoch_day", "occurred_at_ms"],
            name = "index_transactions_period",
        ),
        Index(
            value = ["account_id", "local_date_epoch_day"],
            name = "index_transactions_account",
        ),
        Index(
            value = ["target_account_id", "local_date_epoch_day"],
            name = "index_transactions_target_account",
        ),
        Index(
            value = ["category_id", "local_date_epoch_day"],
            name = "index_transactions_category",
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ledger_id") val ledgerId: String,
    val type: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "target_account_id") val targetAccountId: String?,
    @ColumnInfo(name = "occurred_at_ms") val occurredAtMs: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "local_date_epoch_day") val localDateEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(
            value = ["ledger_id", "period_key", "scope_key"],
            unique = true,
            name = "index_budgets_scope",
        ),
        Index(value = ["category_id"], name = "index_budgets_category_id"),
    ],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ledger_id") val ledgerId: String,
    @ColumnInfo(name = "period_key") val periodKey: String,
    @ColumnInfo(name = "scope_key") val scopeKey: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@Entity(
    tableName = "app_preferences",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["recent_expense_account_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["recent_income_account_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["recent_expense_account_id"], name = "index_preferences_expense_account"),
        Index(value = ["recent_income_account_id"], name = "index_preferences_income_account"),
    ],
)
data class AppPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "ledger_id")
    val ledgerId: String,
    @ColumnInfo(name = "theme_mode") val themeMode: String,
    @ColumnInfo(name = "hide_amounts") val hideAmounts: Boolean,
    @ColumnInfo(name = "recent_expense_account_id") val recentExpenseAccountId: String?,
    @ColumnInfo(name = "recent_income_account_id") val recentIncomeAccountId: String?,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@DatabaseView(
    viewName = "account_transaction_deltas",
    value = """
        SELECT account_id AS account_id,
               CASE type WHEN 'INCOME' THEN amount_minor ELSE -amount_minor END AS delta_minor
        FROM transactions
        WHERE type IN ('INCOME', 'EXPENSE')
        UNION ALL
        SELECT account_id AS account_id, -amount_minor AS delta_minor
        FROM transactions
        WHERE type = 'TRANSFER'
        UNION ALL
        SELECT target_account_id AS account_id, amount_minor AS delta_minor
        FROM transactions
        WHERE type = 'TRANSFER'
    """,
)
data class AccountTransactionDeltaView(
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "delta_minor") val deltaMinor: Long,
)

@DatabaseView(
    viewName = "account_balances",
    value = """
        SELECT a.id AS account_id,
               a.opening_balance_minor + COALESCE(SUM(d.delta_minor), 0) AS balance_minor
        FROM accounts AS a
        LEFT JOIN account_transaction_deltas AS d ON d.account_id = a.id
        GROUP BY a.id, a.opening_balance_minor
    """,
)
data class AccountBalanceView(
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "balance_minor") val balanceMinor: Long,
)

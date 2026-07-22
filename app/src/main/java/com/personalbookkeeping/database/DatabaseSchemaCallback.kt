package com.personalbookkeeping.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

internal object DatabaseSchemaCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(VALIDATE_TRANSACTION_INSERT)
        db.execSQL(VALIDATE_TRANSACTION_UPDATE)
        db.execSQL(VALIDATE_BUDGET_INSERT)
        db.execSQL(VALIDATE_BUDGET_UPDATE)
    }

    private const val VALIDATE_TRANSACTION_INSERT = """
        CREATE TRIGGER IF NOT EXISTS validate_transaction_insert
        BEFORE INSERT ON transactions
        WHEN NEW.amount_minor <= 0
          OR length(COALESCE(NEW.note, '')) > 500
          OR NOT (
              (NEW.type IN ('EXPENSE', 'INCOME') AND NEW.category_id IS NOT NULL
               AND NEW.target_account_id IS NULL)
              OR
              (NEW.type = 'TRANSFER' AND NEW.category_id IS NULL
               AND NEW.target_account_id IS NOT NULL
               AND NEW.account_id <> NEW.target_account_id)
          )
        BEGIN
            SELECT RAISE(ABORT, 'invalid transaction');
        END
    """

    private const val VALIDATE_TRANSACTION_UPDATE = """
        CREATE TRIGGER IF NOT EXISTS validate_transaction_update
        BEFORE UPDATE ON transactions
        WHEN NEW.amount_minor <= 0
          OR length(COALESCE(NEW.note, '')) > 500
          OR NOT (
              (NEW.type IN ('EXPENSE', 'INCOME') AND NEW.category_id IS NOT NULL
               AND NEW.target_account_id IS NULL)
              OR
              (NEW.type = 'TRANSFER' AND NEW.category_id IS NULL
               AND NEW.target_account_id IS NOT NULL
               AND NEW.account_id <> NEW.target_account_id)
          )
        BEGIN
            SELECT RAISE(ABORT, 'invalid transaction');
        END
    """

    private const val VALIDATE_BUDGET_INSERT = """
        CREATE TRIGGER IF NOT EXISTS validate_budget_insert
        BEFORE INSERT ON budgets
        WHEN NEW.amount_minor <= 0
          OR NOT (
              (NEW.scope_key = 'TOTAL' AND NEW.category_id IS NULL)
              OR
              (NEW.category_id IS NOT NULL AND NEW.scope_key = 'CATEGORY:' || NEW.category_id)
          )
        BEGIN
            SELECT RAISE(ABORT, 'invalid budget');
        END
    """

    private const val VALIDATE_BUDGET_UPDATE = """
        CREATE TRIGGER IF NOT EXISTS validate_budget_update
        BEFORE UPDATE ON budgets
        WHEN NEW.amount_minor <= 0
          OR NOT (
              (NEW.scope_key = 'TOTAL' AND NEW.category_id IS NULL)
              OR
              (NEW.category_id IS NOT NULL AND NEW.scope_key = 'CATEGORY:' || NEW.category_id)
          )
        BEGIN
            SELECT RAISE(ABORT, 'invalid budget');
        END
    """
}

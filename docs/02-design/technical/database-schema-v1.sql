-- PersonalBookkeeping schema v1 design reference.
-- Room exported schema becomes the implementation source of truth.

PRAGMA foreign_keys = ON;

CREATE TABLE ledgers (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    currency_code TEXT NOT NULL,
    month_start_day INTEGER NOT NULL,
    created_at_ms INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL
);

CREATE TABLE accounts (
    id TEXT NOT NULL PRIMARY KEY,
    ledger_id TEXT NOT NULL,
    name TEXT NOT NULL,
    active_name_key TEXT,
    type TEXT NOT NULL,
    opening_balance_minor INTEGER NOT NULL,
    include_in_assets INTEGER NOT NULL,
    status TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at_ms INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledgers(id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE UNIQUE INDEX index_accounts_active_name
ON accounts(ledger_id, active_name_key);

CREATE TABLE categories (
    id TEXT NOT NULL PRIMARY KEY,
    ledger_id TEXT NOT NULL,
    kind TEXT NOT NULL,
    name TEXT NOT NULL,
    active_name_key TEXT,
    icon_key TEXT NOT NULL,
    color_key TEXT NOT NULL,
    status TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at_ms INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledgers(id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE UNIQUE INDEX index_categories_active_name
ON categories(ledger_id, kind, active_name_key);

CREATE TABLE transactions (
    id TEXT NOT NULL PRIMARY KEY,
    ledger_id TEXT NOT NULL,
    type TEXT NOT NULL,
    amount_minor INTEGER NOT NULL,
    category_id TEXT,
    account_id TEXT NOT NULL,
    target_account_id TEXT,
    occurred_at_ms INTEGER NOT NULL,
    zone_id TEXT NOT NULL,
    local_date_epoch_day INTEGER NOT NULL,
    note TEXT,
    created_at_ms INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledgers(id) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
    FOREIGN KEY (target_account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
);

CREATE INDEX index_transactions_period
ON transactions(ledger_id, local_date_epoch_day DESC, occurred_at_ms DESC);
CREATE INDEX index_transactions_account
ON transactions(account_id, local_date_epoch_day DESC);
CREATE INDEX index_transactions_target_account
ON transactions(target_account_id, local_date_epoch_day DESC);
CREATE INDEX index_transactions_category
ON transactions(category_id, local_date_epoch_day DESC);

CREATE TABLE budgets (
    id TEXT NOT NULL PRIMARY KEY,
    ledger_id TEXT NOT NULL,
    period_key TEXT NOT NULL,
    scope_key TEXT NOT NULL,
    category_id TEXT,
    amount_minor INTEGER NOT NULL,
    created_at_ms INTEGER NOT NULL,
    updated_at_ms INTEGER NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledgers(id) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
);

CREATE UNIQUE INDEX index_budgets_scope
ON budgets(ledger_id, period_key, scope_key);

CREATE INDEX index_budgets_category_id
ON budgets(category_id);

CREATE TABLE app_preferences (
    ledger_id TEXT NOT NULL PRIMARY KEY,
    theme_mode TEXT NOT NULL,
    hide_amounts INTEGER NOT NULL,
    recent_expense_account_id TEXT,
    recent_income_account_id TEXT,
    updated_at_ms INTEGER NOT NULL,
    FOREIGN KEY (ledger_id) REFERENCES ledgers(id) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY (recent_expense_account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL,
    FOREIGN KEY (recent_income_account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE INDEX index_preferences_expense_account
ON app_preferences(recent_expense_account_id);

CREATE INDEX index_preferences_income_account
ON app_preferences(recent_income_account_id);

CREATE TRIGGER validate_transaction_insert
BEFORE INSERT ON transactions
WHEN NEW.amount_minor <= 0
  OR length(COALESCE(NEW.note, '')) > 500
  OR NOT (
      (NEW.type IN ('EXPENSE', 'INCOME') AND NEW.category_id IS NOT NULL AND NEW.target_account_id IS NULL)
      OR
      (NEW.type = 'TRANSFER' AND NEW.category_id IS NULL AND NEW.target_account_id IS NOT NULL
       AND NEW.account_id <> NEW.target_account_id)
  )
BEGIN
    SELECT RAISE(ABORT, 'invalid transaction');
END;

CREATE TRIGGER validate_transaction_update
BEFORE UPDATE ON transactions
WHEN NEW.amount_minor <= 0
  OR length(COALESCE(NEW.note, '')) > 500
  OR NOT (
      (NEW.type IN ('EXPENSE', 'INCOME') AND NEW.category_id IS NOT NULL AND NEW.target_account_id IS NULL)
      OR
      (NEW.type = 'TRANSFER' AND NEW.category_id IS NULL AND NEW.target_account_id IS NOT NULL
       AND NEW.account_id <> NEW.target_account_id)
  )
BEGIN
    SELECT RAISE(ABORT, 'invalid transaction');
END;

CREATE TRIGGER validate_budget_insert
BEFORE INSERT ON budgets
WHEN NEW.amount_minor <= 0
  OR NOT (
      (NEW.scope_key = 'TOTAL' AND NEW.category_id IS NULL)
      OR
      (NEW.category_id IS NOT NULL AND NEW.scope_key = 'CATEGORY:' || NEW.category_id)
  )
BEGIN
    SELECT RAISE(ABORT, 'invalid budget');
END;

CREATE TRIGGER validate_budget_update
BEFORE UPDATE ON budgets
WHEN NEW.amount_minor <= 0
  OR NOT (
      (NEW.scope_key = 'TOTAL' AND NEW.category_id IS NULL)
      OR
      (NEW.category_id IS NOT NULL AND NEW.scope_key = 'CATEGORY:' || NEW.category_id)
  )
BEGIN
    SELECT RAISE(ABORT, 'invalid budget');
END;

CREATE VIEW account_transaction_deltas AS
SELECT account_id AS account_id,
       CASE type WHEN 'INCOME' THEN amount_minor ELSE -amount_minor END AS delta_minor
FROM transactions
WHERE type IN ('INCOME', 'EXPENSE')
UNION ALL
SELECT account_id, -amount_minor FROM transactions WHERE type = 'TRANSFER'
UNION ALL
SELECT target_account_id, amount_minor FROM transactions WHERE type = 'TRANSFER';

CREATE VIEW account_balances AS
SELECT a.id AS account_id,
       a.opening_balance_minor + COALESCE(SUM(d.delta_minor), 0) AS balance_minor
FROM accounts a
LEFT JOIN account_transaction_deltas d ON d.account_id = a.id
GROUP BY a.id, a.opening_balance_minor;

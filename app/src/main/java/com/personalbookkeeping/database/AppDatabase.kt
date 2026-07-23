package com.personalbookkeeping.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personalbookkeeping.database.dao.OptionDao
import com.personalbookkeeping.database.dao.ManagementDao
import com.personalbookkeeping.database.dao.SeedDao
import com.personalbookkeeping.database.dao.TransactionDao
import com.personalbookkeeping.database.dao.InsightsDao
import com.personalbookkeeping.database.entity.AccountBalanceView
import com.personalbookkeeping.database.entity.AccountEntity
import com.personalbookkeeping.database.entity.AccountTransactionDeltaView
import com.personalbookkeeping.database.entity.AppPreferencesEntity
import com.personalbookkeeping.database.entity.BudgetEntity
import com.personalbookkeeping.database.entity.CategoryEntity
import com.personalbookkeeping.database.entity.LedgerEntity
import com.personalbookkeeping.database.entity.TransactionEntity

@Database(
    entities = [
        LedgerEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        AppPreferencesEntity::class,
    ],
    views = [AccountTransactionDeltaView::class, AccountBalanceView::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seedDao(): SeedDao
    abstract fun optionDao(): OptionDao
    abstract fun transactionDao(): TransactionDao
    abstract fun managementDao(): ManagementDao
    abstract fun insightsDao(): InsightsDao

    companion object {
        private const val DATABASE_NAME = "personal-bookkeeping.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(DatabaseSchemaCallback)
                .build()

        fun buildInMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .addCallback(DatabaseSchemaCallback)
                .build()
    }
}

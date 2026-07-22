package com.personalbookkeeping.app

import android.content.Context
import com.personalbookkeeping.common.SystemAppClock
import com.personalbookkeeping.common.UuidGenerator
import com.personalbookkeeping.data.repository.OfflineTransactionRepository
import com.personalbookkeeping.data.repository.OfflineManagementRepository
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.repository.TransactionRepository
import com.personalbookkeeping.domain.repository.LedgerRepository
import com.personalbookkeeping.domain.repository.ManagementRepository
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import com.personalbookkeeping.domain.usecase.UpdateTransactionUseCase
import java.time.ZoneId

class AppContainer(context: Context) {
    private val database = AppDatabase.build(context.applicationContext)
    private val initialDataSeeder = InitialDataSeeder(database, SystemAppClock)

    private val offlineTransactionRepository = OfflineTransactionRepository(
        database = database,
        initialDataSeeder = initialDataSeeder,
    )
    val transactionRepository: TransactionRepository = offlineTransactionRepository
    val ledgerRepository: LedgerRepository = offlineTransactionRepository
    val managementRepository: ManagementRepository = OfflineManagementRepository(
        database = database,
        clock = SystemAppClock,
        idGenerator = UuidGenerator,
    )

    val createTransactionUseCase = CreateTransactionUseCase(
        repository = transactionRepository,
        clock = SystemAppClock,
        idGenerator = UuidGenerator,
        zoneIdProvider = ZoneId::systemDefault,
    )

    val updateTransactionUseCase = UpdateTransactionUseCase(
        repository = ledgerRepository,
        clock = SystemAppClock,
    )
}

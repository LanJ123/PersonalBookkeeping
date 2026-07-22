package com.personalbookkeeping.app

import android.content.Context
import com.personalbookkeeping.common.SystemAppClock
import com.personalbookkeeping.common.UuidGenerator
import com.personalbookkeeping.data.repository.OfflineTransactionRepository
import com.personalbookkeeping.database.AppDatabase
import com.personalbookkeeping.database.seed.InitialDataSeeder
import com.personalbookkeeping.domain.repository.TransactionRepository
import com.personalbookkeeping.domain.usecase.CreateTransactionUseCase
import java.time.ZoneId

class AppContainer(context: Context) {
    private val database = AppDatabase.build(context.applicationContext)
    private val initialDataSeeder = InitialDataSeeder(database, SystemAppClock)

    val transactionRepository: TransactionRepository = OfflineTransactionRepository(
        database = database,
        initialDataSeeder = initialDataSeeder,
    )

    val createTransactionUseCase = CreateTransactionUseCase(
        repository = transactionRepository,
        clock = SystemAppClock,
        idGenerator = UuidGenerator,
        zoneIdProvider = ZoneId::systemDefault,
    )
}

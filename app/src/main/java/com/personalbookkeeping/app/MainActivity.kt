package com.personalbookkeeping.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import com.personalbookkeeping.ui.transaction.TransactionEditorScreen
import com.personalbookkeeping.ui.transaction.TransactionEditorViewModel
import com.personalbookkeeping.ui.transaction.TransactionEditorViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: TransactionEditorViewModel by viewModels {
        val container = (application as PersonalBookkeepingApplication).container
        TransactionEditorViewModelFactory(
            createTransaction = container.createTransactionUseCase,
            repository = container.transactionRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookkeepingTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                TransactionEditorScreen(
                    state = state,
                    onAmountChanged = viewModel::onAmountChanged,
                    onNoteChanged = viewModel::onNoteChanged,
                    onTypeSelected = viewModel::onTypeSelected,
                    onAccountSelected = viewModel::onAccountSelected,
                    onTargetAccountSelected = viewModel::onTargetAccountSelected,
                    onCategorySelected = viewModel::onCategorySelected,
                    onSave = viewModel::save,
                )
            }
        }
    }
}

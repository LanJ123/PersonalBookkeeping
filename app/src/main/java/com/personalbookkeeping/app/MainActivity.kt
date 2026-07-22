package com.personalbookkeeping.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.personalbookkeeping.ui.navigation.BookkeepingApp
import com.personalbookkeeping.ui.theme.BookkeepingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookkeepingTheme {
                BookkeepingApp((application as PersonalBookkeepingApplication).container)
            }
        }
    }
}

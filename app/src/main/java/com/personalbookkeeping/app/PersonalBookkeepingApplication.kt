package com.personalbookkeeping.app

import android.app.Application

class PersonalBookkeepingApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}

package com.personalbookkeeping.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "security-settings")

data class SecuritySettings(
    val appLockEnabled: Boolean,
    val backgroundTimeoutSeconds: Int,
)

interface AppLockSettingsStore {
    val settings: Flow<SecuritySettings>
    suspend fun setAppLockEnabled(enabled: Boolean)
}

class SecuritySettingsRepository(context: Context) : AppLockSettingsStore {
    private val dataStore = context.applicationContext.securityDataStore

    override val settings: Flow<SecuritySettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            SecuritySettings(
                appLockEnabled = preferences[APP_LOCK_ENABLED] ?: false,
                backgroundTimeoutSeconds = preferences[BACKGROUND_TIMEOUT_SECONDS] ?: DEFAULT_TIMEOUT_SECONDS,
            )
        }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
            preferences[BACKGROUND_TIMEOUT_SECONDS] = DEFAULT_TIMEOUT_SECONDS
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val BACKGROUND_TIMEOUT_SECONDS = intPreferencesKey("background_timeout_seconds")
    }
}

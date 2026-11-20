package com.example.medical_shelf.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val IS_PRO_KEY = booleanPreferencesKey("is_pro_version")
        private val FREE_SCANS_LEFT_KEY = intPreferencesKey("free_scans_left")
        private val LICENSE_KEY = stringPreferencesKey("license_key")
        private val EXPIRY_WARNING_DAYS_KEY = intPreferencesKey("expiry_warning_days")
    }

    val isProVersion: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PRO_KEY] ?: false
    }

    val freeScansLeft: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[FREE_SCANS_LEFT_KEY] ?: 3
    }

    val licenseKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LICENSE_KEY]
    }

    val expiryWarningDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[EXPIRY_WARNING_DAYS_KEY] ?: 14
    }

    suspend fun setProVersion(isPro: Boolean, key: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[IS_PRO_KEY] = isPro
            if (key != null) {
                prefs[LICENSE_KEY] = key
            }
        }
    }

    suspend fun decrementFreeScan(): Int {
        var remaining = 0
        context.dataStore.edit { prefs ->
            val current = prefs[FREE_SCANS_LEFT_KEY] ?: 3
            if (current > 0) {
                remaining = current - 1
                prefs[FREE_SCANS_LEFT_KEY] = remaining
            } else {
                remaining = 0
            }
        }
        return remaining
    }

    suspend fun setExpiryWarningDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[EXPIRY_WARNING_DAYS_KEY] = days
        }
    }
}
// utils/BeaconPreferences.kt
package com.iiitl.locateme.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beacon_prefs")

data class BeaconState(
    val isTransmitting: Boolean = false,
    val uuid: String = "",
    val major: String = "1",
    val minor: String = "1",
    val latitude: String = "",
    val longitude: String = ""
)

class BeaconPreferences(private val context: Context) {
    companion object {
        private val IS_TRANSMITTING = booleanPreferencesKey("is_transmitting")
        private val UUID = stringPreferencesKey("uuid")
        private val MAJOR = stringPreferencesKey("major")
        private val MINOR = stringPreferencesKey("minor")
        private val LATITUDE = stringPreferencesKey("latitude")
        private val LONGITUDE = stringPreferencesKey("longitude")
    }

    val beaconState: Flow<BeaconState> = context.dataStore.data.map { preferences ->
        BeaconState(
            isTransmitting = preferences[IS_TRANSMITTING] ?: false,
            uuid = preferences[UUID] ?: "",
            major = preferences[MAJOR] ?: "1",
            minor = preferences[MINOR] ?: "1",
            latitude = preferences[LATITUDE] ?: "",
            longitude = preferences[LONGITUDE] ?: ""
        )
    }

    suspend fun updateBeaconState(beaconState: BeaconState) {
        context.dataStore.edit { preferences ->
            preferences[IS_TRANSMITTING] = beaconState.isTransmitting
            preferences[UUID] = beaconState.uuid
            preferences[MAJOR] = beaconState.major
            preferences[MINOR] = beaconState.minor
            preferences[LATITUDE] = beaconState.latitude
            preferences[LONGITUDE] = beaconState.longitude
        }
    }

    suspend fun clearBeaconState() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
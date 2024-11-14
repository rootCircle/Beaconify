// utils/BeaconPreferences.kt
package com.iiitl.locateme.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.iiitl.locateme.network.models.VirtualBeacon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beacon_prefs")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val IS_TRANSMITTING = booleanPreferencesKey("is_transmitting")
        private val UUID = stringPreferencesKey("uuid")
        private val MAJOR = stringPreferencesKey("major")
        private val MINOR = stringPreferencesKey("minor")
        private val LATITUDE = stringPreferencesKey("latitude")
        private val LONGITUDE = stringPreferencesKey("longitude")
        private val IS_SERVICE_RUNNING = booleanPreferencesKey("is_service_running")
        private val CURRENT_BEACON = stringPreferencesKey("current_beacon")
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

    suspend fun markServiceRunning(isRunning: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SERVICE_RUNNING] = isRunning
        }
    }

    fun isServiceRunning(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SERVICE_RUNNING] ?: false
        }
        .catch { e ->
            Log.e("BeaconPreferences", "Error reading service state: ${e.message}")
            emit(false)
        }

    suspend fun saveCurrentBeacon(beacon: VirtualBeacon?) {
        context.dataStore.edit { preferences ->
            if (beacon != null) {
                val beaconJson = Gson().toJson(beacon)
                preferences[CURRENT_BEACON] = beaconJson
            } else {
                preferences.remove(CURRENT_BEACON)
            }
        }
    }

    fun getCurrentBeacon(): Flow<VirtualBeacon?> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENT_BEACON]?.let { json ->
                try {
                    Gson().fromJson(json, VirtualBeacon::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
        .catch { e ->
            Log.e("BeaconPreferences", "Error reading beacon data: ${e.message}")
            emit(null)
        }


    suspend fun clearBeaconState() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
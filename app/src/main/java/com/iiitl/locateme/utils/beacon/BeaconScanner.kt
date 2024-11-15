package com.iiitl.locateme.utils.beacon

import android.content.Context
import android.util.Log
import com.iiitl.locateme.network.BeaconApiService
import com.iiitl.locateme.network.models.VirtualBeacon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import java.util.concurrent.ConcurrentHashMap

data class BeaconData(
    val uuid: String,
    val major: String,
    val minor: String,
    val rssi: Int,
    val distance: Double,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class BeaconScanner(private val context: Context) {
    private val TAG = "BeaconScanner"
    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private val _scannedBeacons = MutableStateFlow<List<BeaconData>>(emptyList())
    val scannedBeacons: StateFlow<List<BeaconData>> = _scannedBeacons.asStateFlow()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val knownBeacons = ConcurrentHashMap<String, VirtualBeacon>()

    // Cache for maintaining beacon state
    private val beaconCache = mutableMapOf<String, BeaconData>()
    private val beaconExpirationTime = 10000L // 10 seconds timeout

    init {
        setupBeaconManager()
    }

    private fun setupBeaconManager() {
        // Set up beacon parser for AltBeacon format
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")
        )

        // Set up scanning periods
        beaconManager.foregroundScanPeriod = 1100L
        beaconManager.foregroundBetweenScanPeriod = 0L

        // Add range notifier
        beaconManager.addRangeNotifier { beacons, _ ->
            processBeacons(beacons)
        }
    }

    private fun processBeacons(beacons: Collection<Beacon>) {
        try {
            val currentTime = System.currentTimeMillis()

            beacons.forEach { beacon ->
                val uuid = beacon.id1.toString()
                val major = beacon.id2.toString()
                val minor = beacon.id3.toString()
                val key = getBeaconKey(uuid, major, minor)

                knownBeacons[key]?.let { virtualBeacon ->
                    val beaconData = BeaconData(
                        uuid = uuid,
                        major = major,
                        minor = minor,
                        rssi = beacon.rssi,
                        distance = beacon.distance,
                        latitude = virtualBeacon.latitude,
                        longitude = virtualBeacon.longitude,
                        timestamp = currentTime
                    )
                    beaconCache[key] = beaconData
                }
            }

            // Clean expired beacons
            cleanExpiredBeacons()

            // Emit all cached beacons
            _scannedBeacons.value = beaconCache.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing beacons: ${e.message}")
        }
    }

    private fun cleanExpiredBeacons() {
        val currentTime = System.currentTimeMillis()
        beaconCache.entries.removeAll { entry ->
            currentTime - entry.value.timestamp > beaconExpirationTime
        }
    }

    private fun getBeaconKey(uuid: String, major: String, minor: String): String {
        return "$uuid:$major:$minor"
    }

    fun startScanning() {
        coroutineScope.launch {
            try {
                updateKnownBeacons()
                // Start ranging using the new API
                val region = Region("myRangingUniqueId", null, null, null)
                beaconManager.startRangingBeacons(region)
                Log.d(TAG, "Started beacon scanning")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting beacon scanning: ${e.message}")
            }
        }
    }

    fun stopScanning() {
        try {
            val region = Region("myRangingUniqueId", null, null, null)
            beaconManager.stopRangingBeacons(region)
            beaconCache.clear()
            _scannedBeacons.value = emptyList()
            Log.d(TAG, "Stopped beacon scanning")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping beacon scanning: ${e.message}")
        }
    }

    private suspend fun updateKnownBeacons() {
        try {
            BeaconApiService.getAllBeacons()
                .onSuccess { beacons ->
                    knownBeacons.clear()
                    beacons.forEach { beacon ->
                        val key = getBeaconKey(beacon.uuid, beacon.major, beacon.minor)
                        knownBeacons[key] = beacon
                    }
                    Log.d(TAG, "Updated known beacons: ${knownBeacons.size}")
                }
                .onFailure { error ->
                    Log.e(TAG, "Error fetching beacons: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating known beacons: ${e.message}")
        }
    }

    fun cleanup() {
        coroutineScope.cancel()
        stopScanning()
    }
}

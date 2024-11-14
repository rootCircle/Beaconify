package com.iiitl.locateme.utils.beacon

import android.content.Context
import android.util.Log
import com.iiitl.locateme.network.BeaconApiService
import com.iiitl.locateme.network.models.VirtualBeacon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import java.util.concurrent.CopyOnWriteArrayList

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

class BeaconScanner(private val context: Context) : BeaconConsumer {
    private val TAG = "BeaconScanner"
    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private val _scannedBeacons = MutableStateFlow<List<BeaconData>>(emptyList())
    val scannedBeacons: StateFlow<List<BeaconData>> = _scannedBeacons.asStateFlow()

    private var isBindingInProgress = false
    private var isServiceBound = false
    private val pendingOperations = CopyOnWriteArrayList<() -> Unit>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val knownBeacons = mutableMapOf<String, VirtualBeacon>()

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
        beaconManager.foregroundScanPeriod = 1100L  // Scan for 1.1 seconds
        beaconManager.foregroundBetweenScanPeriod = 0L  // No delay between scans
    }

    override fun getApplicationContext(): Context = context.applicationContext

    override fun bindService(p0: android.content.Intent?, p1: android.content.ServiceConnection, p2: Int): Boolean {
        return true
    }

    override fun unbindService(p0: android.content.ServiceConnection) {
        // Implementation required by interface
    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "Beacon service connected")
        isServiceBound = true
        isBindingInProgress = false

        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, _ ->
            processBeacons(beacons)
        }

        // Execute any pending operations
        pendingOperations.forEach { operation ->
            operation.invoke()
        }
        pendingOperations.clear()
    }

    private fun processBeacons(beacons: Collection<Beacon>) {
        try {
            val beaconDataList = beacons.mapNotNull { beacon ->
                val uuid = beacon.id1.toString()
                val major = beacon.id2.toString()
                val minor = beacon.id3.toString()

                // Check if this is a known beacon
                knownBeacons[getBeaconKey(uuid, major, minor)]?.let { virtualBeacon ->
                    BeaconData(
                        uuid = uuid,
                        major = major,
                        minor = minor,
                        rssi = beacon.rssi,
                        distance = beacon.distance,
                        latitude = virtualBeacon.latitude,
                        longitude = virtualBeacon.longitude
                    )
                }
            }
            _scannedBeacons.value = beaconDataList
        } catch (e: Exception) {
            Log.e(TAG, "Error processing beacons: ${e.message}")
        }
    }

    private fun getBeaconKey(uuid: String, major: String, minor: String): String {
        return "$uuid:$major:$minor"
    }

    private fun ensureServiceBound(operation: () -> Unit) {
        if (isServiceBound) {
            operation.invoke()
        } else {
            pendingOperations.add(operation)
            if (!isBindingInProgress) {
                isBindingInProgress = true
                beaconManager.bind(this)
            }
        }
    }

    private suspend fun updateKnownBeacons() {
        try {
            val result = BeaconApiService.getAllBeacons()
            result.onSuccess { beacons ->
                knownBeacons.clear()
                beacons.forEach { beacon ->
                    val key = getBeaconKey(beacon.uuid, beacon.major, beacon.minor)
                    knownBeacons[key] = beacon
                }
                Log.d(TAG, "Updated known beacons: ${knownBeacons.size}")
            }.onFailure { error ->
                Log.e(TAG, "Error fetching beacons: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating known beacons: ${e.message}")
        }
    }


    fun startScanning() {
        coroutineScope.launch {
            try {
                updateKnownBeacons()
                beaconManager.bind(this@BeaconScanner)
                beaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
            } catch (e: Exception) {
                Log.e(TAG, "Error starting beacon scanning: ${e.message}")
            }
        }
    }

    fun stopScanning() {
        try {
            beaconManager.stopRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
            if (isServiceBound) {
                beaconManager.unbind(this)
                isServiceBound = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping beacon scanning: ${e.message}")
        }
    }

    fun unbind() {
        try {
            stopScanning()
            if (isServiceBound) {
                beaconManager.unbind(this)
                isServiceBound = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding from beacon service: ${e.message}")
        }
    }
}

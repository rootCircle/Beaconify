package com.iiitl.locateme.utils.beacon

import android.content.Context
import android.util.Log
import org.altbeacon.beacon.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

data class BeaconData(
    val uuid: String,
    val major: String,
    val minor: String,
    val rssi: Int,
    val latitude: Double,
    val longitude: Double,
    val distance: Double,
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
                try {
                    // Extract latitude and longitude from data fields
                    if (beacon.dataFields.size >= 2) {
                        val latitude = Double.fromBits(beacon.dataFields[0].toLong())
                        val longitude = Double.fromBits(beacon.dataFields[1].toLong())

                        BeaconData(
                            uuid = beacon.id1.toString(),
                            major = beacon.id2.toString(),
                            minor = beacon.id3.toString(),
                            rssi = beacon.rssi,
                            latitude = latitude,
                            longitude = longitude,
                            distance = beacon.distance
                        )
                    } else {
                        Log.w(TAG, "Beacon ${beacon.id1} doesn't contain location data")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing beacon ${beacon.id1}: ${e.message}")
                    null
                }
            }

            _scannedBeacons.value = beaconDataList
        } catch (e: Exception) {
            Log.e(TAG, "Error processing beacons: ${e.message}")
        }
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

    fun startScanning() {
        ensureServiceBound {
            try {
                Log.d(TAG, "Starting beacon scanning")
                beaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
            } catch (e: Exception) {
                Log.e(TAG, "Error starting beacon scanning: ${e.message}")
            }
        }
    }

    fun stopScanning() {
        if (isServiceBound) {
            try {
                Log.d(TAG, "Stopping beacon scanning")
                beaconManager.stopRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping beacon scanning: ${e.message}")
            }
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

// services/BeaconTransmitterService.kt
package com.iiitl.locateme.services

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.iiitl.locateme.MainActivity
import org.altbeacon.beacon.*
import android.util.Log
import com.iiitl.locateme.network.BeaconApiService
import com.iiitl.locateme.network.models.VirtualBeacon
import com.iiitl.locateme.utils.BeaconPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BeaconTransmitterService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "BeaconTransmitterChannel"
        private const val TAG = "BeaconTransmitterService"
        const val ACTION_SHOW_REGISTER_SCREEN = "com.iiitl.locateme.SHOW_BEACON_SCREEN"
    }

    private var beaconTransmitter: BeaconTransmitter? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var currentBeacon: VirtualBeacon? = null
    private val serviceJob = SupervisorJob()
    private val beaconPreferences by lazy {
        BeaconPreferences.getInstance(applicationContext)
    }


    private fun checkPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Verify permissions before starting transmission
        if (!checkPermissions()) {
            Log.e(TAG, "Missing required permissions")
            broadcastStatus("ERROR", "Missing required permissions")
            stopSelf()
            return START_NOT_STICKY
        }

        // Check if Bluetooth is enabled
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth is not enabled")
            broadcastStatus("ERROR", "Bluetooth is not enabled")
            stopSelf()
            return START_NOT_STICKY
        }

        // Get beacon data from intent
        intent?.let { startIntent ->
            try {
                val uuid = startIntent.getStringExtra("uuid") ?: return@let
                val major = startIntent.getStringExtra("major") ?: return@let
                val minor = startIntent.getStringExtra("minor") ?: return@let
                val latitude = startIntent.getDoubleExtra("latitude", 0.0)
                val longitude = startIntent.getDoubleExtra("longitude", 0.0)


                currentBeacon = VirtualBeacon(
                    uuid = uuid,
                    major = major,
                    minor = minor,
                    latitude = latitude,
                    longitude = longitude
                )

                coroutineScope.launch {
                    registerBeaconWithBackend(currentBeacon!!)
                }

                startBeaconTransmission(uuid, major, minor)

            } catch (e: Exception) {
                Log.e(TAG, "Error starting beacon transmission: ${e.message}")
                broadcastStatus("ERROR", e.message ?: "Unknown error")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startBeaconTransmission(uuid: String, major: String, minor: String) {
        // Check transmission support
        val transmissionSupport = BeaconTransmitter.checkTransmissionSupported(this)
        if (transmissionSupport != BeaconTransmitter.SUPPORTED) {
            throw IllegalStateException("Beacon transmission not supported: $transmissionSupport")
        }

        // Create beacon
        val beacon = Beacon.Builder()
            .setId1(uuid)
            .setId2(major)
            .setId3(minor)
            .setManufacturer(0x0118)
            .setTxPower(-59)
            .setDataFields(listOf(1L))
            .build()

        // Create beacon parser
        val beaconParser = BeaconParser()
            .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")

        // Initialize transmitter
        beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)

        // Start advertising
        beaconTransmitter?.let { transmitter ->
            try {
                if (!transmitter.isStarted) {
                    Log.d(TAG, "Starting beacon transmission with UUID: $uuid")
                    transmitter.startAdvertising(beacon)
                    Log.d(TAG, "Started beacon transmission")
                    broadcastStatus("SUCCESS")

                    // Register with backend after successful transmission start
                    coroutineScope.launch(serviceJob) {
                        registerBeaconWithBackend(currentBeacon!!)
                    }
                } else {
                    Log.w(TAG, "Beacon transmission already active")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception during advertising: ${e.message}")
                broadcastStatus("ERROR", "Permission denied: ${e.message}")
                stopSelf()
            }
        }
    }

    private suspend fun registerBeaconWithBackend(beacon: VirtualBeacon) {
        try {
            withContext(serviceJob) {
                BeaconApiService.registerBeacon(beacon)
                    .onSuccess {
                        Log.d(TAG, "Successfully registered beacon with backend")
                        broadcastStatus("SUCCESS")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to register beacon with backend: ${error.message}")
                        broadcastStatus("ERROR", "Failed to register beacon: ${error.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering beacon with backend: ${e.message}")
            broadcastStatus("ERROR", "Error registering beacon: ${e.message}")
        }
    }

    private fun broadcastStatus(status: String, message: String = "") {
        Intent("com.iiitl.locateme.BEACON_STATUS").also { intent ->
            intent.putExtra("status", status)
            intent.putExtra("message", message)
            sendBroadcast(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Beacon Transmitter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for beacon transmission service"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // In BeaconTransmitterService.kt, update the createNotification() function:

    private fun createNotification(): Notification {
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Beacon Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Virtual Beacon Transmission Service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create pending intent for notification click
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = ACTION_SHOW_REGISTER_SCREEN
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Virtual Beacon Active")
            .setContentText("Beacon is transmitting")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        try {
            currentBeacon?.let { beacon ->
                // Launch a new coroutine for cleanup
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        BeaconApiService.deactivateBeacon(beacon.uuid, beacon.major, beacon.minor)
                            .onSuccess {
                                Log.d(TAG, "Successfully deactivated beacon in backend")
                            }
                            .onFailure { error ->
                                Log.e(TAG, "Failed to deactivate beacon in backend: ${error.message}")
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deactivating beacon in backend: ${e.message}")
                    }
                }
            }

            beaconTransmitter?.let { transmitter ->
                if (transmitter.isStarted) {
                    transmitter.stopAdvertising()
                    Log.d(TAG, "Stopped beacon transmission")
                }
            }

            broadcastStatus("STOPPED")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping beacon transmission: ${e.message}")
            broadcastStatus("ERROR", "Error stopping beacon: ${e.message}")
        } finally {
            // Cancel all coroutines
            serviceJob.cancel()
            coroutineScope.cancel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            super.onDestroy()
        }
    }



    override fun onBind(intent: Intent?): IBinder? = null

}
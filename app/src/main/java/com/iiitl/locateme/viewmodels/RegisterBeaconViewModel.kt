// viewmodels/RegisterBeaconViewModel.kt
package com.iiitl.locateme.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iiitl.locateme.services.BeaconTransmitterService
import com.iiitl.locateme.utils.BeaconPreferences
import com.iiitl.locateme.utils.BeaconState
import com.iiitl.locateme.utils.DeviceUuidManager
import com.iiitl.locateme.utils.PermissionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import java.util.*

data class RegisterBeaconUiState(
    val hasPermissions: Boolean = false,
    val permissionsDenied: Boolean = false,
    val isTransmitting: Boolean = false,
    val uuid: String = "",  // Will be initialized with device UUID
    val major: String = "1",
    val minor: String = "1",
    val latitude: String = "",
    val longitude: String = "",
    val isLocationValid: Boolean = false,
    val transmissionStatus: String? = null,
    val error: String? = null,
    var isBluetoothEnabled: Boolean = false,
    val showBluetoothDialog: Boolean = false
)

class RegisterBeaconViewModel(application: Application) : AndroidViewModel(application) {
    private val deviceUuidManager = DeviceUuidManager(application)
    private val beaconPreferences = BeaconPreferences.getInstance(application)

    private val _uiState = MutableStateFlow(
        RegisterBeaconUiState(
            uuid = deviceUuidManager.getDeviceUuid()
        )
    )
    val uiState: StateFlow<RegisterBeaconUiState> = _uiState.asStateFlow()

    private var permissionsManager: PermissionsManager? = null
    private var beaconTransmitter: BeaconTransmitter? = null

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("status")) {
                "SUCCESS" -> {
                    _uiState.update { it.copy(
                        isTransmitting = true,
                        transmissionStatus = "Beacon transmission active",
                        error = null
                    )}
                }
                "ERROR" -> {
                    val message = intent.getStringExtra("message") ?: "Unknown error"
                    _uiState.update { it.copy(
                        isTransmitting = false,
                        error = "Failed to start beacon: $message"
                    )}
                }
                "STOPPED" -> {
                    _uiState.update { it.copy(
                        isTransmitting = false,
                        transmissionStatus = "Beacon transmission stopped"
                    )}
                }
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_ON -> {
                            _uiState.update { it.copy(
                                isBluetoothEnabled = true,
                                showBluetoothDialog = false
                            )}
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            _uiState.update { it.copy(
                                isBluetoothEnabled = false,
                                showBluetoothDialog = false
                            )}
                            if (uiState.value.isTransmitting) {
                                stopTransmitting()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkBluetoothState() {
        val bluetoothManager = getApplication<Application>().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            _uiState.update {
                it.copy(
                    error = "Bluetooth is not supported on this device",
                    isBluetoothEnabled = false
                )
            }
            return
        }

        _uiState.update { it.copy(isBluetoothEnabled = bluetoothAdapter.isEnabled) }
    }


    init {
        checkBluetoothState()
        monitorBluetoothState()
        viewModelScope.launch {
            // Load saved state
            beaconPreferences.beaconState.collect { savedState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isTransmitting = savedState.isTransmitting,
                        uuid = savedState.uuid.ifEmpty { currentState.uuid },
                        major = savedState.major,
                        minor = savedState.minor,
                        latitude = savedState.latitude,
                        longitude = savedState.longitude,
                        isLocationValid = validateLocation(savedState.latitude, savedState.longitude)
                    )
                }
            }

        }
    }

    private fun monitorBluetoothState() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        getApplication<Application>().registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        if (uiState.value.isTransmitting) {
                            stopTransmitting()
                            _uiState.update { it.copy(
                                error = "Bluetooth was turned off. Beacon transmission stopped."
                            )}
                        }
                    }
                }
            }
        }, filter)
    }

    private fun validateInput(): Boolean {
        val currentState = _uiState.value
        val majorInt = currentState.major.toIntOrNull()
        val minorInt = currentState.minor.toIntOrNull()

        if (majorInt == null || minorInt == null ||
            majorInt !in 0..65535 || minorInt !in 0..65535) {
            _uiState.update { it.copy(
                error = "Major and minor values must be between 0 and 65535"
            )}
            return false
        }

        val lat = currentState.latitude.toDoubleOrNull()
        val lon = currentState.longitude.toDoubleOrNull()

        if (lat == null || lon == null ||
            lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _uiState.update { it.copy(
                error = "Invalid coordinates"
            )}
            return false
        }

        return true
    }



    fun startTransmitting(context: Context) {
        if (!validateInput()) return
        val bluetoothManager = getApplication<Application>().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            _uiState.update { it.copy(error = "Bluetooth is not supported on this device") }
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth is disabled", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(showBluetoothDialog = true) }
            return
        }

        viewModelScope.launch {
            val currentState = _uiState.value

            if (!currentState.isLocationValid) {
                _uiState.update { it.copy(error = "Please enter valid location") }
                return@launch
            }

            try {
                _uiState.update { it.copy(transmissionStatus = "Starting beacon...") }

                val serviceIntent = Intent(getApplication(), BeaconTransmitterService::class.java).apply {
                    putExtra("uuid", currentState.uuid)
                    putExtra("major", currentState.major)
                    putExtra("minor", currentState.minor)
                    putExtra("latitude", currentState.latitude.toDoubleOrNull() ?: 0.0)
                    putExtra("longitude", currentState.longitude.toDoubleOrNull() ?: 0.0)
                }

                // Save state before starting service
                beaconPreferences.updateBeaconState(
                    BeaconState(
                        isTransmitting = true,
                        uuid = currentState.uuid,
                        major = currentState.major,
                        minor = currentState.minor,
                        latitude = currentState.latitude,
                        longitude = currentState.longitude
                    )
                )

                getApplication<Application>().startForegroundService(serviceIntent)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isTransmitting = false,
                    error = "Error starting beacon: ${e.message}"
                )}
                beaconPreferences.updateBeaconState(
                    BeaconState(isTransmitting = false)
                )
            }
        }
    }

    fun showBluetoothDialog(show: Boolean) {
        _uiState.update { it.copy(showBluetoothDialog = show) }
    }


    fun stopTransmitting() {
        viewModelScope.launch {
            try {
                getApplication<Application>().stopService(
                    Intent(getApplication(), BeaconTransmitterService::class.java)
                )
                beaconPreferences.updateBeaconState(
                    BeaconState(isTransmitting = false)
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Error stopping beacon service: ${e.message}"
                )}
            }
        }
    }


    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionsManager = PermissionsManager(
            permissionLauncher = launcher,
            onPermissionsGranted = { handlePermissionsGranted() },
            onPermissionsDenied = { handlePermissionsDenied() }
        )
        checkInitialPermissions()
    }

    fun requestPermissions() {
        permissionsManager?.checkAndRequestPermissions()
    }

    private fun checkInitialPermissions() {
        val hasPermissions = PermissionsManager.hasPermissions(getApplication())
        _uiState.update { it.copy(hasPermissions = hasPermissions) }
    }

    fun handlePermissionsGranted() {
        _uiState.update { it.copy(
            hasPermissions = true,
            permissionsDenied = false
        )}
    }

    fun handlePermissionsDenied() {
        _uiState.update { it.copy(
            hasPermissions = false,
            permissionsDenied = true,
            error = "Permissions required for beacon registration"
        )}
    }

    fun updateLatitude(latitude: String) {
        try {
            if (latitude.isNotEmpty()) {
                latitude.toDouble()
            }
            _uiState.update { it.copy(
                latitude = latitude,
                isLocationValid = validateLocation(latitude, it.longitude),
                error = null
            )}
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(error = "Invalid latitude format") }
        }
    }

    fun updateLongitude(longitude: String) {
        try {
            if (longitude.isNotEmpty()) {
                longitude.toDouble()
            }
            _uiState.update { it.copy(
                longitude = longitude,
                isLocationValid = validateLocation(it.latitude, longitude),
                error = null
            )}
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(error = "Invalid longitude format") }
        }
    }

    fun updateMajor(major: String) {
        _uiState.update { it.copy(major = major, error = null) }
    }

    fun updateMinor(minor: String) {
        _uiState.update { it.copy(minor = minor, error = null) }
    }

    private fun validateLocation(latitude: String, longitude: String): Boolean {
        return latitude.isNotEmpty() && longitude.isNotEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            Log.e("RegisterBeaconViewModel", "Error unregistering receiver: ${e.message}")
        }
    }
}
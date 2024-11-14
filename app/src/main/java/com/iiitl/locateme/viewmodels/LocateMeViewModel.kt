// viewmodels/LocateMeViewModel.kt
package com.iiitl.locateme.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iiitl.locateme.utils.beacon.BeaconData
import com.iiitl.locateme.utils.location.LocationManager
import com.iiitl.locateme.utils.PermissionsManager
import com.iiitl.locateme.utils.positioning.Position
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LocateMeUiState(
    val hasPermissions: Boolean = false,
    val permissionsDenied: Boolean = false,
    val isScanning: Boolean = false,
    val currentPosition: Position? = null,
    val nearbyBeacons: List<BeaconData> = emptyList(),
    val error: String? = null,
    val isBluetoothEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false
)

class LocateMeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LocateMeViewModel"
    private val locationManager = LocationManager(application)
    private val _uiState = MutableStateFlow(LocateMeUiState())
    val uiState: StateFlow<LocateMeUiState> = _uiState.asStateFlow()

    private var permissionsManager: PermissionsManager? = null

    // Add broadcast receiver for Bluetooth state changes
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (uiState.value.isScanning) {
                                stopScanning()
                                _uiState.update { it.copy(
                                    error = "Bluetooth was turned off. Scanning stopped."
                                )}
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        // Observe location updates
        viewModelScope.launch {
            locationManager.locationUpdates
                .collect { update ->
                    _uiState.update { state ->
                        state.copy(
                            currentPosition = update.position,
                            nearbyBeacons = update.nearbyBeacons,
                            error = update.error
                        )
                    }
                }

        }
        checkInitialPermissions()
        getApplication<Application>().registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

    }

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionsManager = PermissionsManager(
            permissionLauncher = launcher,
            onPermissionsGranted = { handlePermissionsGranted() },
            onPermissionsDenied = { handlePermissionsDenied() }
        )
        checkInitialPermissions()
    }

    private fun checkInitialPermissions() {
        _uiState.update { it.copy(
            hasPermissions = PermissionsManager.hasPermissions(getApplication())
        )}
    }

    fun requestPermissions() {
        Log.d(TAG, "Requesting permissions")
        permissionsManager?.checkAndRequestPermissions()
    }

    fun handlePermissionsGranted() {
        Log.d(TAG, "Permissions granted")
        _uiState.update { it.copy(
            hasPermissions = true,
            permissionsDenied = false,
            error = null
        )}
    }

    fun handlePermissionsDenied() {
        Log.d(TAG, "Permissions denied")
        _uiState.update { it.copy(
            hasPermissions = false,
            permissionsDenied = true,
            error = "Permissions required for beacon scanning"
        )}
        stopScanning()
    }

    private fun checkBluetoothEnabled(): Boolean {
        val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    }

    private fun checkLocationEnabled(): Boolean {
        val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    fun startScanning() {
        viewModelScope.launch {
            try {
                if (!uiState.value.hasPermissions) {
                    _uiState.update { it.copy(
                        error = "Required permissions not granted"
                    )}
                    return@launch
                }

                // Check Bluetooth
                if (!checkBluetoothEnabled()) {
                    _uiState.update { it.copy(
                        error = "Please enable Bluetooth to scan for beacons"
                    )}
                    return@launch
                }

                // Check Location
                if (!checkLocationEnabled()) {
                    _uiState.update { it.copy(
                        error = "Please enable Location services to scan for beacons"
                    )}
                    return@launch
                }

                _uiState.update { it.copy(isScanning = true, error = null) }
                locationManager.startLocationUpdates()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isScanning = false,
                    error = "Failed to start scanning: ${e.message}"
                )}
            }
        }
    }



    fun stopScanning() {
        viewModelScope.launch {
            try {
                locationManager.stopLocationUpdates()
                _uiState.update { it.copy(isScanning = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to stop scanning: ${e.message}"
                )}
            }
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering bluetooth receiver: ${e.message}")
        }

        super.onCleared()
        stopScanning()
        locationManager.cleanup()
    }
}
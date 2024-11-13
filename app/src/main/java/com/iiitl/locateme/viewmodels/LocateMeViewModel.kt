// viewmodels/LocateMeViewModel.kt
package com.iiitl.locateme.viewmodels

import android.app.Application
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
    val error: String? = null
)

class LocateMeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LocateMeViewModel"
    private val locationManager = LocationManager(application)
    private val _uiState = MutableStateFlow(LocateMeUiState())
    val uiState: StateFlow<LocateMeUiState> = _uiState.asStateFlow()

    private var permissionsManager: PermissionsManager? = null

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

    fun startScanning() {
        viewModelScope.launch {
            try {
                if (!uiState.value.hasPermissions) {
                    _uiState.update { it.copy(
                        error = "Required permissions not granted"
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
        super.onCleared()
        stopScanning()
        locationManager.cleanup()
    }
}
package com.iiitl.locateme.viewmodels

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iiitl.locateme.utils.PermissionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterBeaconUiState(
    val hasPermissions: Boolean = false,
    val permissionsDenied: Boolean = false,
    val isRegistering: Boolean = false,
    val beaconId: String = "",
    val major: String = "",
    val minor: String = "",
    val error: String? = null
)

class RegisterBeaconViewModel(
    application: Application
) : AndroidViewModel(application) {


    private val _uiState = MutableStateFlow(RegisterBeaconUiState())
    val uiState: StateFlow<RegisterBeaconUiState> = _uiState.asStateFlow()

    private var permissionsManager: PermissionsManager? = null

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionsManager = PermissionsManager(
            permissionLauncher = launcher,
            onPermissionsGranted = { onPermissionsGranted() },
            onPermissionsDenied = { onPermissionsDenied() }
        )
        checkPermissions()
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            val hasPermissions = PermissionsManager.hasPermissions(getApplication())
            _uiState.update { it.copy(hasPermissions = hasPermissions) }
        }
    }

    fun requestPermissions() {
        permissionsManager?.checkAndRequestPermissions()
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(
            hasPermissions = true,
            permissionsDenied = false
        )}
    }

    fun onPermissionsDenied() {
        _uiState.update {
            it.copy(
                hasPermissions = false,
                permissionsDenied = true,
                error = "Permissions required for beacon registration"
            )
        }
    }

    fun updateBeaconId(id: String) {
        _uiState.update { it.copy(beaconId = id, error = null) }
    }

    fun updateMajor(major: String) {
        _uiState.update { it.copy(major = major, error = null) }
    }

    fun updateMinor(minor: String) {
        _uiState.update { it.copy(minor = minor, error = null) }
    }

    fun registerBeacon() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, error = null) }

            // Validate input
            if (_uiState.value.beaconId.isBlank() ||
                _uiState.value.major.isBlank() ||
                _uiState.value.minor.isBlank()) {
                _uiState.update { it.copy(
                    isRegistering = false,
                    error = "All fields are required"
                )}
                return@launch
            }

            try {
                // Here you would implement the actual beacon registration
                // For now, just simulate a delay
                kotlinx.coroutines.delay(1000)
                _uiState.update { it.copy(
                    isRegistering = false,
                    beaconId = "",
                    major = "",
                    minor = ""
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRegistering = false,
                    error = e.message ?: "Failed to register beacon"
                )}
            }
        }
    }
}

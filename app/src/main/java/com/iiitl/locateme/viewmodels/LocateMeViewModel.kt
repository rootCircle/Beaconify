// viewmodels/LocateMeViewModel.kt
package com.iiitl.locateme.viewmodels

import android.app.Application
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iiitl.locateme.utils.PermissionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocateMeUiState(
    val hasPermissions: Boolean = false,
    val permissionsDenied: Boolean = false,
    val isScanning: Boolean = false,
    val error: String? = null
)

class LocateMeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LocateMeViewModel"
    private val _uiState = MutableStateFlow(LocateMeUiState())
    val uiState: StateFlow<LocateMeUiState> = _uiState.asStateFlow()

    private var permissionsManager: PermissionsManager? = null

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
    }

    // Placeholder functions for scanning implementation
    fun startScanning() {
        _uiState.update { it.copy(isScanning = true, error = null) }
        // Implement actual scanning logic here
    }

    fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
        // Implement scan stop logic here
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }
}
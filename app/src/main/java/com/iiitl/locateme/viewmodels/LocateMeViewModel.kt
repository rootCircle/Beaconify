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

data class LocateMeUiState(
    val hasPermissions: Boolean = false,
    val permissionsDenied: Boolean = false,
    val isScanning: Boolean = false
)

class LocateMeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LocateMeUiState())
    val uiState: StateFlow<LocateMeUiState> = _uiState.asStateFlow()

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
        startScanning()
    }

    fun onPermissionsDenied() {
        _uiState.update { it.copy(
            hasPermissions = false,
            permissionsDenied = true
        )}
    }

    private fun startScanning() {
        _uiState.update { it.copy(isScanning = true) }
        // Implement beacon scanning here
    }
}

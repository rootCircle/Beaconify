// viewmodels/HomeViewModel.kt
package com.iiitl.locateme.viewmodels

import android.app.Application
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import com.iiitl.locateme.utils.PermissionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val hasPermissions: Boolean = false,
    val permissionsDenied: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "HomeViewModel"
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var permissionsManager: PermissionsManager? = null

    init {
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
            permissionsDenied = false
        )}
    }

    fun handlePermissionsDenied() {
        Log.d(TAG, "Permissions denied")
        _uiState.update { it.copy(
            hasPermissions = false,
            permissionsDenied = true
        )}
    }
}
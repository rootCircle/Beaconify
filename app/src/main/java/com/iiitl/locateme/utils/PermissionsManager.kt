package com.iiitl.locateme.utils

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class PermissionsManager(
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val onPermissionsGranted: () -> Unit,
    private val onPermissionsDenied: () -> Unit
) {
    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun checkAndRequestPermissions() {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    companion object {
        fun hasPermissions(context: Context): Boolean {
            val requiredPermissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            return requiredPermissions.all { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PermissionChecker.PERMISSION_GRANTED
            }
        }
    }
}


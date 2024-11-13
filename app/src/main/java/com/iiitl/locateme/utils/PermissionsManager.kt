// utils/PermissionsManager.kt
package com.iiitl.locateme.utils

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class PermissionsManager(
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val onPermissionsGranted: () -> Unit,
    private val onPermissionsDenied: () -> Unit
) {
    companion object {
        private val TAG = "PermissionsManager"

        fun getRequiredPermissions(): Array<String> {
            val permissions = mutableListOf<String>()

            // Location permissions
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

            // Add Android 12+ (S) permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }

            // Add legacy Bluetooth permissions for older devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH)
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            // Note: Background location is handled separately

            return permissions.toTypedArray()
        }

        fun hasPermissions(context: Context): Boolean {
            // Check basic permissions first
            if (!getRequiredPermissions().all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
                }) {
                return false
            }

            // Check background location for Android 10+ (Q)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PermissionChecker.PERMISSION_GRANTED) {
                    return false
                }
            }

            return true
        }
    }

    fun checkAndRequestPermissions() {
        Log.d(TAG, "Requesting permissions for SDK ${Build.VERSION.SDK_INT}")
        permissionLauncher.launch(getRequiredPermissions())
    }
}
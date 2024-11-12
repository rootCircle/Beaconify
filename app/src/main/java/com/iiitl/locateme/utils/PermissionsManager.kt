// utils/PermissionsManager.kt
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
    companion object {
        fun getRequiredPermissions(): Array<String> {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.addAll(
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    )
                )
            } else {
                permissions.addAll(
                    listOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    )
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            return permissions.toTypedArray()
        }

        fun hasPermissions(context: Context): Boolean {
            return getRequiredPermissions().all { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PermissionChecker.PERMISSION_GRANTED
            }
        }
    }

    fun checkAndRequestPermissions() {
        permissionLauncher.launch(getRequiredPermissions())
    }
}
// MainActivity.kt
package com.iiitl.locateme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iiitl.locateme.screens.HomeScreen
import com.iiitl.locateme.screens.LocateMeScreen
import com.iiitl.locateme.screens.RegisterBeaconScreen
import com.iiitl.locateme.viewmodels.HomeViewModel
import com.iiitl.locateme.viewmodels.LocateMeViewModel
import com.iiitl.locateme.viewmodels.RegisterBeaconViewModel
import androidx.compose.material3.*

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var locateMeViewModel: LocateMeViewModel
    private lateinit var registerViewModel: RegisterBeaconViewModel

    private var showBackgroundPermissionDialog by mutableStateOf(false)
    private var showNotificationPermissionDialog by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission launcher first
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "Permission results: $permissions")

            val allGranted = permissions.all { it.value }
            if (allGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Check notification permission
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionDialog = true
                    } else {
                        requestNotificationPermission()
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocation()
                } else {
                    handlePermissionsGranted()
                }
            } else {
                handlePermissionsDenied()
            }
        }

        // Initialize ViewModels
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        locateMeViewModel = ViewModelProvider(this)[LocateMeViewModel::class.java]
        registerViewModel = ViewModelProvider(this)[RegisterBeaconViewModel::class.java]

        // Set permission launchers
        homeViewModel.setPermissionLauncher(permissionLauncher)
        locateMeViewModel.setPermissionLauncher(permissionLauncher)
        registerViewModel.setPermissionLauncher(permissionLauncher)

        setContent {
            MaterialTheme {
                Box {
                    BeaconifyApp(
                        homeViewModel = homeViewModel,
                        locateViewModel = locateMeViewModel,
                        registerViewModel = registerViewModel
                    )

                    if (showBackgroundPermissionDialog) {
                        BackgroundLocationPermissionDialog(
                            onConfirm = {
                                showBackgroundPermissionDialog = false
                                requestBackgroundLocationPermission()
                            },
                            onDismiss = {
                                showBackgroundPermissionDialog = false
                                handlePermissionsDenied()
                            }
                        )
                    }

                    if (showNotificationPermissionDialog) {
                        NotificationPermissionDialog(
                            onConfirm = {
                                showNotificationPermissionDialog = false
                                requestNotificationPermission()
                            },
                            onDismiss = {
                                showNotificationPermissionDialog = false
                                // Even if notification permission is denied, we can continue with location
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    requestBackgroundLocation()
                                } else {
                                    handlePermissionsGranted()
                                }
                            }
                        )
                    }
                }
            }
        }

    }

    @Composable
    private fun NotificationPermissionDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Notification Permission") },
            text = {
                Text("Beaconify needs notification permission to show when a virtual beacon is active. " +
                        "This helps you know when your beacon is transmitting.")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }
            }
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BACKGROUND_LOCATION_PERMISSION_CODE -> {
                handleBackgroundLocationPermissionResult(grantResults)
            }
            NOTIFICATION_PERMISSION_CODE -> {
                // Proceed with location permissions regardless of notification permission result
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocation()
                } else {
                    handlePermissionsGranted()
                }
            }
        }
    }

    private fun handleBackgroundLocationPermissionResult(grantResults: IntArray) {
        if (grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handlePermissionsGranted()
        } else {
            handlePermissionsDenied()
        }
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )) {
                showBackgroundPermissionDialog = true
            } else {
                requestBackgroundLocationPermission()
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_CODE
            )
        }
    }

    @Composable
    private fun BackgroundLocationPermissionDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Background Location Required")
            },
            text = {
                Text("This app needs background location access to detect beacons in the background.")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Deny")
                }
            }
        )
    }

    private fun handlePermissionsGranted() {
        homeViewModel.handlePermissionsGranted()
        locateMeViewModel.handlePermissionsGranted()
        registerViewModel.handlePermissionsGranted()
    }

    private fun handlePermissionsDenied() {
        homeViewModel.handlePermissionsDenied()
        locateMeViewModel.handlePermissionsDenied()
        registerViewModel.handlePermissionsDenied()
    }

    companion object {
        private const val BACKGROUND_LOCATION_PERMISSION_CODE = 2
        private const val NOTIFICATION_PERMISSION_CODE = 3
    }
}

@Composable
fun BeaconifyApp(
    locateViewModel: LocateMeViewModel,
    registerViewModel: RegisterBeaconViewModel,
    homeViewModel: HomeViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                viewModel = homeViewModel
            )
        }
        composable(Screen.Locate.route) {
            LocateMeScreen(viewModel = locateViewModel)
        }
        composable(Screen.Register.route) {
            RegisterBeaconScreen(viewModel = registerViewModel)
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Locate : Screen("locate")
    object Register : Screen("register")
}
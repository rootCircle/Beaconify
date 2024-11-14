// MainActivity.kt
package com.iiitl.locateme

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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

// MainActivity.kt
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var locateMeViewModel: LocateMeViewModel
    private lateinit var registerViewModel: RegisterBeaconViewModel

    private var showBackgroundPermissionDialog by mutableStateOf(false)
    private var showNotificationPermissionDialog by mutableStateOf(false)

    // Define the permission launcher using the new API
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permission results: $permissions")

        val allGranted = permissions.all { it.value }
        if (allGranted) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    handleNotificationPermission()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    handleBackgroundLocationPermission()
                }
                else -> {
                    handlePermissionsGranted()
                }
            }
        } else {
            handlePermissionsDenied()
        }
    }

    // Define background location permission launcher
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            handlePermissionsGranted()
        } else {
            handlePermissionsDenied()
        }
    }

    // Define notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Even if notification permission is denied, we can proceed with location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleBackgroundLocationPermission()
        } else {
            handlePermissionsGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                                    handleBackgroundLocationPermission()
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

    private fun handleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showNotificationPermissionDialog = true
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showBackgroundPermissionDialog = true
            } else {
                requestBackgroundLocationPermission()
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

// MainActivity.kt
// Add inside MainActivity class

@Composable
private fun BackgroundLocationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Background Location Required",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "To detect beacons while the app is in the background, we need " +
                        "permission to access your location all the time. This helps us provide " +
                        "you with accurate positioning even when the app is not actively being used.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Deny")
            }
        }
    )
}

@Composable
private fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Notification Permission",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Notifications help you know when virtual beacons are active and " +
                        "when important events happen. Would you like to allow Beaconify to send " +
                        "you notifications?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Not Now")
            }
        }
    )
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Locate : Screen("locate")
    data object Register : Screen("register")
}
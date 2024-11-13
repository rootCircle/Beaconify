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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission launcher first
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "Permission results: $permissions")

            val allGranted = permissions.all { it.value }
            if (allGranted) {
                // For Android 10+ (Q), request background location separately
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

                    // Background Permission Dialog
                    if (showBackgroundPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showBackgroundPermissionDialog = false },
                            title = { Text("Background Location Required") },
                            text = { Text("This app needs background location access to detect beacons in the background.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showBackgroundPermissionDialog = false
                                        ActivityCompat.requestPermissions(
                                            this@MainActivity,
                                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                            BACKGROUND_LOCATION_PERMISSION_CODE
                                        )
                                    }
                                ) {
                                    Text("Grant")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showBackgroundPermissionDialog = false
                                        handlePermissionsDenied()
                                    }
                                ) {
                                    Text("Deny")
                                }
                            }
                        )
                    }
                }
            }
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
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSION_CODE
                )
            }
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
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handlePermissionsGranted()
                } else {
                    handlePermissionsDenied()
                }
            }
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

    companion object {
        private const val BACKGROUND_LOCATION_PERMISSION_CODE = 2
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
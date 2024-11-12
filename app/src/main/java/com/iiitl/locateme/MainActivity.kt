// MainActivity.kt
package com.iiitl.locateme

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iiitl.locateme.screens.*
import com.iiitl.locateme.utils.PermissionsManager
import com.iiitl.locateme.viewmodels.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Locate : Screen("locate")
    object Register : Screen("register")
}

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locateMeViewModel: LocateMeViewModel
    private lateinit var registerViewModel: RegisterBeaconViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                locateMeViewModel.handlePermissionsGranted()
                registerViewModel.handlePermissionsGranted()
            } else {
                locateMeViewModel.handlePermissionsDenied()
                registerViewModel.handlePermissionsDenied()
            }
        }

        // Initialize ViewModels
        locateMeViewModel = ViewModelProvider(this)[LocateMeViewModel::class.java]
        registerViewModel = ViewModelProvider(this)[RegisterBeaconViewModel::class.java]

        // Set permission launchers
        locateMeViewModel.setPermissionLauncher(permissionLauncher)
        registerViewModel.setPermissionLauncher(permissionLauncher)

        setContent {
            MaterialTheme {
                BeaconifyApp(
                    locateViewModel = locateMeViewModel,
                    registerViewModel = registerViewModel
                )
            }
        }
    }
}

@Composable
fun BeaconifyApp(
    locateViewModel: LocateMeViewModel,
    registerViewModel: RegisterBeaconViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Locate.route) {
            LocateMeScreen(viewModel = locateViewModel)
        }
        composable(Screen.Register.route) {
            RegisterBeaconScreen(viewModel = registerViewModel)
        }
    }
}
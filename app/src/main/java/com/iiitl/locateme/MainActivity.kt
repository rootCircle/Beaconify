package com.iiitl.locateme

// MainActivity.kt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiitl.locateme.screens.HomeScreen
import com.iiitl.locateme.screens.LocateMeScreen
import com.iiitl.locateme.screens.RegisterBeaconScreen
import com.iiitl.locateme.viewmodels.LocateMeViewModel
import com.iiitl.locateme.viewmodels.RegisterBeaconViewModel

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locateViewModel: LocateMeViewModel
    private lateinit var registerViewModel: RegisterBeaconViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModels
        locateViewModel = LocateMeViewModel(application)
        registerViewModel = RegisterBeaconViewModel(application)

        // Setup permission launcher before setting content
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                locateViewModel.onPermissionsGranted()
                registerViewModel.onPermissionsGranted()
            } else {
                locateViewModel.onPermissionsDenied()
                registerViewModel.onPermissionsDenied()
            }
        }

        // Pass the launcher to ViewModels
        locateViewModel.setPermissionLauncher(permissionLauncher)
        registerViewModel.setPermissionLauncher(permissionLauncher)

        setContent {
            MaterialTheme {
                BeaconifyApp(
                    locateViewModel = locateViewModel,
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

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("locate") {
            LocateMeScreen(viewModel = locateViewModel)
        }
        composable("register") {
            RegisterBeaconScreen(viewModel = registerViewModel)
        }
    }
}

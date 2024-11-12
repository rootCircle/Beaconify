package com.iiitl.locateme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiitl.locateme.viewmodels.LocateMeViewModel

@Composable
fun LocateMeScreen(
    viewModel: LocateMeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            !uiState.hasPermissions -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Location permissions are required to detect beacons",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { viewModel.requestPermissions() }
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
            uiState.permissionsDenied -> {
                Text(
                    text = "Permissions were denied. Please enable them in settings to use this feature.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                // Your beacon scanning UI goes here
                Text(
                    text = "Scanning for beacons...",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}


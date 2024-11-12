// screens/LocateMeScreen.kt
package com.iiitl.locateme.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iiitl.locateme.utils.PermissionsManager
import com.iiitl.locateme.viewmodels.LocateMeViewModel

@Composable
fun LocateMeScreen(
    viewModel: LocateMeViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasPermissions = remember(uiState.hasPermissions) {
        PermissionsManager.hasPermissions(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Locate Me",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!hasPermissions) {
            PermissionsRequest(
                isDenied = uiState.permissionsDenied,
                onRequestPermissions = {
                    Log.d("LocateScreen", "Requesting permissions")
                    viewModel.requestPermissions()
                }
            )
        } else {
            // Placeholder for actual locate functionality
            LocateContent(
                isScanning = uiState.isScanning,
                error = uiState.error,
                onStartScan = viewModel::startScanning,
                onStopScan = viewModel::stopScanning
            )
        }
    }
}

@Composable
private fun PermissionsRequest(
    isDenied: Boolean,
    onRequestPermissions: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "The following permissions are required:\n" +
                    "• Location (for beacon scanning)\n" +
                    "• Bluetooth (for beacon detection)\n" +
                    "• Background Location (for background scanning)",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        if (isDenied) {
            Text(
                text = "Some permissions were denied. Please grant them to use this feature.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun LocateContent(
    isScanning: Boolean,
    error: String?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Placeholder content
        Text(
            text = "Ready to scan for beacons",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (isScanning) onStopScan() else onStartScan() },
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text(if (isScanning) "Stop Scanning" else "Start Scanning")
        }

        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
package com.iiitl.locateme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiitl.locateme.viewmodels.RegisterBeaconUiState
import com.iiitl.locateme.viewmodels.RegisterBeaconViewModel

@Composable
fun RegisterBeaconScreen(
    viewModel: RegisterBeaconViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            !uiState.hasPermissions -> {
                PermissionRequest(
                    onRequestPermissions = { viewModel.requestPermissions() }
                )
            }
            uiState.permissionsDenied -> {
                PermissionDenied()
            }
            else -> {
                RegisterBeaconContent(
                    uiState = uiState,
                    onBeaconIdChanged = viewModel::updateBeaconId,
                    onMajorChanged = viewModel::updateMajor,
                    onMinorChanged = viewModel::updateMinor,
                    onRegisterClicked = viewModel::registerBeacon
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bluetooth permissions are required to register beacons",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun PermissionDenied() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissions were denied. Please enable them in settings to register beacons.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun RegisterBeaconContent(
    uiState: RegisterBeaconUiState,
    onBeaconIdChanged: (String) -> Unit,
    onMajorChanged: (String) -> Unit,
    onMinorChanged: (String) -> Unit,
    onRegisterClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Register New Beacon",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = uiState.beaconId,
            onValueChange = onBeaconIdChanged,
            label = { Text("Beacon ID") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.major,
            onValueChange = onMajorChanged,
            label = { Text("Major") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.minor,
            onValueChange = onMinorChanged,
            label = { Text("Minor") },
            modifier = Modifier.fillMaxWidth()
        )

        if (!uiState.error.isNullOrBlank()) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onRegisterClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = !uiState.isRegistering
        ) {
            if (uiState.isRegistering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register Beacon")
            }
        }
    }
}
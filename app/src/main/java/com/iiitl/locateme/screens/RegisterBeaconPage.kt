// screens/RegisterBeaconScreen.kt
package com.iiitl.locateme.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.iiitl.locateme.utils.PermissionsManager
import com.iiitl.locateme.viewmodels.RegisterBeaconUiState
import com.iiitl.locateme.viewmodels.RegisterBeaconViewModel

@Composable
fun RegisterBeaconScreen(
    viewModel: RegisterBeaconViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasPermissions = remember(uiState.hasPermissions) {
        PermissionsManager.hasPermissions(context)
    }
    var showErrorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Register Virtual Beacon",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(uiState.error ?: "Unknown error occurred") },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Show Snackbar for transient errors
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }

        // Error Banner
        if (!uiState.error.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (!hasPermissions) {
            PermissionsRequest(
                isDenied = uiState.permissionsDenied,
                onRequestPermissions = {
                    Log.d("RegisterScreen", "Requesting permissions")
                    viewModel.requestPermissions()
                }
            )
        } else {
            BeaconRegistrationForm(
                uiState = uiState,
                onLatitudeChanged = viewModel::updateLatitude,
                onLongitudeChanged = viewModel::updateLongitude,
                onMajorChanged = viewModel::updateMajor,
                onMinorChanged = viewModel::updateMinor,
                onStartTransmitting = { (viewModel::startTransmitting)(context) },
                onStopTransmitting = viewModel::stopTransmitting
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
                    "• Bluetooth (for beacon transmission)\n" +
                    "• Background Location (for background operation)",
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
private fun BeaconRegistrationForm(
    uiState: RegisterBeaconUiState,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onMajorChanged: (String) -> Unit,
    onMinorChanged: (String) -> Unit,
    onStartTransmitting: () -> Unit,
    onStopTransmitting: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Location Fields
        OutlinedTextField(
            value = uiState.latitude,
            onValueChange = onLatitudeChanged,
            label = { Text("Latitude") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = uiState.longitude,
            onValueChange = onLongitudeChanged,
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )

        // UUID Display (Read-only)
        OutlinedTextField(
            value = uiState.uuid,
            onValueChange = { },
            label = { Text("UUID (Auto-generated)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            singleLine = true
        )

        // Major and Minor Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.major,
                onValueChange = onMajorChanged,
                label = { Text("Major") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = uiState.minor,
                onValueChange = onMinorChanged,
                label = { Text("Minor") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
        }

        // Status and Error Messages
        if (!uiState.error.isNullOrBlank()) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        uiState.transmissionStatus?.let { status ->
            Text(
                text = status,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start/Stop Button
        Button(
            onClick = {
                if (!uiState.isTransmitting) {
                    onStartTransmitting()
                } else {
                    onStopTransmitting()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = uiState.isLocationValid || uiState.isTransmitting
        ) {
            Text(if (uiState.isTransmitting) "Stop Transmission" else "Start Virtual Beacon")
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
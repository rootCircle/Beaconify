// screens/LocateMeScreen.kt
package com.iiitl.locateme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iiitl.locateme.utils.beacon.BeaconData
import com.iiitl.locateme.utils.positioning.Position
import com.iiitl.locateme.viewmodels.LocateMeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocateMeScreen(
    viewModel: LocateMeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Locate Me",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!uiState.hasPermissions) {
            PermissionsRequest(
                isDenied = uiState.permissionsDenied,
                onRequestPermissions = viewModel::requestPermissions
            )
        } else {
            LocationContent(
                isScanning = uiState.isScanning,
                currentPosition = uiState.currentPosition,
                nearbyBeacons = uiState.nearbyBeacons,
                error = uiState.error,
                onStartScan = viewModel::startScanning,
                onStopScan = viewModel::stopScanning
            )
        }
    }
}

@Composable
private fun LocationContent(
    isScanning: Boolean,
    currentPosition: Position?,
    nearbyBeacons: List<BeaconData>,
    error: String?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current Position Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Position",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentPosition != null) {
                    Text("Latitude: ${String.format("%.6f", currentPosition.latitude)}")
                    Text("Longitude: ${String.format("%.6f", currentPosition.longitude)}")
                    Text("Accuracy: ${String.format("%.2f", currentPosition.accuracy)} meters")
                    Text(
                        text = "Last Updated: ${
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(currentPosition.timestamp))
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "No position available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Scanning Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isScanning) "Scanning..." else "Scanner Stopped",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = { if (isScanning) onStopScan() else onStartScan() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isScanning) "Stop Scanning" else "Start Scanning")
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // Error Display
        if (!error.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Nearby Beacons Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Beacons",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nearby Beacons (${nearbyBeacons.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nearbyBeacons) { beacon ->
                        BeaconItem(beacon = beacon)
                    }
                }
            }
        }
    }
}

@Composable
private fun BeaconItem(beacon: BeaconData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "UUID: ${beacon.uuid}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Major: ${beacon.major}, Minor: ${beacon.minor}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Distance: ${String.format("%.2f", beacon.distance)} m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Signal Strength: ${beacon.rssi} dBm",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Location: ${String.format("%.6f", beacon.latitude)}, " +
                        "${String.format("%.6f", beacon.longitude)}",
                style = MaterialTheme.typography.bodySmall
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
            .fillMaxSize()
            .padding(16.dp)
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
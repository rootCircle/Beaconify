// screens/LocateMePage.kt
package com.iiitl.locateme.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iiitl.locateme.utils.beacon.BeaconData
import com.iiitl.locateme.utils.positioning.Position
import com.iiitl.locateme.viewmodels.LocateMeUiState
import com.iiitl.locateme.viewmodels.LocateMeViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.viewinterop.AndroidView
import com.iiitl.locateme.utils.graph.BeaconVisualizerView
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun LocateMeScreen(
    viewModel: LocateMeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                ResponsiveLocationContent(
                    uiState = uiState,
                    context = context,
                    screenHeight = screenHeight,
                    screenWidth = screenWidth,
                    onStartScan = viewModel::startScanning,
                    onStopScan = viewModel::stopScanning
                )
            }
        }
    }
}

@Composable
private fun ResponsiveLocationContent(
    uiState: LocateMeUiState,
    context: Context,
    screenHeight: Dp,
    screenWidth: Dp,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    // Calculate responsive dimensions
    val visualizerHeight = (screenHeight * 0.4f).coerceAtMost(300.dp)
    val cardSpacing = 8.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Beacon Visualization Card with responsive height
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualizerHeight)
                .padding(bottom = cardSpacing)
        ) {
            AndroidView(
                factory = { context ->
                    BeaconVisualizerView(context).apply {
                        updateData(
                            latitude = uiState.currentPosition?.latitude,
                            longitude = uiState.currentPosition?.longitude,
                            accuracy = uiState.currentPosition?.accuracy,
                            newBeacons = uiState.nearbyBeacons
                        )
                    }
                },
                update = { view ->
                    view.updateData(
                        latitude = uiState.currentPosition?.latitude,
                        longitude = uiState.currentPosition?.longitude,
                        accuracy = uiState.currentPosition?.accuracy,
                        newBeacons = uiState.nearbyBeacons
                    )
                }
            )
        }

        // Current Position Card
        PositionCard(
            currentPosition = uiState.currentPosition,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = cardSpacing)
        )

        // Scanner Controls
        ScannerControls(
            isScanning = uiState.isScanning,
            hasError = !uiState.error.isNullOrBlank(),
            context = context,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = cardSpacing)
        )

        // Error Display
        if (!uiState.error.isNullOrBlank()) {
            ErrorCard(
                error = uiState.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = cardSpacing)
            )
        }

        // Nearby Beacons Section with LazyColumn
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = cardSpacing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                BeaconHeader(beaconCount = uiState.nearbyBeacons.size)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight * 0.4f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.nearbyBeacons) { beacon ->
                        BeaconItem(beacon = beacon)
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionCard(
    currentPosition: Position?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
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
                Text("Latitude: ${String.format(Locale.US, "%.6f", currentPosition.latitude)}")
                Text("Longitude: ${String.format(Locale.US, "%.6f", currentPosition.longitude)}")
                Text("Accuracy: ${String.format(Locale.US, "%.2f", currentPosition.accuracy)} meters")
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
}

@Composable
private fun ScannerControls(
    isScanning: Boolean,
    hasError: Boolean,
    context: Context,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        if (!isScanning && hasError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                ) {
                    Text("Bluetooth Settings")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                ) {
                    Text("Location Settings")
                }
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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

@Composable
private fun BeaconHeader(beaconCount: Int) {
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
            text = "Nearby Beacons ($beaconCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
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
                text = "Distance: ${String.format(Locale.US, "%.2f", beacon.distance)} m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Signal Strength: ${beacon.rssi} dBm",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Location: ${String.format(Locale.US, "%.6f", beacon.latitude)}, " +
                        String.format(Locale.US, "%.6f", beacon.longitude),
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
            .fillMaxWidth()
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
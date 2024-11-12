package com.iiitl.locateme.utils.location

import android.content.Context
import android.util.Log
import com.iiitl.locateme.utils.beacon.BeaconData
import com.iiitl.locateme.utils.beacon.BeaconScanner
import com.iiitl.locateme.utils.positioning.Position
import com.iiitl.locateme.utils.positioning.PositionCalculator
import com.iiitl.locateme.utils.positioning.PositionCalculatorFactory
import kotlinx.coroutines.flow.*

data class LocationUpdate(
    val position: Position?,
    val nearbyBeacons: List<BeaconData>,
    val error: String? = null
)

class LocationManager(
    context: Context,
    calculatorType: PositionCalculatorFactory.CalculatorType = PositionCalculatorFactory.CalculatorType.WEIGHTED_CENTROID
) {
    private val TAG = "LocationManager"
    private val beaconScanner = BeaconScanner(context)
    private val positionCalculator: PositionCalculator = PositionCalculatorFactory.getCalculator(calculatorType)

    private val _locationUpdates = MutableStateFlow<LocationUpdate>(
        LocationUpdate(null, emptyList())
    )
    val locationUpdates: StateFlow<LocationUpdate> = _locationUpdates.asStateFlow()

    init {
        // Observe beacon scanner updates
        beaconScanner.scannedBeacons
            .onEach { beacons ->
                try {
                    val position = positionCalculator.calculatePosition(beacons)
                    _locationUpdates.value = LocationUpdate(
                        position = position,
                        nearbyBeacons = beacons
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating position: ${e.message}")
                    _locationUpdates.value = LocationUpdate(
                        position = null,
                        nearbyBeacons = beacons,
                        error = "Error calculating position: ${e.message}"
                    )
                }
            }
            .catch { e ->
                Log.e(TAG, "Error in location updates: ${e.message}")
                _locationUpdates.value = LocationUpdate(
                    position = null,
                    nearbyBeacons = emptyList(),
                    error = "Error processing beacons: ${e.message}"
                )
            }
    }

    fun startLocationUpdates() {
        beaconScanner.startScanning()
    }

    fun stopLocationUpdates() {
        beaconScanner.stopScanning()
    }
}
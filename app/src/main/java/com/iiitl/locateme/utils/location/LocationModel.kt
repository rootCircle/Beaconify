// utils/location/LocationModel.kt
package com.iiitl.locateme.utils.location

import com.iiitl.locateme.utils.beacon.BeaconData
import com.iiitl.locateme.utils.positioning.Position

data class LocationUpdate(
    val position: Position?,
    val nearbyBeacons: List<BeaconData>,
    val error: String? = null
)
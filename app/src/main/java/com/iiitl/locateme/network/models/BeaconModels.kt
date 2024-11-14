package com.iiitl.locateme.network.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class VirtualBeacon(
    val uuid: String,
    val major: String,
    val minor: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean = true,
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
)

data class BeaconResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<VirtualBeacon>? = null
)

data class DeactivateBeaconRequest(
    val uuid: String,
    val major: String,
    val minor: String
)

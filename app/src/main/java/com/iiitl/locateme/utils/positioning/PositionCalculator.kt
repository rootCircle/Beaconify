package com.iiitl.locateme.utils.positioning

import com.iiitl.locateme.utils.beacon.BeaconData
import kotlin.math.pow

data class Position(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val timestamp: Long = System.currentTimeMillis()
)

interface PositionCalculator {
    fun calculatePosition(beacons: List<BeaconData>): Position?
}

// Basic weighted centroid implementation
class WeightedCentroidCalculator : PositionCalculator {
    override fun calculatePosition(beacons: List<BeaconData>): Position? {
        if (beacons.isEmpty()) return null

        var totalWeight = 0.0
        var weightedLat = 0.0
        var weightedLon = 0.0

        beacons.forEach { beacon ->
            // Use inverse square of distance as weight
            val weight = 1.0 / (beacon.distance.pow(2))

            weightedLat += beacon.latitude * weight
            weightedLon += beacon.longitude * weight
            totalWeight += weight
        }

        if (totalWeight == 0.0) return null

        return Position(
            latitude = weightedLat / totalWeight,
            longitude = weightedLon / totalWeight,
            accuracy = calculateAccuracy(beacons)
        )
    }

    private fun calculateAccuracy(beacons: List<BeaconData>): Double {
        // Simple accuracy estimation based on number of beacons and their distances
        return if (beacons.isEmpty()) Double.POSITIVE_INFINITY
        else beacons.sumOf { it.distance } / beacons.size
    }
}

// Factory for getting position calculator implementation
object PositionCalculatorFactory {
    fun getCalculator(type: CalculatorType = CalculatorType.WEIGHTED_CENTROID): PositionCalculator {
        return when (type) {
            CalculatorType.WEIGHTED_CENTROID -> WeightedCentroidCalculator()
            // Add more implementations as needed
        }
    }

    enum class CalculatorType {
        WEIGHTED_CENTROID
        // Add more types as needed
    }
}
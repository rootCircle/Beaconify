package com.iiitl.locateme.utils.graph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.iiitl.locateme.utils.beacon.BeaconData
import kotlin.math.max
import kotlin.math.min

data class GraphPosition(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double
)

class BeaconVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var beacons: List<BeaconData> = emptyList()
    private var currentPosition: GraphPosition? = null

    private var minLat = 0.0
    private var maxLat = 0.0
    private var minLon = 0.0
    private var maxLon = 0.0

    private val padding = 50f
    private val gridLines = 10

    companion object {
        private const val TAG = "BeaconVisualizerView"
        private const val BEACON_RADIUS = 20f
        private const val DEVICE_RADIUS = 25f
        private const val TEXT_SIZE = 30f
    }

    fun updateData(latitude: Double?, longitude: Double?, accuracy: Double?, newBeacons: List<BeaconData>) {
        currentPosition = if (latitude != null && longitude != null && accuracy != null) {
            GraphPosition(latitude, longitude, accuracy)
        } else {
            null
        }
        this.beacons = newBeacons
        calculateBounds()
        invalidate()
    }

    private fun calculateBounds() {
        if (beacons.isEmpty() && currentPosition == null) return

        // Initialize with first value or reset to extremes
        minLat = beacons.firstOrNull()?.latitude ?: Double.POSITIVE_INFINITY
        maxLat = beacons.firstOrNull()?.latitude ?: Double.NEGATIVE_INFINITY
        minLon = beacons.firstOrNull()?.longitude ?: Double.POSITIVE_INFINITY
        maxLon = beacons.firstOrNull()?.longitude ?: Double.NEGATIVE_INFINITY

        // Update bounds with beacon positions
        beacons.forEach { beacon ->
            minLat = min(minLat, beacon.latitude)
            maxLat = max(maxLat, beacon.latitude)
            minLon = min(minLon, beacon.longitude)
            maxLon = max(maxLon, beacon.longitude)
        }

        // Include current position in bounds
        currentPosition?.let { pos ->
            minLat = min(minLat, pos.latitude)
            maxLat = max(maxLat, pos.latitude)
            minLon = min(minLon, pos.longitude)
            maxLon = max(maxLon, pos.longitude)
        }

        // Add padding to bounds
        val latPadding = (maxLat - minLat) * 0.1
        val lonPadding = (maxLon - minLon) * 0.1
        minLat -= latPadding
        maxLat += latPadding
        minLon -= lonPadding
        maxLon += lonPadding

        Log.d(TAG, "Bounds: Lat($minLat, $maxLat), Lon($minLon, $maxLon)")
    }

    private fun convertToScreenX(longitude: Double): Float {
        return if (maxLon == minLon) {
            width / 2f
        } else {
            padding + (width - 2 * padding) *
                    ((longitude - minLon) / (maxLon - minLon)).toFloat()
        }
    }

    private fun convertToScreenY(latitude: Double): Float {
        return if (maxLat == minLat) {
            height / 2f
        } else {
            height - padding - (height - 2 * padding) *
                    ((latitude - minLat) / (maxLat - minLat)).toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(Color.WHITE)

        // Draw grid
        drawGrid(canvas)

        // Draw coordinate labels
        drawCoordinateLabels(canvas)

        // Draw beacons
        drawBeacons(canvas)

        // Draw current position
        drawCurrentPosition(canvas)

        // Draw legend
        drawLegend(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        paint.apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Draw grid lines
        for (i in 0..gridLines) {
            val x = padding + (width - 2 * padding) * i / gridLines
            val y = padding + (height - 2 * padding) * i / gridLines

            // Vertical lines
            canvas.drawLine(x, padding, x, height - padding, paint)
            // Horizontal lines
            canvas.drawLine(padding, y, width - padding, y, paint)
        }
    }

    private fun drawCoordinateLabels(canvas: Canvas) {
        paint.apply {
            color = Color.BLACK
            textSize = TEXT_SIZE
            textAlign = Paint.Align.RIGHT
        }

        // Draw latitude labels
        for (i in 0..4) {
            val lat = minLat + (maxLat - minLat) * i / 4
            val y = convertToScreenY(lat)
            canvas.drawText(
                String.format("%.6f°", lat),
                padding - 5f,
                y + TEXT_SIZE/3,
                paint
            )
        }

        // Draw longitude labels
        paint.textAlign = Paint.Align.CENTER
        for (i in 0..4) {
            val lon = minLon + (maxLon - minLon) * i / 4
            val x = convertToScreenX(lon)
            canvas.drawText(
                String.format("%.6f°", lon),
                x,
                height - padding + TEXT_SIZE + 5f,
                paint
            )
        }
    }

    private fun drawBeacons(canvas: Canvas) {
        paint.apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = TEXT_SIZE
        }

        beacons.forEach { beacon ->
            val x = convertToScreenX(beacon.longitude)
            val y = convertToScreenY(beacon.latitude)

            // Draw beacon circle
            paint.color = Color.BLUE
            canvas.drawCircle(x, y, BEACON_RADIUS, paint)

            // Draw beacon label
            paint.color = Color.BLACK
            canvas.drawText("B${beacon.minor}", x, y - BEACON_RADIUS - 5f, paint)

            // Draw distance
            canvas.drawText(
                String.format("%.1fm", beacon.distance),
                x, y + BEACON_RADIUS + TEXT_SIZE,
                paint
            )
        }
    }

    private fun drawCurrentPosition(canvas: Canvas) {
        currentPosition?.let { position ->
            val x = convertToScreenX(position.longitude)
            val y = convertToScreenY(position.latitude)

            // Draw accuracy circle
            paint.apply {
                style = Paint.Style.STROKE
                color = Color.RED
                alpha = 50
            }

            val accuracyRadius = position.accuracy.toFloat() *
                    ((width - 2 * padding) / (maxLon - minLon)).toFloat()
            canvas.drawCircle(x, y, accuracyRadius.coerceAtMost(width/4f), paint)

            // Draw position marker
            paint.apply {
                style = Paint.Style.FILL
                color = Color.RED
                alpha = 255
            }
            canvas.drawCircle(x, y, DEVICE_RADIUS, paint)

            // Draw "Your Position" label
            paint.apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Your Position",
                x,
                y - DEVICE_RADIUS - 10f,
                paint
            )
        }
    }

    private fun drawLegend(canvas: Canvas) {
        val legendX = padding + 10f
        val legendY = padding + 30f
        val spacing = 40f

        paint.apply {
            textAlign = Paint.Align.LEFT
            textSize = TEXT_SIZE
        }

        // Beacon legend
        paint.color = Color.BLUE
        canvas.drawCircle(legendX, legendY, BEACON_RADIUS, paint)
        paint.color = Color.BLACK
        canvas.drawText("Beacons", legendX + 40f, legendY + 10f, paint)

        // Device position legend
        paint.color = Color.RED
        canvas.drawCircle(legendX, legendY + spacing, DEVICE_RADIUS, paint)
        paint.color = Color.BLACK
        canvas.drawText("Your Position", legendX + 40f, legendY + spacing + 10f, paint)

        // Accuracy circle legend
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.alpha = 50
        canvas.drawCircle(legendX, legendY + spacing * 2, DEVICE_RADIUS, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.alpha = 255
        canvas.drawText("Accuracy", legendX + 40f, legendY + spacing * 2 + 10f, paint)
    }
}
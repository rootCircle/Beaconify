package com.iiitl.locateme.utils.graph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
//import android.util.Log
import android.view.View
import androidx.core.view.marginLeft
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
    private var plotRect: RectF = RectF()

    private var minLat = 0.0
    private var maxLat = 0.0
    private var minLon = 0.0
    private var maxLon = 0.0

    private val padding = 75f
    private val gridLines = 10

    companion object {
        private const val TAG = "BeaconVisualizerView"
        private const val BEACON_RADIUS = 15f
        private const val DEVICE_RADIUS = 20f
        private const val TEXT_SIZE = 25f
        private const val LEGEND_ITEM_SPACING = 120f
        private const val LEGEND_ICON_SIZE = 12f
        private const val LEGEND_TEXT_OFFSET = 25f
        private const val LEGEND_VERTICAL_PADDING = 15f
        private const val LEGEND_HEIGHT = 60f
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

        // Initialize bounds
        if (beacons.isNotEmpty()) {
            // Use first beacon's position
            minLat = beacons[0].latitude
            maxLat = beacons[0].latitude
            minLon = beacons[0].longitude
            maxLon = beacons[0].longitude

            // If only one beacon, add a small offset to create valid bounds
            if (beacons.size == 1) {
                val latOffset = 0.0001 // Approximately 10 meters
                val lonOffset = 0.0001
                minLat -= latOffset
                maxLat += latOffset
                minLon -= lonOffset
                maxLon += lonOffset
            } else {
                // Update bounds with remaining beacons
                beacons.drop(1).forEach { beacon ->
                    minLat = min(minLat, beacon.latitude)
                    maxLat = max(maxLat, beacon.latitude)
                    minLon = min(minLon, beacon.longitude)
                    maxLon = max(maxLon, beacon.longitude)
                }
            }
        } else {
            // If no beacons, use current position
            currentPosition?.let { pos ->
                minLat = pos.latitude
                maxLat = pos.latitude
                minLon = pos.longitude
                maxLon = pos.longitude
            }
        }

        // Include current position in bounds if it exists
        currentPosition?.let { pos ->
            minLat = min(minLat, pos.latitude)
            maxLat = max(maxLat, pos.latitude)
            minLon = min(minLon, pos.longitude)
            maxLon = max(maxLon, pos.longitude)
        }

        // Add padding to bounds
        val latPadding = max((maxLat - minLat) * 0.1, 0.0001)
        val lonPadding = max((maxLon - minLon) * 0.1, 0.0001)
        minLat -= latPadding
        maxLat += latPadding
        minLon -= lonPadding
        maxLon += lonPadding
    }

    private fun convertToScreenX(longitude: Double): Float {
        return plotRect.left + (plotRect.width() * (longitude - minLon) / (maxLon - minLon)).toFloat()
    }

    private fun convertToScreenY(latitude: Double): Float {
        return plotRect.bottom - (plotRect.height() * (latitude - minLat) / (maxLat - minLat)).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        // Calculate plotting area
        plotRect = RectF(
            padding + TEXT_SIZE * 3, // Left padding for latitude labels
            padding,                 // Top padding
            width - padding,         // Right padding
            height - padding - LEGEND_HEIGHT - TEXT_SIZE // Bottom padding for legend and longitude labels
        )

        drawGrid(canvas)
        drawCoordinateLabels(canvas)
        drawBeacons(canvas)
        drawCurrentPosition(canvas)
        drawBottomLegend(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        paint.apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Draw vertical grid lines
        for (i in 0..gridLines) {
            val x = plotRect.left + (plotRect.width() * i / gridLines)
            canvas.drawLine(x, plotRect.top, x, plotRect.bottom, paint)
        }

        // Draw horizontal grid lines
        for (i in 0..gridLines) {
            val y = plotRect.top + (plotRect.height() * i / gridLines)
            canvas.drawLine(plotRect.left, y, plotRect.right, y, paint)
        }
    }

    private fun drawCoordinateLabels(canvas: Canvas) {
        val numLabels = 4
        val maxIndex = numLabels - 1

        paint.apply {
            style = Paint.Style.FILL
            color = Color.BLACK
            textSize = TEXT_SIZE
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw latitude labels
        for (i in 0..maxIndex) {
            val lat = minLat + (maxLat - minLat) * (maxIndex - i) / maxIndex
            val y = plotRect.top + (plotRect.height() * i / maxIndex)
            canvas.drawText(
                String.format("%.6f°", lat),
                plotRect.left - 10f,
                y + TEXT_SIZE/3,
                paint
            )
        }

        // Draw longitude labels
        paint.textAlign = Paint.Align.CENTER
        for (i in 0..maxIndex) {
            val lon = minLon + (maxLon - minLon) * i / maxIndex
            val x = plotRect.left + (plotRect.width() * i / maxIndex)
            canvas.drawText(
                String.format("%.6f°", lon),
                x,
                plotRect.bottom + TEXT_SIZE + 10f,
                paint
            )
        }
    }

    private fun drawBeacons(canvas: Canvas) {
        paint.apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = TEXT_SIZE * 0.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)  // Make text bold
        }

        beacons.forEachIndexed { index, beacon ->
            val x = convertToScreenX(beacon.longitude)
            val y = convertToScreenY(beacon.latitude)

            // Draw beacon circle
            paint.apply {
                color = Color.BLUE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(x, y, BEACON_RADIUS, paint)

            // Draw white background for text to improve readability
            paint.apply {
                color = Color.WHITE
                strokeWidth = 8f  // Adjust the stroke width for the background
                style = Paint.Style.FILL_AND_STROKE
            }

            // Draw beacon label with sequential number and white background
            canvas.drawText("B${index + 1}", x, y - BEACON_RADIUS - 5f, paint)
            canvas.drawText(
                String.format("%.1fm", beacon.distance),
                x, y + BEACON_RADIUS + TEXT_SIZE * 0.8f,
                paint
            )
            canvas.drawText(
                "${beacon.rssi} dBm",
                x, y + BEACON_RADIUS + TEXT_SIZE * 1.6f,
                paint
            )

            // Draw the actual text in black on top
            paint.apply {
                color = Color.BLACK
                strokeWidth = 0f
                style = Paint.Style.FILL
            }
            canvas.drawText("B${index + 1}", x, y - BEACON_RADIUS - 5f, paint)
            canvas.drawText(
                String.format("%.1fm", beacon.distance),
                x, y + BEACON_RADIUS + TEXT_SIZE * 0.8f,
                paint
            )
            canvas.drawText(
                "${beacon.rssi} dBm",
                x, y + BEACON_RADIUS + TEXT_SIZE * 1.6f,
                paint
            )
        }

        // Reset typeface for other drawing operations
        paint.typeface = Typeface.DEFAULT
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
                    (plotRect.width() / (maxLon - minLon)).toFloat()
            canvas.drawCircle(x, y, accuracyRadius.coerceAtMost(plotRect.width()/4f), paint)

            // Draw position marker
            paint.apply {
                style = Paint.Style.FILL
                color = Color.RED
                alpha = 255
            }
            canvas.drawCircle(x, y, DEVICE_RADIUS, paint)

            // Draw label
            paint.apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
                textSize = TEXT_SIZE * 0.8f
            }
            canvas.drawText(
                "Your Position",
                x,
                y - DEVICE_RADIUS - 5f,
                paint
            )
        }
    }

    private fun drawBottomLegend(canvas: Canvas) {
        val legendItems = listOf(
            LegendItem("Beacons", Color.BLUE, BEACON_RADIUS),
            LegendItem("Your Position", Color.RED, DEVICE_RADIUS),
            LegendItem("Accuracy Range", Color.RED, DEVICE_RADIUS, true)
        )

        paint.apply {
            textSize = TEXT_SIZE * 0.8f
            textAlign = Paint.Align.LEFT
        }

        val availableWidth = width - 2 * padding
        val itemHeight = TEXT_SIZE * 1.5f
        val bottomPadding = TEXT_SIZE * 2f  // Added bottom padding
        var currentX = padding
        var currentY = height - LEGEND_VERTICAL_PADDING - TEXT_SIZE
        var maxYOffset = 0f

        // First pass to check if we need a second row
        var needsSecondRow = false
        var firstRowWidth = 0f
        legendItems.forEach { item ->
            val itemWidth = paint.measureText(item.text) + LEGEND_TEXT_OFFSET + LEGEND_ICON_SIZE * 3
            if (firstRowWidth + itemWidth > availableWidth) {
                needsSecondRow = true
            } else {
                firstRowWidth += itemWidth + LEGEND_ITEM_SPACING
            }
        }

        // Adjust starting X position to center first row
        currentX = if (needsSecondRow) {
            padding
        } else {
            (width - firstRowWidth + LEGEND_ITEM_SPACING) / 2
        }

        // Adjust starting Y position if we know we'll need a second row
        if (needsSecondRow) {
            currentY -= itemHeight
        }

        legendItems.forEach { item ->
            val itemWidth = paint.measureText(item.text) + LEGEND_TEXT_OFFSET + LEGEND_ICON_SIZE * 3

            // Check if we need to move to next line
            if (currentX + itemWidth > width - padding) {
                currentX = padding
                currentY += itemHeight
                maxYOffset = itemHeight
            }

            // Draw icon
            paint.apply {
                style = if (item.isOutline) Paint.Style.STROKE else Paint.Style.FILL
                color = item.color
                alpha = if (item.isOutline) 50 else 255
                strokeWidth = 2f
            }

            val iconX = currentX + LEGEND_ICON_SIZE
            canvas.drawCircle(iconX, currentY, LEGEND_ICON_SIZE, paint)

            // Draw text
            paint.apply {
                style = Paint.Style.FILL
                color = Color.BLACK
                alpha = 255
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText(
                item.text,
                iconX + LEGEND_TEXT_OFFSET,
                currentY + TEXT_SIZE/3,
                paint
            )

            currentX += itemWidth + LEGEND_ITEM_SPACING
        }

        // Adjust plotRect if we used a second row
        if (maxYOffset > 0) {
            plotRect.bottom -= (maxYOffset + bottomPadding)
        }
    }

    private data class LegendItem(
        val text: String,
        val color: Int,
        val radius: Float,
        val isOutline: Boolean = false
    )
}
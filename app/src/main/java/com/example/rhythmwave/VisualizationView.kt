package com.example.rhythmwave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.log10

class VisualizationView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 30f
    }
    private var fftData = ByteArray(0)

    fun updateVisualizer(fft: ByteArray) {
        fftData = fft
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fftData.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val segmentWidth = width / fftData.size

        for (i in 0 until fftData.size) {
            val magnitude = fftData[i].toInt()
            val x = i * segmentWidth
            val y = height - (magnitude / 256f * height)
            canvas.drawLine(x, height, x, y, paint)
        }
    }
}

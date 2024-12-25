package com.example.rhythmwave

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class VisualizationView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }
    private var fftData = ByteArray(0)
    private val waveAnimator = ValueAnimator.ofFloat(0f, 2f * PI.toFloat()).apply {
        duration = 10000
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            invalidate()
        }
        start()
    }

    fun updateVisualizer(fft: ByteArray) {
        fftData = fft
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fftData.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val cx = width / 2
        val cy = height / 2
        val maxRadius = minOf(cx, cy) * 0.8f

        val waveOffset = waveAnimator.animatedValue as Float

        drawWaveCircle(canvas, cx, cy, maxRadius, waveOffset, 0, fftData.size / 3, Color.RED, Color.MAGENTA)
        drawWaveCircle(canvas, cx, cy, maxRadius, waveOffset + PI.toFloat() / 2, fftData.size / 3, 2 * fftData.size / 3, Color.YELLOW, Color.YELLOW)
        drawWaveCircle(canvas, cx, cy, maxRadius, waveOffset + PI.toFloat(), 2 * fftData.size / 3, fftData.size, Color.GREEN, Color.CYAN)
    }

    private fun drawWaveCircle(canvas: Canvas, cx: Float, cy: Float, maxRadius: Float, waveOffset: Float, startIndex: Int, endIndex: Int, startColor: Int, endColor: Int) {
        val path = Path()
        val wavePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }

        // Создаем градиент для каждого круга
        val gradient = LinearGradient(
            cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius,
            startColor, endColor, Shader.TileMode.CLAMP
        )
        wavePaint.shader = gradient

        for (i in startIndex until endIndex) {
            val magnitude = fftData[i].toInt() + 128 // Преобразуем в положительное значение
            val normalizedMagnitude = magnitude / 256f
            val radius = maxRadius * normalizedMagnitude * 2 // Увеличиваем размер колебаний
            val angle = (i * 2 * PI / fftData.size).toFloat() + waveOffset

            val waveAmplitude = 30f // Амплитуда волны
            val waveFrequency = 10f // Частота волны
            val waveValue = sin(angle * waveFrequency) * waveAmplitude

            val x = cx + (radius + waveValue) * cos(angle)
            val y = cy + (radius + waveValue) * sin(angle)

            if (i == startIndex) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, wavePaint)
    }

}

//package com.example.rhythmwave
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.LinearGradient
//import android.graphics.Shader
//import android.util.AttributeSet
//import android.view.View
//
//class VisualizationView(context: Context, attrs: AttributeSet) : View(context, attrs) {
//    private val paint = Paint().apply {
//        strokeWidth = 20f
//    }
//    private var fftData = ByteArray(0)
//
//    fun updateVisualizer(fft: ByteArray) {
//        fftData = fft
//        invalidate()
//    }
//
//    @SuppressLint("DrawAllocation")
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        if (fftData.isEmpty()) return
//
//        val width = width.toFloat()
//        val height = height.toFloat()
//        val segmentWidth = width / fftData.size
//
//        val gradient = LinearGradient(
//            0f, 0f, width, height,
//            Color.RED, Color.BLUE, Shader.TileMode.CLAMP
//        )
//        paint.shader = gradient
//
//        for (i in 0 until fftData.size) {
//            val magnitude = fftData[i].toInt()
//            val x = i * segmentWidth
//            val y = height - (magnitude / 256f * height)
//            canvas.drawLine(x, height, x, y, paint)
//        }
//    }
//}

//package com.example.rhythmwave
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.RectF
//import android.util.AttributeSet
//import android.view.View
//import kotlin.math.log10
//
//class VisualizationView(context: Context, attrs: AttributeSet) : View(context, attrs) {
//    private val paint = Paint().apply {
//        strokeWidth = 30f
//    }
//    private var fftData = ByteArray(0)
//
//    fun updateVisualizer(fft: ByteArray) {
//        fftData = fft
//        invalidate()
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        if (fftData.isEmpty()) return
//
//        val width = width.toFloat()
//        val height = height.toFloat()
//        val segmentWidth = width / fftData.size
//
//        for (i in 0 until fftData.size) {
//            val magnitude = fftData[i].toInt()
//            val x = i * segmentWidth
//            val y = height - (log10(magnitude.toDouble() + 1) / log10(256.0) * height).toFloat()
//
//            val color = when {
//                i < fftData.size / 3 -> Color.RED  // низкие частоты
//                i < 2 * fftData.size / 3 -> Color.YELLOW  // средние частоты
//                else -> Color.GREEN  // высокие частоты
//            }
//            paint.color = color
//
//            canvas.drawRect(RectF(x, y, x + segmentWidth, height), paint)
//        }
//    }
//}
package com.example.rhythmwave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class VisualizationView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        style = Paint.Style.FILL
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
        val cx = width / 2
        val cy = height / 2
        val maxRadius = min(cx, cy) * 0.8f

        for (i in 0 until fftData.size) {
            val magnitude = fftData[i].toInt() + 128 // Преобразуем в положительное значение
            val normalizedMagnitude = magnitude / 256f
            val radius = maxRadius * normalizedMagnitude
            val angle = (i * 2 * PI / fftData.size).toFloat()

            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)

            // Задаем цвет в зависимости от частоты
            paint.color = when {
                i < fftData.size / 3 -> Color.RED
                i < 2 * fftData.size / 3 -> Color.YELLOW
                else -> Color.GREEN
            }

            // Рисуем круги
            canvas.drawCircle(x, y, 20f, paint)
        }

        // Случайные эффекты (взрывы) для глубины
        drawExplosion(canvas, cx, cy)
    }

    private fun drawExplosion(canvas: Canvas, cx: Float, cy: Float) {
        val explosionCount = (1..5).random() // Случайное количество "взрывов"
        val explosionPaint = Paint().apply { color = Color.CYAN }

        for (i in 0 until explosionCount) {
            val x = Random.nextInt((cx - 50).toInt(), (cx + 50).toInt()).toFloat()
            val y = Random.nextInt((cy - 50).toInt(), (cy + 50).toInt()).toFloat()
            val radius = Random.nextInt(10, 61).toFloat()
            canvas.drawCircle(x, y, radius, explosionPaint)
        }
    }
}

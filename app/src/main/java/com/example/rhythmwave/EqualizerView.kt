package com.example.rhythmwave

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.random.Random

class EqualizerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.neon_purple)
        strokeWidth = 10f
    }

    private val bars = MutableList(3) { Random.nextInt(10, 100) }
    private val barWidth = 20f
    private val barSpacing = 5f

    init {
        startAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val height = height.toFloat()
        Log.d("MyLog", height.toString())
        val width = width.toFloat()
        val startX = (width - (barWidth + barSpacing) * bars.size + barSpacing)

        for (i in bars.indices) {
            val barHeight = bars[i] * height / 100
            canvas.drawLine(
                startX + i * (barWidth + barSpacing),
                height,
                startX + i * (barWidth + barSpacing),
                height - barHeight,
                paint
            )
        }
    }

    private fun startAnimation() {
        postDelayed({
            updateBars()
            invalidate()
            startAnimation()
        }, 100)
    }

    private fun updateBars() {
        for (i in bars.indices) {
            bars[i] = Random.nextInt(10, 100)
        }
    }
}
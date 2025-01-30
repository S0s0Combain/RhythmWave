package com.example.rhythmwave

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import kotlin.math.log10
import kotlin.math.max

class AudioVisualizer(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val smoothingFactor = 0.2f
    private val barsCount = 54
    private val barWidth: Float get() = width / (barsCount * 2f)

    private val piePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var magnitudes = floatArrayOf()
    private val data = mutableListOf<RectF>()

    private var visualizer: Visualizer? = null
    private val colors = intArrayOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.MAGENTA, Color.LTGRAY, Color.DKGRAY,
        Color.WHITE, Color.parseColor("#FF6F00"), Color.parseColor("#D50000"),
        Color.parseColor("#00B0FF"), Color.parseColor("#FFEA00"),
        Color.parseColor("#00E676"), Color.parseColor("#FF3D00"),
        Color.parseColor("#6200EA"), Color.parseColor("#304FFE")
    )

    private var visualizerAnimator: ValueAnimator? = null

    private val dataCaptureListener = object : Visualizer.OnDataCaptureListener {

        override fun onFftDataCapture(v: Visualizer?, data: ByteArray?, sampleRate: Int) {
            data?.let {
                magnitudes = convertFFTtoMagnitudes(data)
                visualizeData()
            }
        }

        override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, sampleRate: Int) = Unit
    }

    init {
        startColorAnimation()
    }

    private fun startColorAnimation() {
        visualizerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val colorIndex = (fraction * (colors.size - 1)).toInt()
                piePaint.shader = LinearGradient(
                    0f, 0f, barWidth * barsCount, height.toFloat(),
                    colors[colorIndex], colors[(colorIndex + 1) % colors.size],
                    Shader.TileMode.REPEAT
                )
                invalidate()
            }
            start()
        }
    }

    fun getPathMedia(exoPlayer: ExoPlayer) {
        if (visualizer != null) return
        visualizer = Visualizer(exoPlayer.audioSessionId)
            .apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    dataCaptureListener,
                    Visualizer.getMaxCaptureRate() * 2 / 3,
                    false,
                    true
                )
                enabled = true
            }
    }

    fun visualizeData() {
        data.clear()

        for (i in 0 until barsCount) {
            val segmentSize = (magnitudes.size / barsCount.toFloat())
            val segmentStart = i * segmentSize
            val segmentEnd = segmentStart + segmentSize

            var sum = 0f
            for (j in segmentStart.toInt() until segmentEnd.toInt()) {
                sum += magnitudes[j]
            }
            val amp = sum
                .run { this / segmentSize }
                .run { this * height }
                .run { max(this, barWidth) }

            val horizontalOffset = barWidth / 2

            val startX = barWidth * i * 2
            val endX = startX + barWidth

            val midY = height / 2
            val startY = midY - amp / 2
            val endY = midY + amp / 2

            data.add(
                RectF(startX + horizontalOffset, startY, endX + horizontalOffset, endY)
            )
        }
        invalidate()
    }

    private val maxMagnitude = calculateMagnitude(128f, 128f)

    private fun convertFFTtoMagnitudes(fft: ByteArray): FloatArray {
        if (fft.isEmpty()) {
            return floatArrayOf()
        }

        val n: Int = fft.size / FFT_NEEDED_PORTION
        val curMagnitudes = FloatArray(n / 2)

        var prevMagnitudes = magnitudes
        if (prevMagnitudes.isEmpty()) {
            prevMagnitudes = FloatArray(n)
        }

        for (k in 0 until n / 2 - 1) {
            val index = k * FFT_STEP + FFT_OFFSET
            val real: Byte = fft[index]
            val imaginary: Byte = fft[index + 1]

            val curMagnitude = calculateMagnitude(real.toFloat(), imaginary.toFloat())
            curMagnitudes[k] = curMagnitude + (prevMagnitudes[k] - curMagnitude) * smoothingFactor
        }
        return curMagnitudes.map { it / maxMagnitude }.toFloatArray()
    }

    private fun calculateMagnitude(r: Float, i: Float) = if (i == 0f && r == 0f) 0f else 10 * log10(r * r + i * i)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        visualizeData()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            data.forEach {
                drawRoundRect(it, 25f, 25f, piePaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        visualizer?.release()
        visualizerAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val FFT_STEP = 2
        private const val FFT_OFFSET = 2
        private const val FFT_NEEDED_PORTION = 3
    }
}
package com.example.rhythmwave

import android.annotation.SuppressLint
import android.media.audiofx.Visualizer
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.yalantis.waves.util.Horizon

class PlayerFragment : Fragment(), GestureDetector.OnGestureListener {
    private lateinit var buttonDown: ImageButton
    private lateinit var titleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var fragmentSeekBar: SeekBar
    private lateinit var gestureDetector: GestureDetector
    private lateinit var trackControlLayout: ConstraintLayout
    private lateinit var visualizationView: GLSurfaceView
    private lateinit var horizon: Horizon
    private lateinit var visualizer: Visualizer
    var musicService: MusicService? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            val currentPosition = musicService?.getCurrentPosition() ?: 0
//            fragmentSeekBar.progress = currentPosition
//            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        visualizationView = view.findViewById(R.id.visualizationView)

        // Initialize Horizon
        horizon = Horizon(
            visualizationView,
            resources.getColor(R.color.primary_background),
            Visualizer.getMaxCaptureRate(),
            1,
            16
        )

        // Initialize Visualizer
        visualizer = Visualizer(musicService?.getAudioSessionId() ?: 0)
        visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
        visualizer.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
            override fun onWaveFormDataCapture(
                visualizer: Visualizer?,
                bytes: ByteArray?,
                samplingRate: Int
            ) {
//                bytes?.let { horizon.updateView(bytes) }
            }

            override fun onFftDataCapture(
                visualizer: Visualizer?,
                bytes: ByteArray?,
                samplingRate: Int
            ) {
                bytes?.let { horizon.updateView(bytes) }
            }
        }, Visualizer.getMaxCaptureRate(), true, true)
        visualizer.enabled = true

        return view
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        //Log.d("MyLog", (musicService == null).toString())
//        val currentTrack = musicService?.getCurrentTrack()
//        if (currentTrack != null) {
//            updateTrackInfo(currentTrack)
//            updateSeekbar(musicService?.getCurrentPosition() ?: 0, currentTrack.duration)
//        }
//        // Initialize Visualizer here
//        musicService?.let {
//            visualizer = Visualizer(it.getAudioSessionId()).apply {
//                captureSize = 512
//                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
//                measurementMode = Visualizer.MEASUREMENT_MODE_NONE
//                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
//                    override fun onWaveFormDataCapture(
//                        visualizer: Visualizer?,
//                        waveform: ByteArray?,
//                        samplingRate: Int
//                    ) {
//                        horizon.updateView(waveform)
//                    }
//
//                    override fun onFftDataCapture(
//                        visualizer: Visualizer?,
//                        fft: ByteArray?,
//                        samplingRate: Int
//                    ) {
////                        fft?.let { visualizationView.updateVisualizer(it) }
//                    }
//                }, Visualizer.getMaxCaptureRate() / 2, true, true)
//                enabled = true
//            }
//        }
//    }

    override fun onResume() {
        super.onResume()
        handler.post(updateSeekBarRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    fun collapseFragment() {
        val fragmentContainer = requireActivity().findViewById<FrameLayout>(R.id.fragmentContainer)
        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                requireActivity().supportFragmentManager.popBackStack()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        fragmentContainer.startAnimation(animation)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 != null && e2 != null) {
            val deltaY = e2.y - e1.y
            if (deltaY > 0) {
                collapseFragment()
                return true
            }
        }
        return false
    }

    fun updateTrackInfo(track: Track) {
//        titleTextView.text = track.title
//        artistTextView.text = track.artist
    }

    fun updateSeekbar(position: Int, duration: Int) {
//        fragmentSeekBar.max = duration
//        fragmentSeekBar.progress = position
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer?.release()
    }
}
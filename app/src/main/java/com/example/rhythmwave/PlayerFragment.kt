package com.example.rhythmwave

import android.annotation.SuppressLint
import android.gesture.Gesture
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView

class PlayerFragment : Fragment(), GestureDetector.OnGestureListener {
    private lateinit var buttonDown: ImageButton
    private lateinit var titleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var fragmentSeekBar: SeekBar
    private lateinit var gestureDetector: GestureDetector
    private lateinit var visualizationView: VisualizationView
    private var visualizer: Visualizer? = null
    private var musicService: MusicService? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            val currentPosition = musicService?.getCurrentPosition() ?: 0
            fragmentSeekBar.progress = currentPosition
            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        buttonDown = view.findViewById(R.id.buttonDown)
        titleTextView = view.findViewById(R.id.titleTextView)
        artistTextView = view.findViewById(R.id.artistTextView)
        prevButton = view.findViewById(R.id.prevButton)
        pauseButton = view.findViewById(R.id.pauseButton)
        nextButton = view.findViewById(R.id.nextButton)
        fragmentSeekBar = view.findViewById(R.id.fragmentSeekBar)
        visualizationView = view.findViewById(R.id.visualizationView)

        buttonDown.setOnClickListener { collapseFragment() }
        prevButton.setOnClickListener { musicService?.previousTrack() }
        pauseButton.setOnClickListener { musicService?.pauseTrack() }
        nextButton.setOnClickListener { musicService?.nextTrack() }
        fragmentSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        gestureDetector = GestureDetector(context, this)
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        musicService = (activity as MainActivity).musicService
        visualizer = Visualizer(musicService?.getAudioSessionId()!!).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    // Not used
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    fft?.let { visualizationView.updateVisualizer(it) }
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)
            enabled = true
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentTrack = musicService?.getCurrentTrack()
        if (currentTrack != null) {
            updateTrackInfo(currentTrack)
            updateSeekbar(musicService?.getCurrentPosition() ?: 0, currentTrack.duration)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateSeekBarRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    private fun collapseFragment() {
        val fragmentContainer = requireActivity().findViewById<FrameLayout>(R.id.fragmentContainer)
        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                fragmentContainer.visibility = View.GONE
                parentFragmentManager.popBackStack()
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
        titleTextView.text = track.title
        artistTextView.text = track.artist
    }

    fun updateSeekbar(position: Int, duration: Int) {
        fragmentSeekBar.max = duration
        fragmentSeekBar.progress = position
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer?.release()
    }
}

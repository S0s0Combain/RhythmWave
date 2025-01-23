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
import android.widget.SeekBar.OnSeekBarChangeListener
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
            fragmentSeekBar.progress = currentPosition
            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        (activity as MainActivity).trackControlLayout.visibility = View.GONE
        buttonDown = view.findViewById(R.id.buttonDown)
        titleTextView = view.findViewById(R.id.titleTextView)
        artistTextView = view.findViewById(R.id.artistTextView)
        prevButton = view.findViewById(R.id.prevButton)
        pauseButton = view.findViewById(R.id.pauseButton)
        nextButton = view.findViewById(R.id.nextButton)
        fragmentSeekBar = view.findViewById(R.id.fragmentSeekBar)
        visualizationView = view.findViewById(R.id.visualizationView)
        trackControlLayout = (activity as MainActivity).findViewById(R.id.trackControlLayout)

        buttonDown.setOnClickListener { collapseFragment() }
        prevButton.setOnClickListener {
            musicService?.previousTrack()
            pauseButton.setImageResource(R.drawable.baseline_pause_circle_24)
        }
        pauseButton.setOnClickListener {
            if (musicService?.isPlaying() == true) {
                pauseButton.setImageResource(R.drawable.ic_play)
            } else {
                pauseButton.setImageResource(R.drawable.baseline_pause_circle_24)
            }
            musicService?.togglePlayPause()
        }
        nextButton.setOnClickListener {
            musicService?.nextTrack()
            pauseButton.setImageResource(R.drawable.baseline_pause_circle_24)
        }
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
        buttonDown = view.findViewById(R.id.buttonDown)
        buttonDown.setOnClickListener { collapseFragment() }
        if (musicService?.isPlaying() == true) {
            pauseButton.setImageResource(R.drawable.baseline_pause_circle_24)
        } else {
            pauseButton.setImageResource(R.drawable.ic_play)
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

        visualizationView = view.findViewById(R.id.visualizationView)

        horizon = Horizon(
            visualizationView,
            resources.getColor(R.color.primary_background),
            Visualizer.getMaxCaptureRate(),
            1,
            16
        )

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
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateSeekBarRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    fun collapseFragment() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                view?.visibility = View.VISIBLE
                (activity as MainActivity).showTrackControl(musicService?.getCurrentTrack()!!)
            }

            override fun onAnimationEnd(animation: Animation?) {
                requireActivity().supportFragmentManager.popBackStack()
                view?.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        view?.startAnimation(animation)
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
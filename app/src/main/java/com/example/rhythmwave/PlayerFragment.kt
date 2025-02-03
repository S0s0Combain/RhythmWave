package com.example.rhythmwave

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.Image
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
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private lateinit var audioVisualizer: AudioVisualizer
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var totalDurationTextView: TextView
    private lateinit var trackImageView: ImageView
    private lateinit var equalizerImageButton: ImageButton
    private lateinit var favoriteButton: ImageButton
    private lateinit var shuffleButton: ImageButton
    private var trackList: List<Track>? = null
    var musicService: MusicService? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            val currentPosition = musicService?.getCurrentPosition() ?: 0
            fragmentSeekBar.progress = currentPosition
            elapsedTimeTextView.text = formatTime(currentPosition)
            handler.postDelayed(this, 1000)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        trackList = musicService?.getTrackList()
        (activity as MainActivity).trackControlLayout.visibility = View.GONE
        buttonDown = view.findViewById(R.id.buttonDown)
        titleTextView = view.findViewById(R.id.titleTextView)
        artistTextView = view.findViewById(R.id.artistTextView)
        prevButton = view.findViewById(R.id.prevButton)
        pauseButton = view.findViewById(R.id.pauseButton)
        nextButton = view.findViewById(R.id.nextButton)
        fragmentSeekBar = view.findViewById(R.id.fragmentSeekBar)
        trackControlLayout = (activity as MainActivity).findViewById(R.id.trackControlLayout)
        audioVisualizer = view.findViewById(R.id.audioVisualizer)
        elapsedTimeTextView = view.findViewById(R.id.elapsedTimeTextView)
        totalDurationTextView = view.findViewById(R.id.totalDurationTextView)
        trackImageView = view.findViewById(R.id.trackImageView)
        equalizerImageButton = view.findViewById(R.id.equalizerImageButton)
        favoriteButton = view.findViewById(R.id.favoriteButton)
        shuffleButton = view.findViewById(R.id.shuffleButton)

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

        equalizerImageButton.setOnClickListener { createEqualizerActivity() }
        favoriteButton.setOnClickListener {
            val currentTrack = musicService?.getCurrentTrack()
            currentTrack?.let { track ->
                isTrackFavorite(track.id) { isFavorite ->
                    if (isFavorite) {
                        removeTrackFromFavorites(track.id)
                        favoriteButton.setColorFilter(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.light_gray
                            ), android.graphics.PorterDuff.Mode.SRC_IN
                        )
                    } else {
                        addTrackToFavorites(track)
                        favoriteButton.setColorFilter(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.accent_color_blue
                            ), android.graphics.PorterDuff.Mode.SRC_IN
                        )
                    }
                }
            }
        }

        shuffleButton.setOnClickListener {
            musicService?.isShuffleEnabled = !musicService?.isShuffleEnabled!!
            saveShuffleState(musicService?.isShuffleEnabled!!)
            if (musicService?.isShuffleEnabled!!) {
                shuffleButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.accent_color_blue
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                musicService?.shuffledIndices = trackList?.indices!!.shuffled()
            } else {
                shuffleButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.light_gray
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        musicService?.isShuffleEnabled = loadShuffleState()
        if (musicService?.isShuffleEnabled!!) {
            shuffleButton.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.accent_color_blue
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            musicService?.shuffledIndices = trackList?.indices!!.shuffled()
        } else {
            shuffleButton.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_gray
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentTrack = musicService?.getCurrentTrack()
        if (currentTrack != null) {
            updateTrackInfo(currentTrack)
            updateSeekbar(musicService?.getCurrentPosition() ?: 0, currentTrack.duration)
            updateFavorite(currentTrack)
        }
        musicService?.let { service ->
            audioVisualizer.getPathMedia(service.getExoPlayer())
        }
    }

    fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_circle_24)
        } else {
            pauseButton.setImageResource(R.drawable.ic_play)
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

    fun collapseFragment() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                view?.visibility = View.VISIBLE
                (activity as MainActivity).showTrackControl(musicService?.getCurrentTrack()!!)
            }

            override fun onAnimationEnd(animation: Animation?) {
                if (isAdded) {
                    requireActivity().supportFragmentManager.popBackStack()
                    view?.visibility = View.GONE
                }
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
        track.albumArt?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            trackImageView.setImageBitmap(bitmap)
        } ?: run {
            trackImageView.setImageResource(R.drawable.default_image)
        }
    }

    fun updateSeekbar(position: Int, duration: Int) {
        fragmentSeekBar.max = duration
        fragmentSeekBar.progress = position
        elapsedTimeTextView.text = formatTime(position)
        totalDurationTextView.text = formatTime(duration)
    }

    fun updateFavorite(currentTrack: Track?){
        isTrackFavorite(currentTrack!!.id) { isFavorite ->
            if (isFavorite) {
                favoriteButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.accent_color_blue
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                favoriteButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.light_gray
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun createEqualizerActivity() {
        val intent = Intent(requireContext(), EqualizerActivity::class.java)
        startActivity(intent)
    }

    private fun isTrackFavorite(trackId: Long, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val favoriteTrackDao = AppDatabase.getDatabase(requireContext()).favoriteTrackDao()
            val count = favoriteTrackDao.isTrackFavorite(trackId)
            callback(count > 0)
        }
    }

    private fun addTrackToFavorites(track: Track) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val favoriteTrackDao = AppDatabase.getDatabase(requireContext()).favoriteTrackDao()
            favoriteTrackDao.insert(
                FavoriteTrack(
                    track.id,
                    track.title,
                    track.artist,
                    track.duration,
                    track.albumArt
                )
            )
        }
    }

    private fun removeTrackFromFavorites(trackId: Long) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            val favoriteTrackDao = AppDatabase.getDatabase(requireContext()).favoriteTrackDao()
            favoriteTrackDao.removeTrackFromFavorites(trackId)
        }
    }

    private fun saveShuffleState(isEnabled: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("MusicServicePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isShuffleEnabled", isEnabled)
        editor.apply()
    }

    private fun loadShuffleState(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("MusicServicePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isShuffleEnabled", false)
    }
}
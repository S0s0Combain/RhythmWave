package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity(), TrackControlCallback {

    private lateinit var searchEditText: EditText
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var equalizerImageButton: ImageButton
    lateinit var trackControlLayout: ConstraintLayout
    private lateinit var trackImage: ImageView
    private lateinit var trackTitleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var favoritesCardView: CardView
    private lateinit var playlistsCardView: CardView
    private lateinit var recentTracksCardView: CardView
    private var musicService: MusicService? = null
    private var isBound = false
    private lateinit var viewPager: ViewPager2
    private var isTrackControlVisible = true
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.READ_MEDIA_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.navigationBarColor = ContextCompat.getColor(this, R.color.primary_background)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        initializeUI()
    }

    private fun initializeUI() {
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        searchEditText = findViewById(R.id.searchEditText)
        fragmentContainer = findViewById(R.id.fragmentContainer)
        equalizerImageButton = findViewById(R.id.equalizerImageButton)
        trackControlLayout = findViewById(R.id.trackControlLayout)
        trackImage = findViewById(R.id.trackImage)
        trackTitleTextView = findViewById(R.id.trackTitleTextView)
        artistTextView = findViewById(R.id.artistTextView)
        prevButton = findViewById(R.id.prevButton)
        pauseButton = findViewById(R.id.pauseButton)
        nextButton = findViewById(R.id.nextButton)
        favoritesCardView = findViewById(R.id.favoritesCardView)
        playlistsCardView = findViewById(R.id.playlistsCardView)
        recentTracksCardView = findViewById(R.id.recentTracksCardView)

        viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Треки"
                1 -> tab.text = "Альбомы"
                2 -> tab.text = "Исполнители"
            }
        }.attach()

        searchEditText.setOnClickListener {
            searchEditText.isEnabled = false
            val searchFragment = SearchFragment()
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                .add(R.id.fragmentContainer, searchFragment).addToBackStack(null).commit()
            isTrackControlVisible = (trackControlLayout.visibility == View.VISIBLE)
            trackControlLayout.visibility = View.GONE
        }
        equalizerImageButton.setOnClickListener { createEqualizerActivity() }

        prevButton.setOnClickListener { MusicService.getInstance()?.previousTrack() }
        pauseButton.setOnClickListener { MusicService.getInstance()?.togglePlayPause() }
        nextButton.setOnClickListener { MusicService.getInstance()?.nextTrack() }
        favoritesCardView.setOnClickListener {
            replaceFragment(
                R.id.fragmentContainer,
                FavoritesFragment()
            )
        }
        playlistsCardView.setOnClickListener {
            replaceFragment(
                R.id.fragmentContainer,
                PlaylistFragment()
            )
        }
        recentTracksCardView.setOnClickListener {
            replaceFragment(
                R.id.fragmentContainer,
                RecentTracksFragment()
            )
        }

        applyEqualizerSettings()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true

            applyEqualizerSettings()
            applyVirtualizerSetting()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_DENIED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Необходимы разрешения")
                    .setMessage("Для работы приложения требуется доступ к хранилищу и микрофону. Пожалуйста, предоставьте необходимые разрешения.")
                    .setPositiveButton("Предоставить") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                        )
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            initializeUI()
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun replaceFragment(containerId: Int, fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(containerId, fragment)
            .addToBackStack(null).commit()
    }

    private class ViewPagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TrackListFragment()
                1 -> AlbumsFragment()
                2 -> ArtistsFragment()
                else -> Fragment()
            }
        }
    }

    private fun createEqualizerActivity() {
        val intent = Intent(this, EqualizerActivity::class.java)
        startActivity(intent)
    }

    override fun onTrackChanged(track: Track) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)
        if ((currentFragment !is PlayerFragment)) {
            showTrackControl(track)
        }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        val currentTrack = musicService?.getCurrentTrack()
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }

        val playerFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateSeekbar(
            MusicService.getInstance()?.getCurrentPosition() ?: 0,
            currentTrack?.duration ?: 0
        )
        playerFragment?.updatePlayPauseButton(isPlaying)
        playerFragment?.updateFavorite(currentTrack)
    }

    fun showTrackControl(track: Track) {
        trackControlLayout.post {
            trackControlLayout.visibility = View.VISIBLE

            if (track.albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
                val roundedBitmap = ImageUtils.roundCorner(bitmap, 40f)
                trackImage.setImageBitmap(roundedBitmap)
            }

            trackTitleTextView.text = track.title
            artistTextView.text = track.artist

            val playerFragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
            playerFragment?.updateTrackInfo(track)
        }
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)

        if (fragmentManager.backStackEntryCount > 0) {
            if (currentFragment is PlayerFragment) {
                currentFragment.collapseFragment()
            } else if (currentFragment is SearchFragment) {
                if (isTrackControlVisible) {
                    trackControlLayout.visibility = View.VISIBLE
                }
                fragmentManager.popBackStack()
                searchEditText.isEnabled = true
            } else {
                fragmentManager.popBackStack()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun applyEqualizerSettings() {
        val sharedPreferences = getSharedPreferences("EqualizerSettings", MODE_PRIVATE)
        val bassLevel = sharedPreferences.getInt("Bass", 0)
        val frequency60Hz = sharedPreferences.getInt("60Hz", 50)
        val frequency230Hz = sharedPreferences.getInt("230Hz", 50)
        val frequency910Hz = sharedPreferences.getInt("910Hz", 50)
        val frequency14_0kHz = sharedPreferences.getInt("14.0kHz", 50)
        val frequency3_6kHz = sharedPreferences.getInt("3.6kHz", 50)
        musicService?.applyEqualizerSettings(
            bassLevel,
            frequency60Hz,
            frequency230Hz,
            frequency910Hz,
            frequency3_6kHz,
            frequency14_0kHz
        )
    }

    private fun applyVirtualizerSetting() {
        val sharedPreferences = getSharedPreferences("EqualizerSettings", MODE_PRIVATE)
        val virtualizerEnabled = sharedPreferences.getBoolean("VirtualizerEnabled", false)
        musicService?.setVirtualizerEnabled(virtualizerEnabled)
    }
}
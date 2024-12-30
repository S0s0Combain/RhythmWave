package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity(), IOnBackPressed, TrackControlCallback {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
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

        viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Треки"
                1 -> tab.text = "Альбомы"
                2 -> tab.text = "Исполнители"
            }
        }.attach()

        searchEditText.setOnClickListener {
            val searchFragment = SearchFragment()
            searchFragment.setTrackList(MusicService.getInstance()?.getTrackList() ?: listOf())
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, searchFragment).addToBackStack(null).commit()
            trackControlLayout.visibility = View.GONE
        }
        equalizerImageButton.setOnClickListener { createEqualizerActivity() }

        prevButton.setOnClickListener { MusicService.getInstance()?.previousTrack() }
        pauseButton.setOnClickListener { MusicService.getInstance()?.togglePlayPause() }
        nextButton.setOnClickListener { MusicService.getInstance()?.nextTrack() }
        favoritesCardView.setOnClickListener { openFavoritesFragment() }
        playlistsCardView.setOnClickListener { openPlaylistsFragment() }

        applyEqualizerSettings()
    }

    private fun openPlaylistsFragment() {
        replaceFragment(R.id.fragmentContainer, PlaylistFragment())
    }

    private fun openFavoritesFragment() {
        replaceFragment(R.id.fragmentContainer, FavoritesFragment())
    }

    private fun replaceFragment(containerId: Int, fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(containerId, fragment)
            .addToBackStack(null).commit()
        trackControlLayout.visibility = View.GONE
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
        showTrackControl(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }

        val playerFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateSeekbar(
            MusicService.getInstance()?.getCurrentPosition() ?: 0,
            MusicService.getInstance()?.getCurrentTrack()?.duration ?: 0
        )
    }

    fun showTrackControl(track: Track) {
        trackControlLayout.visibility = View.VISIBLE
        if (track.albumArt != null) {
            val bitmap =
                BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
            val roundedBitmap = ImageUtils.roundCorner(bitmap, 40f)
            trackImage.setImageBitmap(roundedBitmap)
        }
        trackTitleTextView.text = track.title
        artistTextView.text = track.artist

        val playerFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateTrackInfo(track)
    }

    override fun onBackPressed() {
        if (!backPressed()) {
            super.onBackPressed()
        }
    }

    override fun backPressed(): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        return if (fragment is PlayerFragment) {
            fragment.collapseFragment()
            trackControlLayout.visibility = View.VISIBLE
            true
        } else {
            false
        }
    }

    private fun applyEqualizerSettings() {
        val sharedPreferences = getSharedPreferences("EqualizerSettings", MODE_PRIVATE)
        val bassLevel = sharedPreferences.getInt("Bass", 0)
        val trebleLevel = sharedPreferences.getInt("Treble", 0)

//        MusicService.getInstance()?.applyEqualizerSettings(bassLevel, trebleLevel)
    }
}

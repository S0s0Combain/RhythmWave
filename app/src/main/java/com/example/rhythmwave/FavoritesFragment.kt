package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment(), TrackControlCallback {
    private lateinit var favoriteRecyclerView: RecyclerView
    private lateinit var favoriteTrackAdapter: FavoriteTrackAdapter
    private val favoriteTracks = mutableListOf<FavoriteTrack>()
    private lateinit var pauseButton: ImageButton
    private lateinit var trackControlLayout: ConstraintLayout

    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicServiceBinder).getService()
            isBound = true
            musicService?.setFavoriteTrackAdapter(favoriteTrackAdapter)
            loadFavoriteTracks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        favoriteRecyclerView = view.findViewById(R.id.favoriteRecyclerView)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        favoriteRecyclerView.addItemDecoration(SpacesItemDecorations(spaceInPixels))
        favoriteRecyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
        favoriteTrackAdapter = FavoriteTrackAdapter(favoriteTracks) { track ->
            onTrackClick(track)
        }
        trackControlLayout = (activity as MainActivity).findViewById(R.id.trackControlLayout)
        pauseButton = (activity as MainActivity).findViewById(R.id.pauseButton)
        favoriteRecyclerView.adapter = favoriteTrackAdapter
        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        view.setOnClickListener { }
    }

    private fun loadFavoriteTracks() {
        CoroutineScope(Dispatchers.IO).launch {
            val favorites =
                AppDatabase.getDatabase(requireContext()).favoriteTrackDao().getAllFavorites()
            withContext(Dispatchers.Main) {
                favoriteTracks.clear()
                favoriteTracks.addAll(favorites)
                favoriteTrackAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun onTrackClick(track: FavoriteTrack) {
        val trackToPlay = Track(
            track.title,
            track.artist,
            track.duration,
            track.id,
            track.albumArt
        )

        val currentTrackList = musicService?.getTrackList() ?: return

        if (currentTrackList != favoriteTracks) {
            musicService?.setTrackList(favoriteTracks.map {
                Track(
                    it.title,
                    it.artist,
                    it.duration,
                    it.id,
                    it.albumArt
                )
            })
        }

        if (musicService?.getCurrentTrack() == trackToPlay) {
            if (musicService?.isPlaying() == true) {
                musicService?.pauseTrack()
            } else {
                musicService?.resumeTrack()
                openPlayerFragment(musicService!!)
            }
        } else {
            musicService?.playTrack(trackToPlay)
            openPlayerFragment(musicService!!)
        }
    }

    override fun onTrackChanged(track: Track) {
        (activity as MainActivity).showTrackControl(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }

        val playerFragment =
            parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateSeekbar(
            musicService?.getCurrentPosition() ?: 0,
            musicService?.getCurrentTrack()?.duration ?: 0
        )
    }

    private fun openPlayerFragment(musicService: MusicService) {
        trackControlLayout.visibility = View.GONE
        val playerFragment = PlayerFragment().apply {
            this.musicService = musicService
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
            .add(R.id.fragmentContainer, playerFragment)
            .addToBackStack(null)
            .commit()
    }
}

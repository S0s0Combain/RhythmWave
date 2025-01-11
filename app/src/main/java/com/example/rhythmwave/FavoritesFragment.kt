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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment(), TrackControlCallback {
    private lateinit var favoriteRecyclerView: RecyclerView
    private lateinit var favoriteTrackAdapter: FavoriteTrackAdapter
    private val favoriteTracks = mutableListOf<FavoriteTrack>()

    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicServiceBinder).getService()
            isBound = true
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
        favoriteRecyclerView.adapter = favoriteTrackAdapter
        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
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

        musicService?.playTrack(trackToPlay)
    }

    override fun onTrackChanged(track: Track) {
        (activity as MainActivity).showTrackControl(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {

    }
}

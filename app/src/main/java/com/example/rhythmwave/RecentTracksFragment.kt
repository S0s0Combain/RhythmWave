package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class RecentTracksFragment : Fragment(), TrackControlCallback {

    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var tracks: MutableList<Track>
    var musicService: MusicService? = null
    private var isBound = false
    private lateinit var pauseButton: ImageButton
    private lateinit var trackControlLayout: ConstraintLayout
    private lateinit var randomButton: MaterialButton
    private lateinit var clearImageButton: ImageButton
    private lateinit var backButton: ImageButton

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicServiceBinder).getService()
            isBound = true
            musicService?.setTrackAdapter(trackAdapter)
            loadRecentTracks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recent_tracks, container, false)
        tracksRecyclerView = view.findViewById(R.id.tracksRecyclerView)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        tracksRecyclerView.addItemDecoration(SpacesItemDecorations(spaceInPixels))
        trackControlLayout = (activity as MainActivity).findViewById(R.id.trackControlLayout)
        pauseButton = (activity as MainActivity).findViewById(R.id.pauseButton)
        randomButton = view.findViewById(R.id.randomButton)
        clearImageButton = view.findViewById(R.id.clearImageButton)
        clearImageButton.setOnClickListener { showClearConfirmationDialog() }
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                onTrackClick(track)
            },
            onShareClick = { track ->
                TrackUtils.shareTrack(
                    requireContext(),
                    track,
                    requireContext().contentResolver
                )
            },
            onDeleteTrack = { track -> deleteTrack(track) }
        )

        tracksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tracksRecyclerView.adapter = trackAdapter
        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        randomButton.setOnClickListener { shuffleTracks() }
        view.setOnClickListener { }
        return view
    }

    private fun shuffleTracks() {
        val currentTrackList = musicService?.getTrackList() ?: return

        if (currentTrackList != tracks) {
            musicService?.setTrackList(tracks.map {
                Track(
                    it.title,
                    it.artist,
                    it.duration,
                    it.id,
                    it.albumArt
                )
            })
            musicService?.shuffleTrackList()
        }
    }

    private fun onTrackClick(track: Track) {
        val currentTrackList = musicService?.getTrackList() ?: return
        if (currentTrackList != tracks) {
            musicService?.setTrackList(tracks)
        }
        if (musicService?.getCurrentTrack() == track) {
            if (musicService?.isPlaying() == true) {
                musicService?.pauseTrack()
            } else {
                musicService?.resumeTrack()
                openPlayerFragment(musicService)
            }
        } else {
            musicService?.playTrack(track)
            openPlayerFragment(musicService)
        }
    }

    private fun loadRecentTracks() {
        CoroutineScope(Dispatchers.IO).launch {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

            val recentTracks = AppDatabase.getDatabase(requireContext()).recentTrackDao()
                .getRecentTracks(sevenDaysAgo)

            tracks = mutableListOf()

            for (recentTrack in recentTracks) {
                val track = getTrackById(recentTrack.trackId)
                if (track != null) {
                    tracks.add(track)
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getDatabase(requireActivity().applicationContext).recentTrackDao()
                            .deleteByTrackId(recentTrack.trackId)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                trackAdapter.updateTracks(tracks)
            }
        }
    }

    private fun getTrackById(trackId: Long): Track? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = activity?.applicationContext?.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(trackId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val duration = it.getInt(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))

                return Track(
                    title,
                    artist,
                    duration,
                    id,
                    getAlbumArt(requireActivity().applicationContext, id)
                )
            }
        }
        return null
    }

    private fun getAlbumArt(context: Context, trackId: Long): ByteArray? {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(trackId.toString())
        val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)

        var albumId: Long? = null
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    albumId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                }
            }

        return albumId?.let { id ->
            val albumArtUri: Uri = Uri.parse("content://media/external/audio/albumart/$id")
            try {
                context.contentResolver.openInputStream(albumArtUri)?.use { inputStream ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    inputStream.copyTo(byteArrayOutputStream)
                    byteArrayOutputStream.toByteArray()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun deleteTrack(track: Track) {

    }

    private fun openPlayerFragment(musicService: MusicService?) {
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

    override fun onTrackChanged(track: Track) {
        if (isAdded) {
            val fragmentManager = parentFragmentManager
            val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment !is PlayerFragment) {
                (activity as MainActivity).showTrackControl(track)
            } else {
                val playerFragment =
                    parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
                playerFragment?.updateTrackInfo(track)
            }
        }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }
        if (isAdded) {
            val playerFragment =
                parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
            playerFragment?.updateSeekbar(
                musicService?.getCurrentPosition() ?: 0,
                musicService?.getCurrentTrack()?.duration ?: 0
            )
        }
    }

    private fun showClearConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Очистка недавних треков")
            .setMessage("Вы действительно хотите очистить список недавних треков?")
            .setPositiveButton("Да") { _, _ ->
                clearRecentTracks()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun clearRecentTracks() {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(requireContext()).recentTrackDao().clearRecentTracks()
            loadRecentTracks()
        }
    }
}
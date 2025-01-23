package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayOutputStream

class TracksFragment : Fragment(), TrackControlCallback {

    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var pauseButton: ImageButton
    private lateinit var trackControlLayout: ConstraintLayout
    private lateinit var titleTextView: TextView
    private var album: Album? = null
    private var artist: Artist? = null
    private lateinit var tracks: MutableList<Track>
    private lateinit var randomButton: MaterialButton
    private lateinit var backButton: ImageButton

    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicServiceBinder).getService()
            isBound = true
            musicService?.setTrackControlCallback(this@TracksFragment)
            musicService?.setTrackAdapter(trackAdapter)
            loadTracks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tracks, container, false)
        tracksRecyclerView = view.findViewById(R.id.tracksRecyclerView)
        trackControlLayout = (activity as MainActivity).trackControlLayout
        pauseButton = (activity as MainActivity).findViewById(R.id.pauseButton)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        tracksRecyclerView.addItemDecoration(SpacesItemDecorations(spaceInPixels))
        titleTextView = view.findViewById(R.id.titleTextView)
        titleTextView.setText(arguments?.getString("title"))
        randomButton = view.findViewById(R.id.randomButton)
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }

        album = arguments?.getParcelable("album")
        artist = arguments?.getParcelable("artist")

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

    private fun loadTracks() {
        val selection: String = if (album != null) {
            "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        } else if (artist != null) {
            "${MediaStore.Audio.Media.ARTIST} = ?"
        } else {
            return
        }

        val selectionArgs = arrayOf(album?.albumId?.toString() ?: artist?.name)
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val cursor: Cursor? = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        tracks = mutableListOf()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist =
                        it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val duration =
                        it.getInt(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val mimeType =
                        it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

                    if (duration < 30000 || mimeType != "audio/mpeg") {
                        continue
                    }

                    val albumArt = getAlbumArt(requireContext(), id)

                    tracks.add(Track(title, artist, duration, id, albumArt))
                } while (it.moveToNext())
            }
        }

        trackAdapter.updateTracks(tracks)
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

        if (albumId != null) {
            val albumArtUri: Uri = Uri.parse("content://media/external/audio/albumart/$albumId")
            return try {
                val inputStream = context.contentResolver.openInputStream(albumArtUri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                inputStream?.copyTo(byteArrayOutputStream)
                byteArrayOutputStream.toByteArray()
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun openPlayerFragment(musicService: MusicService?) {
        trackControlLayout.visibility = View.GONE
        val playerFragment = PlayerFragment().apply {
            this.musicService = musicService
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, playerFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteTrack(track: Track) {
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(track.id.toString())
        requireContext().contentResolver.delete(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            selection,
            selectionArgs
        )

        val updatedTrackList = trackAdapter.getTracks().filter { it.id != track.id }
        trackAdapter.updateTracks(updatedTrackList)
    }

    override fun onTrackChanged(track: Track) {
        val fragmentManager = parentFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment !is PlayerFragment) {
            (activity as MainActivity).showTrackControl(track)
        }else{
            val playerFragment =
                parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
            playerFragment?.updateTrackInfo(track)
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
}

package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PlaylistTracksFragment : Fragment() {

    private lateinit var tracksList: RecyclerView
    private var musicService: MusicService? = null
    private var isBound = false
    private lateinit var trackAdapter: PlaylistTracksAdapter
    private var playlistId: Long? = null
    private lateinit var playlistImage: ImageView

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
            loadTracks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getLong("playlistId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        return inflater.inflate(R.layout.fragment_playlist_tracks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tracksList = view.findViewById(R.id.playlistTracksRecyclerView)
        tracksList.layoutManager = LinearLayoutManager(context)

        playlistImage = view.findViewById(R.id.playlistImage)
        trackAdapter = PlaylistTracksAdapter()
        tracksList.adapter = trackAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            context?.unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun loadTracks() {
        playlistId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                val playlistTracks = AppDatabase.getDatabase(requireContext()).playlistTrackDao().getTracksInPlaylist(id)

                val tracks = mutableListOf<Track>()
                val trackIds = mutableListOf<Long>()

                for (playlistTrack in playlistTracks) {
                    val track = getTrackById(playlistTrack.trackId)
                    if (track != null) {
                        tracks.add(track)
                        trackIds.add(playlistTrack.trackId)
                    } else {
                        AppDatabase.getDatabase(requireContext()).playlistTrackDao().removeTrackFromPlaylist(id, playlistTrack.trackId)
                    }
                }

                withContext(Dispatchers.Main) {
                    trackAdapter.updateTracks(tracks)
                }
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

        val cursor = requireContext().contentResolver.query(
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

                return Track(title, artist, duration, id, getAlbumArt(requireContext(), id))
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
}

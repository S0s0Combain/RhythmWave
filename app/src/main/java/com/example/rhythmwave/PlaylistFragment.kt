package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PlaylistFragment : Fragment() {
    private lateinit var playlistRecyclerView: RecyclerView
    private val playlists = mutableListOf<Playlist>()
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var createPlaylistButton: ImageButton
    private lateinit var backButton: ImageButton
    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicServiceBinder).getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        createPlaylistButton = view.findViewById(R.id.addPlaylistImageButton)
        createPlaylistButton.setOnClickListener { createNewPlaylist() }
        playlistRecyclerView = view.findViewById(R.id.playlistRecyclerView)
        playlistRecyclerView.layoutManager = GridLayoutManager(context, 2)
        playlistAdapter = PlaylistAdapter(playlists, this::onPlaylistClick, this::onPlayButtonClick)
        playlistRecyclerView.adapter = playlistAdapter
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        loadPlaylists()
        view.setOnClickListener { }

        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun loadPlaylists() {
        CoroutineScope(Dispatchers.IO).launch {
            val savedPlaylists =
                AppDatabase.getDatabase(requireContext()).playlistDao().getAllPlaylists()
            withContext(Dispatchers.Main) {
                playlists.clear()
                playlists.addAll(savedPlaylists)
                playlistAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun createNewPlaylist() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Создание нового плейлиста")

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton("Создать") { dialog, which ->
            val newPlaylistName = input.text.toString()
            if (newPlaylistName.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getDatabase(requireContext()).playlistDao().insert(
                        Playlist(
                            name = newPlaylistName,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    loadPlaylists()
                }

                Toast.makeText(
                    context,
                    "Плейлист '$newPlaylistName' успешно создан!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(context, "Имя плейлиста не может быть пустым", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        builder.setNegativeButton("Отмена") { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun onPlaylistClick(playlist: Playlist) {
        val fragment = PlaylistTracksFragment().apply {
            arguments = Bundle().apply {
                putLong("playlistId", playlist.id)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    private fun onPlayButtonClick(playlist: Playlist) {
        if (musicService != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val trackEntities = AppDatabase.getDatabase(requireContext()).playlistTrackDao()
                    .getTracksInPlaylist(playlist.id)
                val tracks: List<Track> = trackEntities.mapNotNull { track ->
                    getTrackById(track.trackId)
                }
                if (tracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        musicService?.setTrackList(tracks)
                        musicService?.playTrack(tracks.first())
                        openPlayerFragment(musicService)
                    }
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

    private fun openPlayerFragment(musicService: MusicService?) {
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

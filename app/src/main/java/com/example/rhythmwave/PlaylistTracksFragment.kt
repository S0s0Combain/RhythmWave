package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PlaylistTracksFragment : Fragment(), TrackControlCallback {

    private lateinit var tracksList: RecyclerView
    private var musicService: MusicService? = null
    private var isBound = false
    private lateinit var trackAdapter: PlaylistTracksAdapter
    private var playlistId: Long? = null
    private lateinit var playlistImage: ImageView
    private lateinit var tracks: MutableList<Track>
    private lateinit var editPlaylistButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var trackControlLayout: ConstraintLayout
    private lateinit var backButton: ImageButton
    private lateinit var playPlaylistButton: ImageButton

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
            musicService?.setPlaylistTrackAdapter(trackAdapter)
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
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        tracksList.addItemDecoration(SpacesItemDecorations(spaceInPixels))

        editPlaylistButton = view.findViewById(R.id.editPlaylistButton)
        editPlaylistButton.setOnClickListener { showContextMenu(it) }
        pauseButton = (activity as MainActivity).findViewById(R.id.pauseButton)
        playlistImage = view.findViewById(R.id.playlistImage)
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        playPlaylistButton = view.findViewById(R.id.playPlaylistButton)
        playPlaylistButton.setOnClickListener {
            if (tracks.isNotEmpty()) {
                onTrackClick(tracks[0])
            }
        }
        trackAdapter = PlaylistTracksAdapter(
            onTrackClick = { track -> onTrackClick(track) },
            onShareClick = { track ->
                TrackUtils.shareTrack(
                    requireContext(),
                    track,
                    requireContext().contentResolver
                )
            },
            onDeleteTrackClick = { track -> showDeleteConfirmationDialog(track) }
        )
        trackControlLayout = (activity as MainActivity).trackControlLayout
        tracksList.adapter = trackAdapter

        tracks = mutableListOf()
        view.setOnClickListener { }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            context?.unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun onTrackClick(track: Track) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
        } else {
            Toast.makeText(
                requireContext(),
                "Отсутствует разрешение на доступ к микрофону",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDeleteConfirmationDialog(track: Track) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Подтвердите удаление")
            .setMessage("Вы действительно хотите удалить трек из плейлиста?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteTrackFromPlaylist(track)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteTrackFromPlaylist(track: Track) {
        playlistId?.let { playlistId ->
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(requireContext()).playlistTrackDao()
                    .removeTrackFromPlaylist(playlistId, track.id)
                withContext(Dispatchers.Main) {
                    loadTracks()
                }
            }
        }
    }

    private fun loadTracks() {
        playlistId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                val playlistTracks = AppDatabase.getDatabase(requireContext()).playlistTrackDao()
                    .getTracksInPlaylist(id)

                val playlist =
                    AppDatabase.getDatabase(requireContext()).playlistDao().getPlaylistById(id)

                val newTracks = mutableListOf<Track>()
                val trackIds = mutableListOf<Long>()

                for (playlistTrack in playlistTracks) {
                    val track = getTrackById(playlistTrack.trackId)
                    if (track != null) {
                        newTracks.add(track)
                        trackIds.add(playlistTrack.trackId)
                    } else {
                        AppDatabase.getDatabase(requireContext()).playlistTrackDao()
                            .removeTrackFromPlaylist(id, playlistTrack.trackId)
                    }
                }

                withContext(Dispatchers.Main) {
                    tracks.clear()
                    tracks.addAll(newTracks)
                    trackAdapter.updateTracks(tracks)
                    view?.findViewById<TextView>(R.id.playlistNameTextView)?.text = playlist?.name
                    if (playlist?.image == null) {
                        playlistImage.setImageResource(R.drawable.default_image)
                    } else {
                        playlistImage.setImageBitmap(playlist?.image?.let {
                            BitmapFactory.decodeByteArray(
                                it,
                                0,
                                it.size
                            )
                        })
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

    override fun onTrackChanged(track: Track) {
        Log.d("MyLog", "onTrackChanged")
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
        Log.d("MyLog", "onPlaybackStateChanged")
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

    private fun showContextMenu(view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.playlist_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_playlist -> {
                    openEditPlaylistFragment()
                    true
                }

                R.id.menu_delete -> {
                    deletePlaylist()
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun openEditPlaylistFragment() {
        val fragment = EditPlaylistFragment().apply {
            arguments = Bundle().apply {
                putLong("playlistId", playlistId!!)
            }
        }
        parentFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deletePlaylist() {
        playlistId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(requireContext()).playlistDao().deletePlaylistById(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Плейлист удалён", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
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

    override fun onResume() {
        super.onResume()
        loadTracks()
    }
}
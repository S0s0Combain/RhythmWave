package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.ByteArrayOutputStream

class TrackListFragment : Fragment(), TrackControlCallback {

    private lateinit var tracksList: RecyclerView
    private lateinit var trackControlLayout: ConstraintLayout
    private lateinit var trackImage: ImageView
    private lateinit var trackTitleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var tracks: MutableList<Track>

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.RECORD_AUDIO
    )

    var musicService: MusicService? = null
    private var isBound = false
    private lateinit var trackAdapter: TrackAdapter

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
            musicService?.setTrackControlCallback(this@TrackListFragment)
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
        musicService = MusicService.getInstance()
        return inflater.inflate(R.layout.fragment_track_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tracksList = view.findViewById(R.id.searchRecyclerView)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        tracksList.addItemDecoration(SpacesItemDecorations(spaceInPixels))
        trackControlLayout = (activity as MainActivity).findViewById(R.id.trackControlLayout)
        trackImage = (activity as MainActivity).findViewById(R.id.trackImage)
        trackTitleTextView = (activity as MainActivity).findViewById(R.id.trackTitleTextView)
        artistTextView = (activity as MainActivity).findViewById(R.id.artistTextView)
        pauseButton = (activity as MainActivity).findViewById(R.id.pauseButton)
        fragmentContainer = (activity as MainActivity).findViewById(R.id.fragmentContainer)
        trackControlLayout.setOnClickListener { openPlayerFragment(musicService) }

        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound) {
            context?.unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun loadTracks() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val cursor: Cursor? = context?.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        tracks = mutableListOf()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val title =
                        cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist =
                        cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val duration =
                        cursor.getInt(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val id = cursor.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val mimeType =
                        cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

                    if (duration < 30000) {
                        continue
                    }

                    if (mimeType != "audio/mpeg") {
                        continue
                    }

                    val albumArt = getAlbumArt(requireContext(), id)
                    tracks.add(Track(title, artist, duration, id, albumArt))
                } while (it.moveToNext())
            }
        }

        musicService?.setTrackList(tracks)

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
        musicService?.setTrackAdapter(trackAdapter)
        tracksList.layoutManager = LinearLayoutManager(context)
        tracksList.adapter = trackAdapter
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
        } else{
//            Toast.makeText(requireContext(), "Отсутствует разрешение на доступ к микрофону", Toast.LENGTH_SHORT).show()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Необходимы разрешения")
                .setMessage("Для работы приложения требуется доступ к хранилищу и микрофону. Пожалуйста, предоставьте необходимые разрешения.")
                .setPositiveButton("Предоставить") { _, _ ->
                    ActivityCompat.requestPermissions(
                        requireActivity(), REQUIRED_PERMISSIONS, 1001
                    )
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onTrackChanged(track: Track) {
        trackAdapter.updateCurrentTrack(track)
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

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        val currentTrack = musicService?.getCurrentTrack()
        trackAdapter.updateCurrentTrack(currentTrack)
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }
        val playerFragment =
            parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateSeekbar(
            musicService?.getCurrentPosition() ?: 0,
            currentTrack?.duration ?: 0
        )
        playerFragment?.updatePlayPauseButton(isPlaying)
        playerFragment?.updateFavorite(currentTrack)
    }

    private fun deleteTrack(track: Track) {
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(track.id.toString())
        context?.contentResolver?.delete(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            selection,
            selectionArgs
        )

        val updatedTrackList =
            musicService?.getTrackList()?.filter { it.id != track.id } ?: emptyList()
        musicService?.setTrackList(updatedTrackList)
        trackAdapter.updateTracks(updatedTrackList)
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
}
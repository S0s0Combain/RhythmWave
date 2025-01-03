package com.example.rhythmwave

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class RecentTracksFragment : Fragment() {

    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recent_tracks, container, false)
        tracksRecyclerView = view.findViewById(R.id.tracksRecyclerView)

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                MusicService.getInstance()?.playTrack(track)
                (requireActivity() as MainActivity).showTrackControl(track)
                openPlayerFragment()
            },
            onShareClick = { track -> TrackUtils.shareTrack(requireContext(), track, requireContext().contentResolver) },
            onDeleteTrack = { track -> deleteTrack(track) }
        )

        tracksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tracksRecyclerView.adapter = trackAdapter

        loadRecentTracks()

        return view
    }

    private fun loadRecentTracks() {
        CoroutineScope(Dispatchers.IO).launch {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

            val recentTracks = AppDatabase.getDatabase(requireContext()).recentTrackDao().getRecentTracks(sevenDaysAgo)

            val tracks = mutableListOf<Track>()

            for (recentTrack in recentTracks) {
                val track = getTrackById(recentTrack.trackId)
                if (track != null) {
                    tracks.add(track)
                } else {
                   TODO("Реализовать удаление треков, которые не были найдены")
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
                    albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
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

    private fun openPlayerFragment() {

    }

    private fun deleteTrack(track: Track) {

    }
}
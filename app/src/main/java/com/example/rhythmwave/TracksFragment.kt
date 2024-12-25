package com.example.rhythmwave

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

class TracksFragment : Fragment(), TrackControlCallback {

    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var trackControlLayout: ConstraintLayout
    private var album: Album? = null
    private var artist: Artist? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album_tracks, container, false)
        tracksRecyclerView = view.findViewById(R.id.tracksRecyclerView)
        trackControlLayout = (activity as MainActivity).trackControlLayout

        album = arguments?.getParcelable("album")
        artist = arguments?.getParcelable("artist")

        trackAdapter = TrackAdapter(
            onTrackClick = { track ->
                (requireActivity() as MainActivity).musicService?.playTrack(track)
                (requireActivity() as MainActivity).showTrackControl(track)
                openPlayerFragment((requireActivity() as MainActivity).musicService)
            },
            onShareClick = { track -> TrackUtils.shareTrack(requireContext(), track, requireContext().contentResolver) },
            onDeleteTrack = { track -> deleteTrack(track) }
        )
        tracksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tracksRecyclerView.adapter = trackAdapter

        loadTracks()

        return view
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

        val tracks = mutableListOf<Track>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val duration = it.getInt(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

                    if (duration < 30000 || mimeType != "audio/mpeg") {
                        continue
                    }

                    val albumArt = getAlbumArt(requireContext(), album?.albumId ?: 0)
                    tracks.add(Track(title, artist, duration, id, albumArt))
                } while (it.moveToNext())
            }
        }

        trackAdapter.updateTracks(tracks)
    }

    private fun getAlbumArt(context: Context, albumId: Long): ByteArray? {
        val uri: Uri = Uri.parse("content://media/external/audio/albumart/$albumId")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            inputStream?.copyTo(byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun openPlayerFragment(musicService: MusicService?) {
        trackControlLayout.visibility = View.GONE
        val playerFragment = PlayerFragment().apply {
            this.musicService = musicService
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, playerFragment)
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
        (requireActivity() as MainActivity).showTrackControl(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        (requireActivity() as MainActivity).onPlaybackStateChanged(isPlaying)
    }
}

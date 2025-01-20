package com.example.rhythmwave

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

class AlbumsFragment : Fragment() {

    private lateinit var albumsRecyclerView: RecyclerView
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var fragmentContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_albums, container, false)
        albumsRecyclerView = view.findViewById(R.id.albumsRecyclerView)
        fragmentContainer = (activity as MainActivity).findViewById(R.id.fragmentContainer)
        albumAdapter = AlbumAdapter(
            onAlbumClick = { album ->
                val albumTracksFragment = TracksFragment().apply {
                    arguments = Bundle().apply {
                        putString("title", album.name)
                        putParcelable("album", album)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                    .replace(R.id.fragmentContainer, albumTracksFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        albumsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        albumsRecyclerView.adapter = albumAdapter

        loadAlbums()

        return view
    }

    private fun loadAlbums() {
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST
        )
        val cursor: Cursor? = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.ALBUM} ASC"
        )

        val albumSet = mutableSetOf<Pair<String, String>>()
        val albums = mutableListOf<Album>()

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val albumName =
                        it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                    val albumId =
                        it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val artist =
                        it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    if (artist != null && artist != "<unknown>") {
                        val uniqueKey = Pair(albumName, artist)
                        if (albumSet.add(uniqueKey)) {
                            val albumArt = getAlbumArt(requireContext(), albumId)
                            albums.add(Album(albumName, artist, albumId, albumArt))
                        }
                    }
                } while (it.moveToNext())
            }
        }

        albumAdapter.updateAlbums(albums)
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
}

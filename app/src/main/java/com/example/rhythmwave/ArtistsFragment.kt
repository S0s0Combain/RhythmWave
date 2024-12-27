package com.example.rhythmwave

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArtistsFragment : Fragment() {

    private lateinit var artistsRecyclerView: RecyclerView
    private lateinit var artistAdapter: ArtistsAdapter
    private lateinit var fragmentContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_artists, container, false)
        artistsRecyclerView = view.findViewById(R.id.artistsRecyclerView)
        fragmentContainer = (activity as MainActivity).findViewById(R.id.fragmentContainer)
        artistAdapter = ArtistsAdapter { artist ->
            openArtistTracksFragment(artist)
        }
        artistsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        artistsRecyclerView.adapter = artistAdapter

        loadArtists()

        return view
    }

    private fun loadArtists() {
        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID
        )
        val cursor: Cursor? = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.ARTIST} ASC"
        )

        val artistsMap = mutableMapOf<String, Int>()

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    if (artist != "<unknown>") {
                        val count = artistsMap.getOrDefault(artist, 0) + 1
                        artistsMap[artist] = count
                    }
                } while (it.moveToNext())
            }
        }

        val artists = artistsMap.map { Artist(it.key, it.value) }
        artistAdapter.updateArtists(artists)
    }

    private fun openArtistTracksFragment(artist: Artist) {
        val artistTracksFragment = TracksFragment().apply {
            arguments = Bundle().apply {
                putParcelable("artist", artist)
            }
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, artistTracksFragment)
            .addToBackStack(null)
            .commit()
    }
}
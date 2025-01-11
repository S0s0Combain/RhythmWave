package com.example.rhythmwave

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistFragment : Fragment() {
    private lateinit var playlistRecyclerView: RecyclerView
    private val playlists = mutableListOf<Playlist>()
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var createPlaylistButton: ImageButton

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
        playlistAdapter = PlaylistAdapter(playlists, this::onPlaylistClick)
        playlistRecyclerView.adapter = playlistAdapter
        loadPlaylists()
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
}

package com.example.rhythmwave

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment() {
    private lateinit var favoriteRecyclerView: RecyclerView
    private lateinit var favoriteTrackAdapter: FavoriteTrackAdapter
    private val favoriteTracks = mutableListOf<FavoriteTrack>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        favoriteRecyclerView = view.findViewById(R.id.favoriteRecyclerView)
        favoriteRecyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
        favoriteTrackAdapter = FavoriteTrackAdapter(favoriteTracks)
        favoriteRecyclerView.adapter = favoriteTrackAdapter

        loadFavoriteTracks()
    }

    private fun loadFavoriteTracks() {
        CoroutineScope(Dispatchers.IO).launch {
            val favorites = AppDatabase.getDatabase(requireContext()).favoriteTrackDao().getAllFavorites()
            withContext(Dispatchers.Main) {
                favoriteTracks.clear()
                favoriteTracks.addAll(favorites)
                favoriteTrackAdapter.notifyDataSetChanged()
            }
        }
    }
}

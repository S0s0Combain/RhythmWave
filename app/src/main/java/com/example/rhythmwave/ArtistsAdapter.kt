package com.example.rhythmwave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ArtistsAdapter(private val onArtistClick: (Artist) -> Unit) : RecyclerView.Adapter<ArtistsAdapter.ArtistsViewHolder>() {

    private var artists: List<Artist> = emptyList()

    class ArtistsViewHolder(itemView: View, val onClick: (Artist) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val artistName: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val trackCount: TextView = itemView.findViewById(R.id.trackCountTextView)

        fun bind(artist: Artist) {
            artistName.text = artist.name
            trackCount.text = "${artist.trackCount} треков"
            itemView.setOnClickListener { onClick(artist) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return ArtistsViewHolder(view, onArtistClick)
    }

    override fun onBindViewHolder(holder: ArtistsViewHolder, position: Int) {
        holder.bind(artists[position])
    }

    override fun getItemCount(): Int = artists.size

    fun updateArtists(newArtists: List<Artist>) {
        artists = newArtists
        notifyDataSetChanged()
    }
}

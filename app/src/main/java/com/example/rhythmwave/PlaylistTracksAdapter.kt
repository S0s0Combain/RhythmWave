package com.example.rhythmwave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaylistTracksAdapter : RecyclerView.Adapter<PlaylistTracksAdapter.PlaylistTracksViewHolder>() {

    private var tracks: List<Track> = emptyList()

    fun updateTracks(newTracks: List<Track>) {
        tracks = newTracks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistTracksViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_track, parent, false)
        return PlaylistTracksViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistTracksViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track)
    }

    override fun getItemCount(): Int = tracks.size

    inner class PlaylistTracksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackTitle: TextView = itemView.findViewById(R.id.trackTitle)
        private val trackArtist: TextView = itemView.findViewById(R.id.trackArtist)

        fun bind(track: Track) {
            trackTitle.text = track.title
            trackArtist.text = track.artist
        }
    }
}

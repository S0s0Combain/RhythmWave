package com.example.rhythmwave

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteTrackAdapter(private val tracks: List<FavoriteTrack>, private val onTrackClick: (FavoriteTrack) -> Unit) : RecyclerView.Adapter<FavoriteTrackAdapter.FavoriteTrackViewHolder>() {

    class FavoriteTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)
        val trackImageView = itemView.findViewById<ImageView>(R.id.trackImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteTrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return FavoriteTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteTrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.titleTextView.text = track.title
        holder.artistTextView.text = track.artist
        if (track.albumArt != null) {
            val bitmap = BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
            val roundedBitmap = ImageUtils.roundCorner(bitmap, 40f)
            holder.trackImageView.setImageBitmap(roundedBitmap)
        } else {

        }

        holder.itemView.setOnClickListener { onTrackClick(track) }
    }

    override fun getItemCount() = tracks.size
}

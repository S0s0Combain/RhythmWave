package com.example.rhythmwave

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class FavoriteTrackAdapter(
    private val tracks: List<FavoriteTrack>,
    private val onTrackClick: (FavoriteTrack) -> Unit
) : RecyclerView.Adapter<FavoriteTrackAdapter.FavoriteTrackViewHolder>() {
    private var currentTrack: Track? = null

    class FavoriteTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.artistTextView)
        val trackImageView = itemView.findViewById<ImageView>(R.id.trackImage)
        val equalizerView = itemView.findViewById<EqualizerView>(R.id.equalizerView)
        val replyImageView = itemView.findViewById<ImageView>(R.id.replyImageView)
        val contextMenuImageView = itemView.findViewById<ImageButton>(R.id.contextMenuImageView)
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
        Log.d("MyLog", track.toString())
        Log.d("MyLog", currentTrack.toString())
        if (Track(track.title, track.artist, track.duration, track.id, track.albumArt) == currentTrack) {
            holder.titleTextView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.accent_color_blue
                )
            )
            holder.equalizerView.visibility = View.VISIBLE
            holder.replyImageView.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.accent_color_blue
                )
            )
            holder.contextMenuImageView.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.accent_color_blue
                )
            )
        } else {
            holder.titleTextView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.light_gray
                )
            )
            holder.equalizerView.visibility = View.GONE
            holder.replyImageView.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.light_gray2
                )
            )
            holder.contextMenuImageView.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.light_gray2
                )
            )
        }
    }

    override fun getItemCount() = tracks.size
    fun updateCurrentTrack(track: Track?) {
        currentTrack = track
        notifyDataSetChanged()
    }

}

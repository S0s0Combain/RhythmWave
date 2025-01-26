package com.example.rhythmwave

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaylistAdapter(
    private val playlists: List<Playlist>,
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onPlayButtonClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.playlistNameTextView)
        val playlistImageView: ImageView = itemView.findViewById(R.id.playlistImageView)
        val playButton: ImageButton = itemView.findViewById(R.id.playButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.nameTextView.text = playlist.name

        if (playlist.image != null) {
            val bitmap = BitmapFactory.decodeByteArray(playlist.image, 0, playlist.image.size)
            holder.playlistImageView.setImageBitmap(bitmap)
        } else {
            holder.playlistImageView.setImageResource(R.drawable.playlist_deafult)
        }

        holder.itemView.setOnClickListener { onPlaylistClick(playlist) }
        holder.playButton.setOnClickListener { onPlayButtonClick(playlist) }
    }

    override fun getItemCount() = playlists.size
}

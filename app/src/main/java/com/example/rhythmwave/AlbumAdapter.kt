package com.example.rhythmwave

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlbumAdapter(
    private val onAlbumClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    private var albums: List<Album> = emptyList()

    fun updateAlbums(newAlbums: List<Album>) {
        albums = newAlbums
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album)
    }

    override fun getItemCount(): Int = albums.size

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val albumImage: ImageView = itemView.findViewById(R.id.albumImage)
        private val albumName: TextView = itemView.findViewById(R.id.albumName)
        private val albumArtist: TextView = itemView.findViewById(R.id.albumArtist)

        fun bind(album: Album) {
            albumName.text = album.name
            albumArtist.text = album.artist
            if (album.albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(album.albumArt, 0, album.albumArt.size)
                albumImage.setImageBitmap(bitmap)
            } else {
                //albumImage.setImageResource(R.drawable.default_album_art)
            }
            itemView.setOnClickListener { onAlbumClick(album) }
        }
    }
}

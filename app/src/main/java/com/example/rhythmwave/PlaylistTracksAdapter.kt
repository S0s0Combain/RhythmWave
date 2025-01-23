package com.example.rhythmwave

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PlaylistTracksAdapter(
    private val onTrackClick: (Track) -> Unit,
    private val onShareClick: (Track) -> Unit,
    private val onDeleteTrackClick: (Track) -> Unit
) : RecyclerView.Adapter<PlaylistTracksAdapter.PlaylistTracksViewHolder>() {

    private var tracks: List<Track> = emptyList()
    private var currentTrack: Track? = null

    fun updateTracks(newTracks: List<Track>) {
        tracks = newTracks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistTracksViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_track, parent, false)
        return PlaylistTracksViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistTracksViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track)
    }

    override fun getItemCount(): Int = tracks.size

    inner class PlaylistTracksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackTitle: TextView = itemView.findViewById(R.id.titleTextView)
        private val trackArtist: TextView = itemView.findViewById(R.id.artistTextView)
        val trackImageView = itemView.findViewById<ImageView>(R.id.trackImage)
        private val equalizerView = itemView.findViewById<EqualizerView>(R.id.equalizerView)
        private val deleteTrackFromPlaylist: ImageButton =
            itemView.findViewById(R.id.deleteTrackFromPlaylist)
        val replyImageView = itemView.findViewById<ImageView>(R.id.replyImageView)

        fun bind(track: Track) {
            trackTitle.text = track.title
            trackArtist.text = track.artist
            if (track.albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
                val roundedBitmap = ImageUtils.roundCorner(bitmap, 40f)
                trackImageView.setImageBitmap(roundedBitmap)
            } else {

            }
            if (track == currentTrack) {
                trackTitle.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.accent_color_blue
                    )
                )
                replyImageView.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.accent_color_blue
                    )
                )

                deleteTrackFromPlaylist.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.accent_color_blue
                    )
                )
                equalizerView.visibility = View.VISIBLE
            } else {
                trackTitle.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.light_gray
                    )
                )
                replyImageView.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.light_gray2
                    )
                )
                deleteTrackFromPlaylist.setColorFilter(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.light_gray2
                    )
                )
                equalizerView.visibility = View.GONE
                itemView.setOnClickListener { onTrackClick(track) }
            }
            replyImageView.setOnClickListener { onShareClick(track) }
            deleteTrackFromPlaylist.setOnClickListener { onDeleteTrackClick(track) }
        }
    }

    fun updateCurrentTrack(track: Track?) {
        currentTrack = track
        notifyDataSetChanged()
    }
}
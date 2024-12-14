package com.example.rhythmwave

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(
    private val onTrackClick: (Track) -> Unit,
    private val onShareClick: (Track) -> Unit,
    private val onDeleteTrack: (Track) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    private var tracks: List<Track> = emptyList()
    private var currentTrack: Track? = null

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.titleTextView)
        val artistTextView = itemView.findViewById<TextView>(R.id.artistTextView)
        val trackImageView = itemView.findViewById<ImageView>(R.id.trackImage)
        val equalizerView = itemView.findViewById<EqualizerView>(R.id.equalizerView)
        val replyImageView = itemView.findViewById<ImageView>(R.id.replyImageView)
        val contextMenuImageView = itemView.findViewById<ImageButton>(R.id.contextMenuImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
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
        holder.replyImageView.setOnClickListener { onShareClick(track) }
        holder.contextMenuImageView.setOnClickListener { showContextMenu(holder.contextMenuImageView, track) }

        if (track == currentTrack) {
            holder.titleTextView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.neon_purple
                )
            )
            holder.equalizerView.visibility = View.VISIBLE
        } else {
            holder.titleTextView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.light_gray
                )
            )
            holder.equalizerView.visibility = View.GONE
        }
    }

    fun getTracks(): List<Track> {
        return tracks
    }

    private fun showContextMenu(view: View, track: Track) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.context_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.menu_delete -> {
                    onDeleteTrack(track)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun updateTracks(newTracks: List<Track>) {
        val diffCallback = TrackDiffCallback(tracks, newTracks)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        tracks = newTracks
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateCurrentTrack(track: Track?) {
        currentTrack = track
        notifyDataSetChanged()
    }

    class TrackDiffCallback(
        private val oldList: List<Track>,
        private val newList: List<Track>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
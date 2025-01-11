package com.example.rhythmwave

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        holder.contextMenuImageView.setOnClickListener {
            showContextMenu(
                holder.contextMenuImageView,
                track
            )
        }

        if (track == currentTrack) {
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

    fun getTracks(): List<Track> {
        return tracks
    }

    private fun showContextMenu(view: View, track: Track) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.context_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {
                    onDeleteTrack(track)
                    true
                }

                R.id.menu_add_to_favorites -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val favoriteTrackDao = AppDatabase.getDatabase(view.context).favoriteTrackDao()
                        val existingCount = favoriteTrackDao.isTrackFavorite(track.id)

                        if (existingCount > 0) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(view.context, "Трек уже добавлен в избранные", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            favoriteTrackDao.insert(FavoriteTrack(track.id, track.title, track.artist, track.duration, track.albumArt))
                            withContext(Dispatchers.Main) {
                                Toast.makeText(view.context, "Трек добавлен в избранные", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    true
                }

                R.id.menu_add_to_playlist -> {
                    showPlaylistDialog(view.context, track)
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

    private fun showPlaylistDialog(context: Context, track: Track) {
        CoroutineScope(Dispatchers.IO).launch {
            val savedPlaylists = AppDatabase.getDatabase(context).playlistDao().getAllPlaylists()

            withContext(Dispatchers.Main) {
                val playlistNames = savedPlaylists.map { it.name }.toTypedArray()

                val builder = MaterialAlertDialogBuilder(context)
                builder.setTitle("Выберите плейлист или создайте новый")

                val adapter =
                    ArrayAdapter(context, android.R.layout.simple_list_item_1, playlistNames)

                builder.setAdapter(adapter) { dialog, which ->
                    val selectedPlaylist = savedPlaylists[which]
                    addTrackToPlaylist(context, selectedPlaylist.id, track)
                }

                builder.setNegativeButton("Создать новый плейлист") { dialog, which ->
                    showCreatePlaylistDialog(context, track)
                }
                builder.show()
            }
        }
    }

    private fun showCreatePlaylistDialog(context: Context, track: Track) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle("Создание нового плейлиста")

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton("Создать") { dialog, which ->
            val newPlaylistName = input.text.toString()
            if (newPlaylistName.isNotEmpty()) {
                createNewPlaylist(context, newPlaylistName, track)
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

    private fun addTrackToPlaylist(context: Context, playlistId: Long, track: Track) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                AppDatabase.getDatabase(context).playlistTrackDao()
                    .insert(PlaylistTrack(playlistId = playlistId, trackId = track.id))
            }
        }
        Toast.makeText(
            context,
            "Трек '${track.title}' добавлен в плейлист",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun createNewPlaylist(context: Context, playlistName: String, track: Track) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val newPlaylist = AppDatabase.getDatabase(context).playlistDao()
                    .insert(Playlist(name = playlistName, createdAt = System.currentTimeMillis()))
                AppDatabase.getDatabase(context).playlistTrackDao()
                    .insert(PlaylistTrack(playlistId = newPlaylist, trackId = track.id))
            }
        }
    }
}
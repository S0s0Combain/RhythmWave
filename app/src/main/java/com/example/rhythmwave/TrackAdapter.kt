package com.example.rhythmwave

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackAdapter(
    private val tracks: List<Track>,
    private val onTrackClick: (Track) -> Unit
) :
    RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {
    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.titleTextView)
        val artistTextView = itemView.findViewById<TextView>(R.id.artistTextView)
        val trackImageView = itemView.findViewById<ImageView>(R.id.trackImage)
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
        holder.artistTextView.text = track.author
        if(track.albumArt != null){
            val bitmap = BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
            val roundedBitmap = roundCorner(bitmap, 40f)
            holder.trackImageView.setImageBitmap(roundedBitmap)
        } else{

        }
        holder.itemView.setOnClickListener { onTrackClick(track) }
    }

    fun roundCorner(src: Bitmap, round: Float):Bitmap{
        val width = src.width
        val height = src.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawARGB(0,0,0,0)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)

        canvas.drawRoundRect(rectF, round, round, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        return result
    }
}
package com.example.rhythmwave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PresetAdapter(private val presets: List<Preset>, private val onPresetSelected: (Preset) -> Unit) : RecyclerView.Adapter<PresetAdapter.PresetViewHolder>() {

    class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val presetNameTextView: TextView = itemView.findViewById(R.id.presetNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.preset_item, parent, false)
        return PresetViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val preset = presets[position]
        holder.presetNameTextView.text = preset.name
        holder.itemView.setOnClickListener { onPresetSelected(preset) }
    }

    override fun getItemCount(): Int = presets.size
}

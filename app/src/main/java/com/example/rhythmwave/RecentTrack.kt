package com.example.rhythmwave

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_tracks")
data class RecentTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val timestamp: Long
)

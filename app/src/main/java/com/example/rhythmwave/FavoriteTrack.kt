package com.example.rhythmwave

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteTrack(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val duration: Int,
    val albumArt: ByteArray?
)

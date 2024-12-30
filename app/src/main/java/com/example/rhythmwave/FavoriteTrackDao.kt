package com.example.rhythmwave

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface FavoriteTrackDao {
    @Insert
    suspend fun insert(track: FavoriteTrack)

    @Delete
    suspend fun delete(track: FavoriteTrack)

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteTrack>
}
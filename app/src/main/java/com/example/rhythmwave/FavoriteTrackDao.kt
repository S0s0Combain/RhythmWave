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

    @Query("SELECT COUNT(*) FROM favorites WHERE id = :trackId")
    suspend fun isTrackFavorite(trackId: Long): Int

    @Query("DELETE FROM favorites")
    suspend fun clearFavoritesTracks()

    @Query("DELETE FROM favorites WHERE id = :trackId")
    suspend fun removeTrackFromFavorites(trackId: Long)
}
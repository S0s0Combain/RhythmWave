package com.example.rhythmwave

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecentTrackDao {
    @Insert
    suspend fun insert(recentTrack: RecentTrack)

    @Query("SELECT * FROM recent_tracks WHERE timestamp >= :cutoffTime ORDER BY timestamp DESC")
    suspend fun getRecentTracks(cutoffTime: Long): List<RecentTrack>

    @Query("DELETE FROM recent_tracks WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: Long): Int

    @Query("DELETE FROM recent_tracks WHERE timestamp < :cutoffTime")
    suspend fun deleteOldRecords(cutoffTime: Long): Int
}
package com.example.rhythmwave

interface TrackControlCallback {
    fun onTrackChanged(track: Track)
    fun onPlaybackStateChanged(isPlaying: Boolean)
}
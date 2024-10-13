package com.example.rhythmwave

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MusicService : Service() {
    private lateinit var exoPlayer: ExoPlayer
    private var currentTrack: Track? = null
    private var trackList: List<Track> = emptyList()
    private var currentTrackIndex: Int = 0
    private var trackControlCallback: TrackControlCallback? = null

    private val binder = MusicServiceBinder()

    override fun onCreate() {
        super.onCreate()
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : com.google.android.exoplayer2.Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == com.google.android.exoplayer2.Player.STATE_ENDED) {
                    nextTrack()
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun playTrack(track: Track) {
        currentTrack = track
        currentTrackIndex = trackList.indexOf(track)
        val mediaItem = MediaItem.fromUri(getTrackUri(track.id))
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, "MusicPlayer")
            )
        ).createMediaSource(mediaItem)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        trackControlCallback?.onTrackChanged(track)
    }

    fun pauseTrack() {
        exoPlayer.playWhenReady = false
    }

    fun resumeTrack() {
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
    }

    fun previousTrack() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--
            playTrack(trackList[currentTrackIndex])
        }
    }

    fun nextTrack() {
        if (currentTrackIndex < trackList.size - 1) {
            currentTrackIndex++
            playTrack(trackList[currentTrackIndex])
        }
    }

    fun seekTo(position: Int) {
        exoPlayer.seekTo(position.toLong())
    }

    fun getCurrentTrack(): Track? = currentTrack

    fun getCurrentPosition(): Int = exoPlayer.currentPosition.toInt()

    fun getTrackUri(trackId: Long): Uri = Uri.withAppendedPath(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId.toString()
    )

    fun setTrackList(tracks: List<Track>) {
        trackList = tracks
        currentTrackIndex = 0
    }

    fun isPlaying(): Boolean {
        return exoPlayer.playWhenReady
    }

    fun setTrackControlCallback(callback: TrackControlCallback){
        trackControlCallback = callback
    }

    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}
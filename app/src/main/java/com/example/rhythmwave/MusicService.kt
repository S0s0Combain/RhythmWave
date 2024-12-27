package com.example.rhythmwave

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.androidgamesdk.gametextinput.Listener

class MusicService : Service() {
    private lateinit var exoPlayer: ExoPlayer
    private var currentTrack: Track? = null
    private var trackList: List<Track> = emptyList()
    private var currentTrackIndex: Int = 0
    private var trackControlCallback: TrackControlCallback? = null
    private lateinit var equalizerHelper: EqualizerHelper

    private val binder = MusicServiceBinder()

    companion object {
        @Volatile
        private var instance: MusicService? = null

        fun getInstance(): MusicService? {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        equalizerHelper = EqualizerHelper(getAudioSessionId())

        (exoPlayer as SimpleExoPlayer).addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
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

    fun applyEqualizerSettings(bassLevel: Int, trebleLevel: Int) {
        if (::equalizerHelper.isInitialized) {
            val bassBand = 0
            val trebleBand = 1

            equalizerHelper.setBandLevel(bassBand.toShort(), (bassLevel * 10).toShort())
            equalizerHelper.setBandLevel(trebleBand.toShort(), (trebleLevel * 10).toShort())
        }
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
        notifyPlaybackStateChanged(true)
    }

    fun pauseTrack() {
        exoPlayer.playWhenReady = false
        notifyPlaybackStateChanged(false)
    }

    fun resumeTrack() {
        exoPlayer.playWhenReady = true
        notifyPlaybackStateChanged(true)
    }

    fun togglePlayPause() {
        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
        notifyPlaybackStateChanged(exoPlayer.playWhenReady)
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
        } else {
            currentTrackIndex = 0
        }
        playTrack(trackList[currentTrackIndex])
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

    fun setTrackControlCallback(callback: TrackControlCallback) {
        trackControlCallback = callback
    }

    fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        trackControlCallback?.onPlaybackStateChanged(isPlaying)
    }

    fun getAudioSessionId(): Int {
        return exoPlayer.audioSessionId
    }

    fun getTrackList(): List<Track> = trackList

    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (::equalizerHelper.isInitialized) {
            equalizerHelper.release()
        }
        exoPlayer.release()
    }
}
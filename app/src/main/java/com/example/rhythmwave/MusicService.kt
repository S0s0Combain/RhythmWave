package com.example.rhythmwave

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicService : Service() {
    private lateinit var exoPlayer: ExoPlayer
    private var currentTrack: Track? = null
    private var trackList: List<Track> = emptyList()
    private var currentTrackIndex: Int = 0
    private var trackControlCallback: TrackControlCallback? = null
    private lateinit var equalizer: Equalizer
    private var trackAdapter: TrackAdapter? = null
    private var favoriteTrackAdapter: FavoriteTrackAdapter? = null
    private var playlistTrackAdapter: PlaylistTracksAdapter? = null
    private var updateFavoriteAdapter: Boolean = false
    private var updatePlaylistAdapter: Boolean = false
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionCallback: MediaSessionCompat.Callback

    private val binder = MusicServiceBinder()

    companion object {
        @Volatile
        private var instance: MusicService? = null

        fun getInstance(): MusicService? {
            return instance
        }

        const val ACTION_PLAY_PAUSE = "com.example.rhythmwave.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.rhythmwave.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.rhythmwave.ACTION_PREVIOUS"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        equalizer = Equalizer(0, getAudioSessionId())
        equalizer.enabled = true

        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSessionCallback = object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                resumeTrack()
            }

            override fun onPause() {
                super.onPause()
                pauseTrack()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                nextTrack()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                previousTrack()
            }

            override fun onStop() {
                super.onStop()
                pauseTrack()
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                seekTo(pos.toInt())
            }
        }
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.isActive = true

        (exoPlayer as SimpleExoPlayer).addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    nextTrack()
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> togglePlayPause()
                ACTION_NEXT -> nextTrack()
                ACTION_PREVIOUS -> previousTrack()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun applyEqualizerSettings(
        bassLevel: Int,
        level60Hz: Int,
        level230Hz: Int,
        level910Hz: Int,
        level3_6kHz: Int,
        level14kHz: Int
    ) {
        if (::equalizer.isInitialized) {
            equalizer.setBandLevel(0, (level60Hz * 10).toShort()) // 60Hz
            equalizer.setBandLevel(1, (level230Hz * 10).toShort()) // 230Hz
            equalizer.setBandLevel(2, (level910Hz * 10).toShort()) // 910Hz
            equalizer.setBandLevel(3, (level3_6kHz * 10).toShort()) // 3.6kHz
            equalizer.setBandLevel(4, (level14kHz * 10).toShort()) // 14.0kHz

            val bassBoost = BassBoost(0, getAudioSessionId())
            bassBoost.setEnabled(true)
            bassBoost.setStrength((bassLevel * 10).toShort())

            equalizer.setEnabled(true)
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
        CoroutineScope(Dispatchers.IO).launch {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            AppDatabase.getDatabase(applicationContext).recentTrackDao().deleteByTrackId(track.id)
            AppDatabase.getDatabase(applicationContext).recentTrackDao().insert(
                RecentTrack(trackId = track.id, timestamp = System.currentTimeMillis())
            )
            AppDatabase.getDatabase(applicationContext).recentTrackDao()
                .deleteOldRecords(sevenDaysAgo)
        }
        if (updateFavoriteAdapter) {
            favoriteTrackAdapter?.updateCurrentTrack(track)
        } else if (updatePlaylistAdapter) {
            playlistTrackAdapter?.updateCurrentTrack(track)
        } else {
            trackAdapter?.updateCurrentTrack(track)
        }
        notifyPlaybackStateChanged(true)
        updateNotification()
    }

    fun setTrackAdapter(trackAdapter: TrackAdapter) {
        this.trackAdapter = trackAdapter
        updateFavoriteAdapter = false
        updatePlaylistAdapter = false
    }

    fun setFavoriteTrackAdapter(favoriteTrackAdapter: FavoriteTrackAdapter) {
        this.favoriteTrackAdapter = favoriteTrackAdapter
        updateFavoriteAdapter = true
        updatePlaylistAdapter = false
    }

    fun setPlaylistTrackAdapter(playlistTrackAdapter: PlaylistTracksAdapter) {
        this.playlistTrackAdapter = playlistTrackAdapter
        updateFavoriteAdapter = false
        updatePlaylistAdapter = true
    }

    fun pauseTrack() {
        exoPlayer.playWhenReady = false
        notifyPlaybackStateChanged(false)
        updateNotification()
    }

    fun resumeTrack() {
        exoPlayer.playWhenReady = true
        notifyPlaybackStateChanged(true)
        updateNotification()
    }

    fun togglePlayPause() {
        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
        notifyPlaybackStateChanged(exoPlayer.playWhenReady)
        updateNotification()
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

    fun shuffleTrackList() {
        trackList = trackList.shuffled()
        currentTrackIndex = 0
        if (!trackList.isEmpty()) {
            playTrack(trackList[currentTrackIndex])
        }
    }

    fun setTrackControlCallback(callback: TrackControlCallback) {
        trackControlCallback = callback
    }

    fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        trackControlCallback?.onPlaybackStateChanged(isPlaying)
        updatePlaybackState(isPlaying)
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
        if (::equalizer.isInitialized) {
            equalizer.release()
        }
        exoPlayer.release()
    }

    private fun createNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "music_channel"
        val channelName = "Music Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

        val playPauseIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap: Bitmap? = currentTrack?.albumArt?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }

        val playPauseIcon = if (exoPlayer.playWhenReady) {
            R.drawable.baseline_pause_24
        } else {
            R.drawable.baseline_play_arrow_24
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(currentTrack?.title ?: "No Track")
            .setContentText(currentTrack?.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(bitmap)
            .setStyle(mediaStyle)
            .addAction(R.drawable.baseline_skip_previous_24, "Previous", prevIntent)
            .addAction(playPauseIcon, "Pause", playPauseIntent)
            .addAction(R.drawable.baseline_skip_next_24, "Next", nextIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification()
        notificationManager.notify(1, notification)
    }

    private fun
            updatePlaybackState(isPlaying: Boolean) {
        val playbackState = PlaybackStateCompat.Builder()
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                exoPlayer.currentPosition,
                1.0f
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
    }
}

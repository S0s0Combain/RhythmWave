package com.example.rhythmwave

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity(), TrackControlCallback {
    private lateinit var tracksList: RecyclerView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var trackControlLayout: ConstraintLayout
    private lateinit var trackImage: ImageView
    private lateinit var trackTitleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MyLog", "Сервис подключен")
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
            musicService?.setTrackControlCallback(this@MainActivity)
            loadTracks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("MyLog", "Сервис не подключен")
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tracksList = findViewById(R.id.tracksRecyclerView)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        tracksList.addItemDecoration(SpacesItemDecoration(spaceInPixels))
        trackControlLayout = findViewById(R.id.trackControlLayout)
        trackImage = findViewById(R.id.trackImage)
        trackTitleTextView = findViewById(R.id.trackTitleTextView)
        artistTextView = findViewById(R.id.artistTextView)
        prevButton = findViewById(R.id.prevButton)
        pauseButton = findViewById(R.id.pauseButton)
        nextButton = findViewById(R.id.nextButton)
        fragmentContainer = findViewById(R.id.fragmentContainer)

        prevButton.setOnClickListener { musicService?.previousTrack() }
        pauseButton.setOnClickListener { musicService?.togglePlayPause() }
        nextButton.setOnClickListener { musicService?.nextTrack() }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                1
            )
            Log.d("MyLog", "разрешения не предоставлены")
        } else {
            Log.d("MyLog", "разрешения предоставлены")
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun loadTracks() {
        Log.d("MyLog", "Функция вызвана")
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        Log.d("MyLog", selection)
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.MIME_TYPE
        )
        Log.d("MyLog", projection.toString())
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        val tracks = mutableListOf<Track>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val title =
                        cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist =
                        cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val duration =
                        cursor.getInt(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val id = cursor.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val mimeType =
                        cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

                    if (duration < 30000) {
                        continue
                    }

                    if (mimeType != "audio/mpeg") {
                        continue
                    }

                    val albumArt = getAlbumArt(this, id)
                    tracks.add(Track(title, artist, duration, id, albumArt))
                } while (it.moveToNext())
            } else {
                Log.d("MyLog", "Треки не найдены")
            }
        }

        musicService?.setTrackList(tracks)

        tracksList.layoutManager = LinearLayoutManager(this)
        tracksList.adapter = TrackAdapter(tracks) { track ->
            if (musicService?.getCurrentTrack() == track) {
                if (musicService?.isPlaying() == true) {
                    musicService?.pauseTrack()
                } else {
                    musicService?.resumeTrack()
                    showTrackControl(track)
                    fragmentContainer.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                        .replace(R.id.fragmentContainer, PlayerFragment()).addToBackStack(null)
                        .commit()
                }
            } else {
                musicService?.playTrack(track)

                fragmentContainer.visibility = View.VISIBLE
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                    .replace(R.id.fragmentContainer, PlayerFragment()).addToBackStack(null).commit()
                showTrackControl(track)
            }
        }
    }

    private fun getAlbumArt(context: Context, trackId: Long): ByteArray? {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(trackId.toString())
        val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)

        var albumId: Long? = null
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    albumId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                }
            }

        if (albumId != null) {
            val albumArtUri: Uri = Uri.parse("content://media/external/audio/albumart/$albumId")
            return try {
                val inputStream = context.contentResolver.openInputStream(albumArtUri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                inputStream?.copyTo(byteArrayOutputStream)
                byteArrayOutputStream.toByteArray()
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    override fun onBackPressed() {
        if (fragmentContainer.visibility == View.VISIBLE) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    fragmentContainer.visibility = View.GONE
                    supportFragmentManager.popBackStack()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
            fragmentContainer.startAnimation(animation)
        } else {
            super.onBackPressed()
        }
    }

    private fun showTrackControl(track: Track) {
        trackControlLayout.visibility = View.VISIBLE
        if (track.albumArt != null) {
            val bitmap =
                BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
            val roundedBitmap = ImageUtils.roundCorner(bitmap, 40f)
            trackImage.setImageBitmap(roundedBitmap)
        }
        trackTitleTextView.text = track.title
        artistTextView.text = track.artist

        val playerFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateTrackInfo(track)
    }

    override fun onTrackChanged(track: Track) {
        showTrackControl(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }

        val playerFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
        playerFragment?.updateSeekbar(
            musicService?.getCurrentPosition() ?: 0,
            musicService?.getCurrentTrack()?.duration ?: 0
        )
    }
}
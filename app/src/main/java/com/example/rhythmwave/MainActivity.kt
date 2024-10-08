package com.example.rhythmwave

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var tracksList: RecyclerView
    private var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MyLog", "Сервис подключен")
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
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
            bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
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
                    val mimeType = cursor.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

                    if(duration<30000){
                        continue
                    }

                    if(mimeType !="audio/mpeg"){
                        continue
                    }

                    tracks.add(Track(title, artist, duration, id))
                } while (it.moveToNext())
            } else {
                Log.d("MyLog", "Треки не найдены")
            }
        }

        musicService?.setTrackList(tracks)

        tracksList.layoutManager = LinearLayoutManager(this)
        tracksList.adapter = TrackAdapter(tracks) { track ->
            if (musicService?.getCurrentTrack() == track) {
                musicService?.togglePlayPause()
            } else {
                musicService?.playTrack(track)
            }
        }
    }
}

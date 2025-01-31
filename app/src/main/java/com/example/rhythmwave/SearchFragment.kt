package com.example.rhythmwave

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

class SearchFragment : Fragment(), TrackControlCallback {
    private lateinit var searchEditText: EditText
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: TrackAdapter
    private lateinit var noResultsTextView: TextView
    private lateinit var downloadButton: Button
    private lateinit var trackList: MutableList<Track>
    private lateinit var pauseButton: ImageButton

    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
            musicService?.setTrackControlCallback(this@SearchFragment)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.space_between_items)
        searchRecyclerView.addItemDecoration(SpacesItemDecorations(spaceInPixels))
        noResultsTextView = view.findViewById(R.id.noResultsTextView)
        downloadButton = view.findViewById(R.id.downloadButton)
        pauseButton = (activity as MainActivity).findViewById(R.id.pauseButton)

        searchAdapter = TrackAdapter(
            onTrackClick = { track -> onTrackClick(track) },
            onShareClick = { track ->
                TrackUtils.shareTrack(
                    requireContext(),
                    track,
                    requireContext().contentResolver
                )
            },
            onDeleteTrack = { track -> deleteTrack(track) }
        )
        searchRecyclerView.layoutManager = LinearLayoutManager(context)
        searchRecyclerView.adapter = searchAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                downloadButton.visibility = View.VISIBLE
                performSearch(searchEditText.text.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchEditText.right - searchEditText.compoundDrawables[2].bounds.width())) {
                    startVoiceRecognitionActivity()
                    return@setOnTouchListener true
                }
            }
            false
        }

        downloadButton.setOnClickListener {
            val query = searchEditText.text.toString()
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=$query download")
            )
            startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        loadTracks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound) {
            context?.unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun startVoiceRecognitionActivity() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите что-нибудь")
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                searchEditText.setText(result?.get(0) ?: "")
                performSearch(result?.get(0) ?: "")
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchAdapter.updateTracks(emptyList())
            noResultsTextView.visibility = View.VISIBLE
        } else {
            val filteredTracks = filterTracks(query)
            if (filteredTracks.isEmpty()) {
                noResultsTextView.visibility = View.VISIBLE
            } else {
                noResultsTextView.visibility = View.GONE
            }
            searchAdapter.updateTracks(filteredTracks)
        }
    }

    private fun filterTracks(query: String): List<Track> {
        return trackList.filter { track ->
            track.title.contains(query, ignoreCase = true) || track.artist.contains(
                query,
                ignoreCase = true
            )
        }
    }

    companion object {
        const val REQUEST_CODE_SPEECH_INPUT = 100
    }

    private fun loadTracks() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val cursor: Cursor? = context?.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        trackList = mutableListOf()
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

                    if (duration < 30000 || mimeType != "audio/mpeg") {
                        continue
                    }

                    val albumArt = getAlbumArt(requireContext(), id)
                    trackList.add(Track(title, artist, duration, id, albumArt))
                } while (it.moveToNext())
            }
        }
        musicService?.setTrackList(trackList)
    }

    private fun onTrackClick(track: Track) {
        val currentTrackList = musicService?.getTrackList() ?: return
        if (currentTrackList != trackList) {
            musicService?.setTrackList(trackList)
        }
        searchAdapter.updateCurrentTrack(track)
        if (musicService?.getCurrentTrack() == track) {
            if (musicService?.isPlaying() == true) {
                musicService?.pauseTrack()
            } else {
                musicService?.resumeTrack()
                openPlayerFragment(musicService)
            }
        } else {
            musicService?.playTrack(track)
            openPlayerFragment(musicService)
        }
    }

    override fun onTrackChanged(track: Track) {
        searchAdapter.updateCurrentTrack(track)
        if (isAdded) {
            val fragmentManager = parentFragmentManager
            val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is PlayerFragment) {
                val playerFragment =
                    parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
                playerFragment?.updateTrackInfo(track)
            }
        }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }
        if (isAdded) {
            val playerFragment =
                parentFragmentManager.findFragmentById(R.id.fragmentContainer) as? PlayerFragment
            playerFragment?.updateSeekbar(
                musicService?.getCurrentPosition() ?: 0,
                musicService?.getCurrentTrack()?.duration ?: 0
            )
        }
    }

    private fun deleteTrack(track: Track) {
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(track.id.toString())
        context?.contentResolver?.delete(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            selection,
            selectionArgs
        )

        val updatedTrackList =
            musicService?.getTrackList()?.filter { it.id != track.id } ?: emptyList()
        musicService?.setTrackList(updatedTrackList)
        searchAdapter.updateTracks(updatedTrackList)
    }

    private fun openPlayerFragment(musicService: MusicService?) {
        val playerFragment = PlayerFragment().apply {
            this.musicService = musicService
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
            .add(R.id.fragmentContainer, playerFragment)
            .addToBackStack(null)
            .commit()
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
}
package com.example.rhythmwave

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchFragment : Fragment() {
    private lateinit var searchEditText: EditText
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: TrackAdapter
    private lateinit var noResultsTextView: TextView
    private lateinit var downloadButton: Button
    private lateinit var trackList: List<Track>

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

        searchAdapter = TrackAdapter(onTrackClick = { track ->
        }, onShareClick = { track ->

        }, onDeleteTrack = {})
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

    fun setTrackList(tracks: List<Track>) {
        this.trackList = tracks
    }
}
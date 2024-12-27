package com.example.rhythmwave

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EqualizerActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var seekBarBass: SeekBar
    private lateinit var seekBarTreble: SeekBar
    private lateinit var bassTextView: TextView
    private lateinit var trebleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_equalizer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("EqualizerSettings", MODE_PRIVATE)

        seekBarBass = findViewById(R.id.seekBarBass)
        seekBarTreble = findViewById(R.id.seekBarTreble)
        bassTextView = findViewById(R.id.bassTextView)
        trebleTextView = findViewById(R.id.trebleTextView)

        loadEqualizerSettings()

        seekBarBass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                bassTextView.text = "Bass: $progress"
                MusicService.getInstance()?.applyEqualizerSettings(progress, seekBarTreble.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveEqualizerSettings()
            }
        })

        seekBarTreble.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                trebleTextView.text = "Treble: $progress"
                MusicService.getInstance()?.applyEqualizerSettings(seekBarBass.progress, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveEqualizerSettings()
            }
        })
    }

    private fun loadEqualizerSettings() {
        val bass = sharedPreferences.getInt("Bass", 0)
        val treble = sharedPreferences.getInt("Treble", 0)

        seekBarBass.progress = bass
        seekBarTreble.progress = treble
        bassTextView.text = "Bass: $bass"
        trebleTextView.text = "Treble: $treble"
    }

    private fun saveEqualizerSettings() {
        val editor = sharedPreferences.edit()
        editor.putInt("Bass", seekBarBass.progress)
        editor.putInt("Treble", seekBarTreble.progress)
        editor.apply()
    }
}

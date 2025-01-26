package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Orientation

class EqualizerActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var seekBarBass: SeekBar
    private lateinit var seekBar60Hz: SeekBar
    private lateinit var seekBar230Hz: SeekBar
    private lateinit var seekBar910Hz: SeekBar
    private lateinit var seekBar3_6kHz: SeekBar
    private lateinit var seekBar14_0kHz: SeekBar

    private lateinit var bassTextView: TextView
    private lateinit var frequency60HzTextView: TextView
    private lateinit var frequency230HzTextView: TextView
    private lateinit var frequency910HzTextView: TextView
    private lateinit var frequency3_6kHzTextView: TextView
    private lateinit var frequency14_0kHzTextView: TextView

    private lateinit var backButton: ImageButton
    private lateinit var presetRecyclerView: RecyclerView
    private lateinit var presetAdapter: PresetAdapter
    private lateinit var volumeSwitch: SwitchCompat

    private lateinit var musicService: MusicService
    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_equalizer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.navigationBarColor = ContextCompat.getColor(this, R.color.primary_background)
        sharedPreferences = getSharedPreferences("EqualizerSettings", MODE_PRIVATE)

        seekBarBass = findViewById(R.id.bassBoostSeekBar)
        seekBar60Hz = findViewById(R.id.seekBar60Hz)
        seekBar230Hz = findViewById(R.id.seekBar230Hz)
        seekBar910Hz = findViewById(R.id.seekBar910Hz)
        seekBar3_6kHz = findViewById(R.id.seekBar3_6kHz)
        seekBar14_0kHz = findViewById(R.id.seekBar14_0kHz)

        bassTextView = findViewById(R.id.bassTextView)
        frequency60HzTextView = findViewById(R.id.frequency60HzTextView)
        frequency230HzTextView = findViewById(R.id.frequency230HzTextView)
        frequency910HzTextView = findViewById(R.id.frequency910HzTextView)
        frequency3_6kHzTextView = findViewById(R.id.frequency3_6kHzTextView)
        frequency14_0kHzTextView = findViewById(R.id.frequency14_0kHzTextView)
        volumeSwitch = findViewById(R.id.volumeSwitch)
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener { onBackPressed() }

        setSeekBarLimits(seekBarBass)
        setSeekBarLimits(seekBar60Hz)
        setSeekBarLimits(seekBar230Hz)
        setSeekBarLimits(seekBar910Hz)
        setSeekBarLimits(seekBar3_6kHz)
        setSeekBarLimits(seekBar14_0kHz)

        loadEqualizerSettings()

        setupSeekBarListener(seekBarBass, bassTextView, "Bass")
        setupSeekBarListener(seekBar60Hz, frequency60HzTextView, "60Hz")
        setupSeekBarListener(seekBar230Hz, frequency230HzTextView, "230Hz")
        setupSeekBarListener(seekBar910Hz, frequency910HzTextView, "910Hz")
        setupSeekBarListener(seekBar3_6kHz, frequency3_6kHzTextView, "3.6kHz")
        setupSeekBarListener(seekBar14_0kHz, frequency14_0kHzTextView, "14.0kHz")
        volumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBound) {
                musicService.setVirtualizerEnabled(isChecked)
                saveVirtualizerSetting(isChecked)
            }
        }

        volumeSwitch.isChecked = loadVirtualizerSetting()

        presetRecyclerView = findViewById(R.id.presetRecyclerView)
        presetRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        presetAdapter = PresetAdapter(getPresets()) { preset ->
            applyPreset(preset)
        }
        presetRecyclerView.adapter = presetAdapter
    }

    private fun getPresets(): List<Preset> {
        return listOf(
            Preset("Рок", 70, 60, 50, 40, 30, 20),
            Preset("Поп", 50, 40, 30, 20, 10, 0),
            Preset("Классика", 30, 20, 10, 0, 10, 20),
            Preset("Джаз", 40, 30, 20, 10, 0, 10),
            Preset("Электроника", 80, 70, 60, 50, 40, 30),
            Preset("Хип-хоп", 90, 80, 70, 60, 50, 40),
            Preset("Акустика", 40, 30, 20, 10, 0, 10),
            Preset("Танцевальная", 70, 60, 50, 40, 30, 20),
            Preset("Вокал", 30, 20, 10, 0, 10, 20),
            Preset("Плоский", 50, 50, 50, 50, 50, 50),
            Preset("Усиление басов", 100, 90, 80, 70, 60, 50),
            Preset("Усиление высоких частот", 30, 40, 50, 60, 70, 80),
            Preset("Громкость", 60, 50, 40, 30, 20, 10)
        )
    }


    private fun applyPreset(preset: Preset) {
        seekBarBass.progress = preset.bass
        seekBar60Hz.progress = preset.level60Hz
        seekBar230Hz.progress = preset.level230Hz
        seekBar910Hz.progress = preset.level910Hz
        seekBar3_6kHz.progress = preset.level3_6kHz
        seekBar14_0kHz.progress = preset.level14kHz
        updateTextViews()
        applyEqualizerSettings()
        saveEqualizerSettings()
    }

    private fun loadEqualizerSettings() {
        seekBarBass.progress = sharedPreferences.getInt("Bass", 50)
        seekBar60Hz.progress = sharedPreferences.getInt("60Hz", 50)
        seekBar230Hz.progress = sharedPreferences.getInt("230Hz", 50)
        seekBar910Hz.progress = sharedPreferences.getInt("910Hz", 50)
        seekBar3_6kHz.progress = sharedPreferences.getInt("3.6kHz", 50)
        seekBar14_0kHz.progress = sharedPreferences.getInt("14.0kHz", 50)
        updateTextViews()
    }

    private fun saveEqualizerSettings() {
        val editor = sharedPreferences.edit()
        editor.putInt("Bass", seekBarBass.progress)
        editor.putInt("60Hz", seekBar60Hz.progress)
        editor.putInt("230Hz", seekBar230Hz.progress)
        editor.putInt("910Hz", seekBar910Hz.progress)
        editor.putInt("3.6kHz", seekBar3_6kHz.progress)
        editor.putInt("14.0kHz", seekBar14_0kHz.progress)
        editor.apply()
    }

    private fun applyEqualizerSettings() {
        if (isBound) {
            musicService.applyEqualizerSettings(
                bassLevel = seekBarBass.progress,
                level60Hz = seekBar60Hz.progress,
                level230Hz = seekBar230Hz.progress,
                level910Hz = seekBar910Hz.progress,
                level3_6kHz = seekBar3_6kHz.progress,
                level14kHz = seekBar14_0kHz.progress
            )
        }
    }

    private fun loadVirtualizerSetting(): Boolean {
        return sharedPreferences.getBoolean("VirtualizerEnabled", false)
    }

    private fun saveVirtualizerSetting(enabled: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("VirtualizerEnabled", enabled)
        editor.apply()
    }

    private fun setupSeekBarListener(seekBar: SeekBar, textView: TextView, settingKey: String) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = "$settingKey: $progress"
                applyEqualizerSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveEqualizerSettings()
            }
        })
    }

    private fun setSeekBarLimits(seekBar: SeekBar) {
        seekBar.max = 100
        seekBar.progress = 50
    }

    private fun updateTextViews() {
        bassTextView.text = "Bass: ${seekBarBass.progress}"
        frequency60HzTextView.text = "60 Hz: ${seekBar60Hz.progress}"
        frequency230HzTextView.text = "230 Hz: ${seekBar230Hz.progress}"
        frequency910HzTextView.text = "910 Hz: ${seekBar910Hz.progress}"
        frequency3_6kHzTextView.text = "3.6 kHz: ${seekBar3_6kHz.progress}"
        frequency14_0kHzTextView.text = "14.0 kHz: ${seekBar14_0kHz.progress}"
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

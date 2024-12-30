package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EqualizerActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var seekBarBass: SeekBar
    private lateinit var seekBar60Hz: SeekBar
    private lateinit var seekBar230Hz: SeekBar
    private lateinit var seekBar910Hz: SeekBar
    private lateinit var seekBar3_6kHz: SeekBar
    private lateinit var seekBar14_0kHz: SeekBar

    private lateinit var bassTextView: TextView
    private lateinit var trebleTextView: TextView
    private lateinit var frequency60HzTextView: TextView
    private lateinit var frequency230HzTextView: TextView
    private lateinit var frequency910HzTextView: TextView
    private lateinit var frequency3_6kHzTextView: TextView
    private lateinit var frequency14_0kHzTextView: TextView

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

        // Устанавливаем пределы ползунков
        setSeekBarLimits(seekBarBass)
        setSeekBarLimits(seekBar60Hz)
        setSeekBarLimits(seekBar230Hz)
        setSeekBarLimits(seekBar910Hz)
        setSeekBarLimits(seekBar3_6kHz)
        setSeekBarLimits(seekBar14_0kHz)

        loadEqualizerSettings()

        // Установка обработчиков для ползунков
        setupSeekBarListener(seekBarBass, bassTextView, "Bass")
        setupSeekBarListener(seekBar60Hz, frequency60HzTextView, "60Hz")
        setupSeekBarListener(seekBar230Hz, frequency230HzTextView, "230Hz")
        setupSeekBarListener(seekBar910Hz, frequency910HzTextView, "910Hz")
        setupSeekBarListener(seekBar3_6kHz, frequency3_6kHzTextView, "3.6kHz")
        setupSeekBarListener(seekBar14_0kHz, frequency14_0kHzTextView, "14.0kHz")
    }

    private fun loadEqualizerSettings() {
        seekBarBass.progress = sharedPreferences.getInt("Bass", 0)
        seekBar60Hz.progress = sharedPreferences.getInt("60Hz", 0)
        seekBar230Hz.progress = sharedPreferences.getInt("230Hz", 0)
        seekBar910Hz.progress = sharedPreferences.getInt("910Hz", 0)
        seekBar3_6kHz.progress = sharedPreferences.getInt("3.6kHz", 0)
        seekBar14_0kHz.progress = sharedPreferences.getInt("14.0kHz", 0)

        // Обновление текстовых полей
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

    private fun setupSeekBarListener(seekBar: SeekBar, textView: TextView, settingKey: String) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = "$settingKey: $progress"
                applyEqualizerSettings() // Применение настроек эквалайзера
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveEqualizerSettings() // Сохранение настроек
            }
        })
    }

    private fun setSeekBarLimits(seekBar: SeekBar) {
        seekBar.max = 100 // Установите максимальное значение, если необходимо
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

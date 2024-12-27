package com.example.rhythmwave

import android.media.audiofx.Equalizer
import android.os.Build

class EqualizerHelper(private val audioSessionId: Int) {
    private val equalizer: Equalizer

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            equalizer = Equalizer(0, audioSessionId)
            equalizer.setEnabled(true)
            for (i in 0 until equalizer.numberOfBands) {
                equalizer.setBandLevel(i.toShort(), 0)
            }
        } else {
            equalizer = Equalizer(0, audioSessionId)
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            equalizer.setBandLevel(band, level)
        }
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            equalizer.release()
        }
    }
}
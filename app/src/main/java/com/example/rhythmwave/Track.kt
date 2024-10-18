package com.example.rhythmwave

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(val title: String, val artist: String, val duration: Int, val id: Long, val albumArt: ByteArray?) :
    Parcelable

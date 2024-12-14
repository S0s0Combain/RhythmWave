package com.example.rhythmwave

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val name: String,
    val artist: String,
    val albumId: Long,
    val albumArt: ByteArray?
) : Parcelable

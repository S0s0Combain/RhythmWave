package com.example.rhythmwave

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(val title: String, val author: String, val duration: Int, val id: Long) :
    Parcelable

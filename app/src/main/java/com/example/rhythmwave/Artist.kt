package com.example.rhythmwave
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artist(val name: String, val trackCount: Int) : Parcelable


package com.example.rhythmwave

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object TrackUtils {
    fun shareTrack(context: Context, track: Track, contentResolver: ContentResolver) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(track.id.toString())
        val cursor = contentResolver.query(uri, null, selection, selectionArgs, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val file = File(filePath)
                val mimeType = "audio/mpeg"
                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = mimeType
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
            }
        }
    }

}
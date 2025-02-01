package com.example.rhythmwave

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EditPlaylistFragment : Fragment() {
    private lateinit var playlistNameEditText: EditText
    private lateinit var playlistImageView: ImageView
    private lateinit var saveButton: Button
    private var playlistId: Long? = null
    private var selectedImageUri: Uri? = null
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistId = arguments?.getLong("playlistId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_edit_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        playlistNameEditText = view.findViewById(R.id.editPlaylistName)
        playlistImageView = view.findViewById(R.id.playlistImageView)
        saveButton = view.findViewById(R.id.saveButton)
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }

        loadPlaylist()

        view.findViewById<ImageButton>(R.id.selectImageButton).setOnClickListener {
            selectImageFromGallery()
        }

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadPlaylist() {
        playlistId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                val playlist = AppDatabase.getDatabase(requireContext()).playlistDao().getAllPlaylists().first { it.id == id }
                withContext(Dispatchers.Main) {
                    playlistNameEditText.setText(playlist.name)
                    playlist.image?.let {
                        val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                        playlistImageView.setImageBitmap(bitmap)
                    } ?: playlistImageView.setImageResource(R.drawable.default_image)
                }
            }
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            playlistImageView.setImageURI(selectedImageUri)
        }
    }

    private fun saveChanges() {
        val name = playlistNameEditText.text.toString()
        val imageData = selectedImageUri?.let { uri ->
            uriToByteArray(requireContext().contentResolver, uri)
        }

        playlistId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                val oldPlaylist = AppDatabase.getDatabase(requireContext()).playlistDao().getPlaylistById(id)
                AppDatabase.getDatabase(requireContext()).playlistDao().update(Playlist(id, name, oldPlaylist!!.createdAt, imageData ?: oldPlaylist.image))
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireActivity().applicationContext, "Плейлист обновлён", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun uriToByteArray(contentResolver: ContentResolver, uri: Uri?): ByteArray? {
        if (uri == null) return null
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true) // Например, 512 пикселей по ширине
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 1
    }
}

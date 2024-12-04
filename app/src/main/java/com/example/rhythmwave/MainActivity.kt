package com.example.rhythmwave

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var equalizerImageButton: ImageButton
    var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MyLog", "Сервис подключен")
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("MyLog", "Сервис не подключен")
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        searchEditText = findViewById(R.id.searchEditText)
        fragmentContainer = findViewById(R.id.fragmentContainer)
        equalizerImageButton = findViewById(R.id.equalizerImageButton)

        viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Tracks"
                1 -> tab.text = "Empty 1"
                2 -> tab.text = "Empty 2"
            }
        }.attach()

        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        searchEditText.setOnClickListener {
            val searchFragment = SearchFragment()
            searchFragment.setTrackList(musicService?.getTrackList() ?: listOf())
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, searchFragment).addToBackStack(null).commit()
            fragmentContainer.visibility = View.VISIBLE
        }
        equalizerImageButton.setOnClickListener { createEqualizerActivity() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private class ViewPagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TrackListFragment()
                else -> Fragment() // Пустые фрагменты
            }
        }
    }

    private fun createEqualizerActivity() {
        val intent = Intent(this, EqualizerActivity::class.java)
        startActivity(intent)
    }
}

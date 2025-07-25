package com.example.mymusic.main.pager

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mymusic.main.ArtworkWithWaveFormFragment
import com.example.mymusic.main.LyricsPageFragment
import com.example.mymusic.main.playlist.PlaylistRecyclerFragment
import com.example.mymusic.main.playtime.PlayTimeCalendar

class MusicPlayingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    @SuppressLint("UnsafeOptInUsageError")
    private val fragments : List<Fragment> = listOf(
        PlaylistRecyclerFragment(),
        ArtworkWithWaveFormFragment(),
        PlayTimeCalendar(),
        LyricsPageFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

}
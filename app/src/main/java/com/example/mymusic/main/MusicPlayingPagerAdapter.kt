package com.example.mymusic.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MusicPlayingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments : List<Fragment> = listOf(
        ArtworkWithWaveFormFragment(),
        PlaylistRecyclerFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

}
package com.example.mymusic.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.databinding.FragmentLyricsPageBinding

class LyricsPageFragment : Fragment() {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()

    private var _binding: FragmentLyricsPageBinding? = null
    private val binding get() = _binding!!

    private val lyricsTextView: TextView by lazy { binding.lyrics }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLyricsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val favorite = mainActivityViewModel.currentTrack.value
        favorite?.let {
            val lyrics = favorite.metadata.lyrics
            lyricsTextView.text = if (!lyrics.isNullOrEmpty()) "\n\n\n\n\n\n\n" + lyrics + "\n\n\n\n\n\n\n" else  "\n\n\n\n\n\n\n가사정보 없음"
        } ?: run { "정보 없음" }
    }
}
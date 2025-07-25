package com.example.mymusic.ui.musicInfo.subFragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.example.mymusic.databinding.FragmentLyricsPageBinding
import com.example.mymusic.ui.musicInfo.MusicInfoViewModel
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import kotlinx.coroutines.Dispatchers

class LyricsPageFragment : Fragment() {
    private val musicInfoViewModel: MusicInfoViewModel by activityViewModels()

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
        Log.d(TAG, "favorite" + musicInfoViewModel.favorite)
        musicInfoViewModel.favorite?.let { favorite ->

            Glide.with(requireContext())
                .load(favorite.track.artworkUrl)
                .centerCrop()
                .into(binding.backgroundImage)

            if (favorite.track.primaryColor != null) {
                setBackgroundColor(favorite.track.primaryColor)
            }
            else {
                ImageColorAnalyzer.analyzePrimaryColor(requireContext(), favorite.track.artworkUrl,
                    object : ImageColorAnalyzer.OnPrimaryColorAnalyzedListener {
                        override fun onSuccess(
                            dominantColor: Int,
                            primaryColor: Int,
                            selectedColor: Int,
                            unselectedColor: Int
                        ) {
                            setBackgroundColor(primaryColor)
                            favorite.track.primaryColor = primaryColor
                        }

                        override fun onFailure() {}

                    })
            }

            val lyrics = favorite.metadata.lyrics
            lyrics?.let { lyricsTextView.setText("\n\n\n\n\n\n\n" + lyrics + "\n\n\n\n\n\n\n") }
                ?: run { lyricsTextView.setText("가사정보 없음") }
        }
    }

    private fun setBackgroundColor(primaryColor: Int) {
        var darkenColor: Int
        if (context != null && DarkModeUtils.isDarkMode(context)) {
            darkenColor =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.3f
                )
        } else {
            darkenColor =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.9f
                )
        }
        binding.dimView.setBackgroundColor(MyColorUtils.adjustForWhiteText(darkenColor))
    }

    companion object {
        const val TAG = "LyricsPageFragment"
    }
}
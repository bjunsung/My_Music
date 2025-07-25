package com.example.mymusic.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.databinding.FragmentPlaylistRecyclerBinding

import com.example.mymusic.model.Favorite
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import com.example.mymusic.util.VerticalSpaceItemDecoration

class PlaylistRecyclerFragment : Fragment() {
    private var _binding: FragmentPlaylistRecyclerBinding? = null
    private val binding get() = _binding!!

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()
    private val playlistRecyclerView: RecyclerView by lazy { binding.playlistRecyclerView }
    private var playlist: List<Favorite>? = null

    private var playlistAdapter: PlaylistTrackAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistRecyclerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()


    }
    private fun bind() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val favoriteTrackColorUnificationState: Boolean = prefs.getBoolean("favorites_track_color_unification_state", false)
        playlistAdapter = PlaylistTrackAdapter(
            requireContext(),
            emptyList(),
            object :
                PlaylistTrackAdapter.OnItemClickListener {
                override fun onItemClick(
                    trackId: String?,
                    trackName: String?,
                    albumName: String?,
                    artistName: String?,
                    position: Int
                ) {
                    mainActivityViewModel.exoPlayer?.seekToDefaultPosition(position)
                }

            },
            favoriteTrackColorUnificationState
        )

        // dp 값을 px로 변환
        val spacingDp = -11
        val spacingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spacingDp.toFloat(),
            playlistRecyclerView.getResources().getDisplayMetrics()
        ).toInt()


        // 데코레이션 추가
        playlistRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spacingPx))

        playlistRecyclerView.adapter = playlistAdapter
        playlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        playlist = mainActivityViewModel.playlist.value
        playlistAdapter!!.updateData(playlist, mainActivityViewModel.currentIndex)

        mainActivityViewModel.playlist.observe(viewLifecycleOwner) { playlistSync ->
            playlist = playlistSync
            updateRecyclerView(playlistSync)
        }

        mainActivityViewModel.currentTrack.observe(viewLifecycleOwner){favoriteSync ->
            loadPrimaryColorAndUpdateUnificationColor()
            playlistAdapter!!.updateColors()
        }

        musicPlayingViewModel.currentPage.observe(viewLifecycleOwner) { page ->
            if (page == 0) {
                playlistRecyclerView.smoothScrollToPosition(mainActivityViewModel.currentIndex)
            }
        }
    }

    private fun updateRecyclerView(playlist: List<Favorite>) {
        playlistAdapter!!.updateData(playlist, mainActivityViewModel.currentIndex)
        loadPrimaryColorAndUpdateUnificationColor()
    }

    private fun loadPrimaryColorAndUpdateUnificationColor() {
        val currentTrack = mainActivityViewModel.currentTrack.value!!
        val primaryColor = currentTrack.track.primaryColor
        if (primaryColor != null) {
            updateUnificationColor(primaryColor)
        }
        else {
            ImageColorAnalyzer.analyzePrimaryColor(
                requireContext(),
                currentTrack.track.artworkUrl,
                object : ImageColorAnalyzer.OnPrimaryColorAnalyzedListener {
                    override fun onSuccess(
                        dominantColor: Int,
                        primaryColor: Int,
                        selectedColor: Int,
                        unselectedColor: Int
                    ) {
                        updateUnificationColor(primaryColor)
                    }

                    override fun onFailure() {
                        updateUnificationColor(Color.DKGRAY)
                    }
                })
        }

    }

    private fun updateUnificationColor(primaryColor: Int) {
        val darkenColor = MyColorUtils.darkenHslColor(primaryColor, 0.7f)
        playlistAdapter!!.setPrimaryBackgroundColor(darkenColor)
        val textColor = MyColorUtils.getSoftWhiteTextColor(darkenColor)
        playlistAdapter!!.setTextColor(textColor)

        playlistAdapter?.updateData(mainActivityViewModel.playlist.value, mainActivityViewModel.currentIndex)

        playlistRecyclerView.smoothScrollToPosition(mainActivityViewModel.currentIndex)
        Log.d(TAG, "smooth scroll to position: " + mainActivityViewModel.currentIndex)

    }

    companion object {
        const val TAG = "PlaylistRecyclerFragment"
    }

}


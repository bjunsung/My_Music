package com.example.mymusic.main.playlist

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.databinding.FragmentPlaylistRecyclerBinding
import com.example.mymusic.main.MusicPlayingViewModel

import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import com.example.mymusic.model.SessionKind
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import com.example.mymusic.util.VerticalSpaceItemDecoration

@UnstableApi
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

    override fun onResume() {
        super.onResume()
        val displayMetrics: DisplayMetrics = requireContext().resources.displayMetrics
        val orientation = requireContext().resources.configuration.orientation
        val screenWidth = displayMetrics.widthPixels      // px 단위 가로
        val screenHeight = displayMetrics.heightPixels    // px 단위 세로
        playlistRecyclerView.layoutParams = playlistRecyclerView.layoutParams.apply {
            width = if (orientation == Configuration.ORIENTATION_LANDSCAPE) (screenWidth * 0.492).toInt() else (screenWidth * 0.985).toInt()
            height = if (orientation == Configuration.ORIENTATION_LANDSCAPE) (screenHeight * 0.8).toInt() else (screenHeight * 0.875).toInt()
        }
        playlistRecyclerView.requestLayout()
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
                    mainActivityViewModel.mediaController?.seekToDefaultPosition(position)
                }

            },
            favoriteTrackColorUnificationState
        )

        // dp 값을 px로 변환
        val spacingDp = -11
        val spacingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spacingDp.toFloat(),
            playlistRecyclerView.resources.displayMetrics
        ).toInt()

        // 데코레이션 추가
        playlistRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spacingPx))

        playlistRecyclerView.adapter = playlistAdapter
        playlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        playlist = mainActivityViewModel.nowPlayingList.value
        playlistAdapter!!.updateData(playlist, mainActivityViewModel.currentIndex, 0)

        mainActivityViewModel.nowPlayingList.observe(viewLifecycleOwner) { playlistSync ->
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


        mainActivityViewModel.nowPlayingList.observe(viewLifecycleOwner) {
            val sessionKind = mainActivityViewModel.activeKind.value ?: SessionKind.AD_HOC
            when (sessionKind) {
                SessionKind.SAVED -> {
                    binding.playlistName.visibility = View.VISIBLE
                    binding.dot.visibility = View.VISIBLE
                    val currentPlaylist: Playlist? = mainActivityViewModel.getLastSelectedPlaylist()
                    currentPlaylist?.let {
                        binding.playlistName.text = it.playlistName
                        binding.playlistCount.text = "${it.trackIds.size}곡"
                        binding.playlistTotalDuration.text = it.getDurationStr()
                    }
                }
                else -> {
                    binding.playlistName.visibility = View.GONE
                    binding.dot.visibility = View.GONE

                    val playlist = mainActivityViewModel.nowPlayingList.value
                    playlist?.let {
                        binding.playlistCount.text = "${it.size}곡"
                        val durationSec = it.map { it.duration }.sum() / 1000
                        val h = durationSec / 3600
                        val m = durationSec / 60 % 60
                        val s = durationSec % 60

                        binding.playlistTotalDuration.text = when {
                            h >= 24 -> "${h / 24}일 ${h % 24}시간"
                            h > 0 && m == 0 && s == 0 -> "${h}시간"
                            h > 0 && s == 0 -> "${h}시간 ${m}분"
                            h > 0 -> "${h}시간 ${m}분 ${s}초"
                            m > 0 && s == 0 -> "${m}분"
                            m > 0 -> "${m}분 ${s}초"
                            else -> "${s}초"
                        }

                    }
                }
            }
        }


        mainActivityViewModel.activeKind.observe(viewLifecycleOwner) { type ->

        }
    }

    private fun updateRecyclerView(playlist: List<Favorite>) {
       // playlistAdapter!!.updateData(playlist, mainActivityViewModel.currentIndex)
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

        playlistAdapter?.updateData(mainActivityViewModel.nowPlayingList.value, mainActivityViewModel.currentIndex, primaryColor)

        Log.d(TAG, "smooth scroll to position: " + mainActivityViewModel.currentIndex)

    }

    companion object {
        const val TAG = "PlaylistRecyclerFragment"
    }

}


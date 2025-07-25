package com.example.mymusic.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.adapter.FavoritesWithCardViewAdapter
import com.example.mymusic.databinding.FragmentPlaylistRecyclerBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.util.MyColorUtils

class PlaylistRecyclerFragment : Fragment() {
    private var _binding: FragmentPlaylistRecyclerBinding? = null
    private val binding get() = _binding!!

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()

    private val playlistRecyclerView: RecyclerView by lazy { binding.playlistRecyclerView }
    private var playlist: List<Favorite>? = null

    private var playlistAdapter: FavoritesWithCardViewAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
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
        playlistAdapter = FavoritesWithCardViewAdapter(
            requireContext(),
            emptyList(),
            object: FavoritesWithCardViewAdapter.OnItemClickListener {
                override fun onItemClick(
                    trackId: String?,
                    trackName: String?,
                    albumName: String?,
                    artistName: String?,
                    position: Int
                ) {
                    //TODO("Not yet implemented")
                }

            },
            favoriteTrackColorUnificationState
        )

        playlistRecyclerView.adapter = playlistAdapter
        playlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        playlist = mainActivityViewModel.playlist.value
        playlistAdapter!!.updateData(playlist)

        mainActivityViewModel.playlist.observe(viewLifecycleOwner) { playlistSync ->
            playlist = playlistSync
            updateRecyclerView(playlistSync)
        }

        mainActivityViewModel.currentTrack.observe(viewLifecycleOwner){favoriteSync ->
            playlistRecyclerView.scrollToPosition(mainActivityViewModel.currentIndex)
        }
    }

    private fun updateRecyclerView(playlist: List<Favorite>) {
        playlistAdapter!!.updateData(playlist)
        val primaryColor = mainActivityViewModel.currentTrack.value?.track?.primaryColor ?: Color.DKGRAY
        playlistAdapter!!.setPrimaryBackgroundColor(primaryColor)
        val textColor = MyColorUtils.getSoftWhiteTextColor(primaryColor)
        playlistAdapter!!.setTextColor(textColor)
        playlistRecyclerView.scrollToPosition(mainActivityViewModel.currentIndex)
    }

}


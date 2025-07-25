package com.example.mymusic.ui.musicInfo.subFragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentPlaylistInMusicInfoBinding
import com.example.mymusic.model.Playlist
import com.example.mymusic.ui.musicInfo.MusicInfoViewModel
import com.example.mymusic.ui.playlist.playlistDetail.PlaylistDetailFragment
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class ContainingPlaylistFragment: Fragment() {

    var _binding : FragmentPlaylistInMusicInfoBinding? = null
    val binding get() = _binding!!

    val vmMainActivity: MainActivityViewModel by activityViewModels()
    val vmMusicInfo: MusicInfoViewModel by activityViewModels()

    val adapter : ContainingPlaylistInMusicInfoAdapter by lazy {
        ContainingPlaylistInMusicInfoAdapter(
            emptyList(),
            object : ContainingPlaylistInMusicInfoAdapter.OnClickListener {
                override fun onItemClick(playlist: Playlist) {
                    val args = Bundle().apply {
                        putParcelable(PlaylistDetailFragment.ARGUMENT_KEY, playlist)
                    }
                    findNavController().navigate(R.id.fragment_playlist_detail, args)
                }

                override fun onPlayButtonClick(playlist: Playlist) {
                    if (playlist.trackIds.isNotEmpty()) {
                        vmMainActivity.setPlaylistFromPlaylist(playlist, 0)
                    }
                }

                override fun onShuffleButtonClick(playlist: Playlist) {
                    if (playlist.trackIds.isNotEmpty()) {
                        val shuffled = playlist.deepCopy().apply { favorites = favorites.shuffled() }
                        vmMainActivity.setPlaylistFromPlaylist(shuffled, 0)
                    }
                }

            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistInMusicInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
        setObserver()
    }

    private fun bind() {
        binding.playlistRecyclerView.adapter = adapter
        binding.playlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        vmMusicInfo.favorite?.let { favorite ->
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

            vmMainActivity.viewModelScope.launch(Dispatchers.IO) {
                if (vmMusicInfo.allPlaylists == null) {
                    vmMusicInfo.allPlaylists =  vmMusicInfo.playlistRepository.getAllWithFavorites()
                }
                vmMusicInfo.allPlaylists?.filter { it.trackIds.contains(favorite.track.trackId) }?.let {
                    vmMusicInfo.containingPlaylists.postValue(it)
                }
            }
        }

    }
    private fun setObserver() {
        vmMusicInfo.containingPlaylists.observe(viewLifecycleOwner) { playlist ->
            Log.d("ContainingDebug", "containing playlists: " + playlist.toString())
            adapter.updateList(playlist)
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

}
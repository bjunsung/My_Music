package com.example.mymusic.main.playlist

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentPlaylistRecyclerBinding
import com.example.mymusic.main.MusicPlayingViewModel

import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import com.example.mymusic.model.SessionKind
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.ui.playlist.playlistDetail.ContainingPlaylistAdapter
import com.example.mymusic.ui.playlist.playlistDetail.PlaylistDetailViewModel
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import com.example.mymusic.util.VerticalSpaceItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class PlaylistRecyclerFragment : Fragment() {
    private var _binding: FragmentPlaylistRecyclerBinding? = null
    private val binding get() = _binding!!

    private val vmMainActivity: MainActivityViewModel by activityViewModels()
    private val vmMusicPlaying: MusicPlayingViewModel by activityViewModels()
    private val playlistRecyclerView: RecyclerView by lazy { binding.playlistRecyclerView }
    private var playlist: List<Favorite>? = null

    private var playlistAdapter: PlaylistTrackAdapter? = null
    private val dragHandleCallback: DragReorderCallback by lazy(LazyThreadSafetyMode.NONE) {
        val adapter = requireNotNull(playlistAdapter) { "playlistAdapter must be set before dragHandleCallback" }
        DragReorderCallback(
            onMoveInAdapter = adapter::onItemMove,
            readCurrentIds  = { adapter.currentOrder().map { it.track.trackId } },
            onCommit        = { ids ->
                vmMainActivity.viewModelScope.launch(Dispatchers.IO) {
                    val reordered = vmMusicPlaying.loadFavoritesByIds(ids)
                    withContext(Dispatchers.Main) { vmMainActivity.applyNewQueuePreservingCurrent(reordered) }
                }
            })
    }

    private val itemTouchHelper: ItemTouchHelper by lazy { ItemTouchHelper(dragHandleCallback) }


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
            mutableListOf(),
            object :
                PlaylistTrackAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) { vmMainActivity.mediaController?.seekToDefaultPosition(position) }
                override fun onMoreButtonClick(holder: PlaylistTrackAdapter.FavoritesWithCardViewHolder, favorite: Favorite) {
                    val anchorView = holder.itemView
                    val popupView = layoutInflater.inflate(R.layout.popup_menu_for_playlist_track_item, null)
                    val popupWidth = 420
                    val popupWindow = PopupWindow(
                        popupView,
                        popupWidth,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                    ).apply {
                        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        isOutsideTouchable = true
                        isFocusable = false
                        elevation = 16f
                    }

                    anchorView.post {
                        val anchorLocation = IntArray(2)
                        anchorView.getLocationOnScreen(anchorLocation)
                        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                        val popupViewHeight = popupView.measuredHeight
                        val anchorX = anchorLocation[0]
                        val anchorY = anchorLocation[1]
                        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                        val isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        var x = if (isLandScape) screenWidth/2 else (screenWidth - anchorX - 1.5 * popupWidth).toInt()
                        var y = anchorY
                        popupWindow.showAtLocation(
                            anchorView.rootView,
                            Gravity.NO_GRAVITY,
                            x,
                            y
                        )
                    }
                    val removeTrack = popupView.findViewById<LinearLayout>(R.id.remove_from_playlist_layout)
                    if (favorite.track.trackId == vmMainActivity.currentTrack.value?.track?.trackId) {
                        removeTrack.visibility = View.GONE
                        popupView.findViewById<View>(R.id.separate_line).visibility = View.GONE
                    }
                    else {
                        popupView.findViewById<View>(R.id.separate_line).visibility = View.VISIBLE
                    }
                    removeTrack.setOnClickListener {
                        popupWindow.dismiss()
                        playlistAdapter?.currentOrder()?.let { before ->
                            val removed = before.filterNot { it.track.trackId == favorite.track.trackId }
                            vmMainActivity.applyNewQueuePreservingCurrent(removed)
                        }
                    }
                    val shuffleWhenMusicEnd = popupView.findViewById<LinearLayout>(R.id.shuffle_when_music_end_layout)
                    shuffleWhenMusicEnd.setOnClickListener {
                        popupWindow.dismiss()

                        val others: List<Favorite> =
                            playlist.orEmpty().filter { it.track.trackId != favorite.track.trackId }

                        val shuffled: List<Favorite> =
                            listOf(favorite) + others.shuffled()  // ✅ 평탄화된 Favorite 리스트

                        vmMainActivity.applyNewQueuePreservingCurrent(shuffled)
                    }

                    val showContainingPlaylists = popupView.findViewById<LinearLayout>(R.id.show_playlists_for_track_layout)

                    showContainingPlaylists.setOnClickListener {
                        popupWindow.dismiss()
                        val dialog = Dialog(requireContext()).apply {
                            window?.setBackgroundDrawableResource(android.R.color.transparent)
                            setContentView(R.layout.dialog_custom_playlists)
                            setCancelable(true)
                        }
                        val focusedImageView = dialog.findViewById<ImageView>(R.id.artwork_image)
                        val containingPlaylistRecyclerView = dialog.findViewById<RecyclerView>(R.id.playlist_recycler_view)
                        val dismissButton = dialog.findViewById<TextView>(R.id.dismiss_button)
                        Glide.with(requireContext())
                            .load(favorite.track.artworkUrl)
                            .error(R.drawable.ic_image_not_found_foreground)
                            .override(160, 160)
                            .centerCrop()
                            .into(focusedImageView)
                        val containingPlaylistAdapter =  ContainingPlaylistAdapter(
                            emptyList(),
                            object : ContainingPlaylistAdapter.OnClickListener {
                                override fun onItemClick(playlist: Playlist) {}

                                override fun onPlayButtonClick(playlist: Playlist) {
                                    if (playlist.trackIds.isEmpty()) return
                                    vmMainActivity.setPlaylistFromPlaylist(playlist, 0)
                                }

                                override fun onShuffleButtonClick(playlist: Playlist) {
                                    if (playlist.trackIds.isEmpty()) return
                                    vmMainActivity.setPlaylistFromPlaylist(playlist.deepCopy().apply { favorites = favorites.shuffled() }, 0)
                                }
                            }
                        )
                        containingPlaylistRecyclerView.adapter = containingPlaylistAdapter
                        containingPlaylistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                        vmMusicPlaying.viewModelScope.launch(Dispatchers.IO) {
                            val containingPlaylists = vmMusicPlaying.findContainingPlaylist(favorite.track.trackId)
                            withContext(Dispatchers.Main) { containingPlaylistAdapter.updateList(containingPlaylists) }
                        }
                        dismissButton.setOnClickListener { dialog.dismiss() }
                        dialog.setOnDismissListener { containingPlaylistRecyclerView.adapter = null }
                        dialog.show()
                    }

                    val seeMusicInfo = popupView.findViewById<LinearLayout>(R.id.see_music_info)
                    seeMusicInfo.setOnClickListener {
                        val args = Bundle().apply {
                            putParcelable(MusicInfoFragment.ARGUMENTS_KEY, favorite)
                            putBoolean(MusicInfoFragment.REQUEST_BOTTOM_SHEET, true)
                        }

                        findNavController().navigate(R.id.musicInfoFragment, args)
                        vmMusicPlaying.requestDismiss(true)

                    }

                    vmMainActivity.currentTrack.observe(viewLifecycleOwner) { changed ->
                        popupWindow.dismiss()
                    }
                }


                override fun onDragHandleTouch(holder: RecyclerView.ViewHolder) {
                    val rv = holder.itemView.parent as? RecyclerView ?: return
                    if (!rv.isAttachedToWindow) return
                    itemTouchHelper.startDrag(holder)
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
        /*
        val animator = playlistRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator)
            animator.supportsChangeAnimations = false
        playlistRecyclerView.itemAnimator = animator


         */
        playlistRecyclerView.adapter = playlistAdapter
        playlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        itemTouchHelper.attachToRecyclerView(playlistRecyclerView)
        playlist = vmMainActivity.nowPlayingList.value
        playlistAdapter!!.updateData(playlist?.toMutableList() ?: mutableListOf(), vmMainActivity.currentTrack.value?.track?.trackId ?: "track_not_found", 0)

        vmMainActivity.nowPlayingList.observe(viewLifecycleOwner) { playlistSync ->
            playlist = playlistSync
            updateRecyclerView(playlistSync)
        }

        vmMainActivity.currentTrack.observe(viewLifecycleOwner){ favoriteSync ->
            loadPrimaryColorAndUpdateUnificationColor()
            playlistAdapter!!.updateColors()
        }

        vmMusicPlaying.currentPage.observe(viewLifecycleOwner) { page ->
            if (page == 0) {
                playlistRecyclerView.smoothScrollToPosition(vmMainActivity.currentIndex)
            }
        }


        vmMainActivity.nowPlayingList.observe(viewLifecycleOwner) {
            val sessionKind = vmMainActivity.activeKind.value ?: SessionKind.AD_HOC
            when (sessionKind) {
                SessionKind.SAVED -> {
                    binding.playlistName.visibility = View.VISIBLE
                    binding.dot.visibility = View.VISIBLE
                    val currentPlaylist: Playlist? = vmMainActivity.getLastSelectedPlaylist()
                    currentPlaylist?.let {
                        binding.playlistName.text = it.playlistName
                        binding.playlistCount.text = "${it.trackIds.size}곡"
                        binding.playlistTotalDuration.text = it.getDurationStr()
                    }
                }
                else -> {
                    binding.playlistName.visibility = View.GONE
                    binding.dot.visibility = View.GONE

                    val playlist = vmMainActivity.nowPlayingList.value
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


        vmMainActivity.activeKind.observe(viewLifecycleOwner) { type ->

        }
    }

    private fun updateRecyclerView(playlist: List<Favorite>) {
       // playlistAdapter!!.updateData(playlist, mainActivityViewModel.currentIndex)
        loadPrimaryColorAndUpdateUnificationColor()
    }

    private fun loadPrimaryColorAndUpdateUnificationColor() {
        val currentTrack = vmMainActivity.currentTrack.value!!
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playlistRecyclerView.adapter = null
        _binding = null
    }

    private fun updateUnificationColor(primaryColor: Int) {
        val darkenColor = MyColorUtils.darkenHslColor(primaryColor, 0.7f)
        playlistAdapter!!.setPrimaryBackgroundColor(darkenColor)
        val textColor = MyColorUtils.getSoftWhiteTextColor(darkenColor)
        playlistAdapter!!.setTextColor(textColor)

        playlistAdapter?.updateData(vmMainActivity.nowPlayingList.value?.toMutableList()?: mutableListOf(), vmMainActivity.currentTrack.value?.track?.trackId ?: "track_not_found", primaryColor)

        Log.d(TAG, "smooth scroll to position: " + vmMainActivity.currentIndex)

    }

    companion object {
        const val TAG = "PlaylistRecyclerFragment"
    }

}


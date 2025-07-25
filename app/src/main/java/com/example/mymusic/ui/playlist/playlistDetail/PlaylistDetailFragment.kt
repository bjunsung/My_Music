package com.example.mymusic.ui.playlist.playlistDetail

import android.app.Dialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.transition.ArcMotion
import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet
import androidx.transition.Slide
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.databinding.FragmentPlaylistDetailBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.ui.playlist.searchPlaylist.SearchPlaylistFragment
import com.example.mymusic.util.ImageCollageUtil
import com.example.mymusic.util.VerticalSpaceItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class PlaylistDetailFragment: Fragment() {
    val vmMainActivity: MainActivityViewModel by activityViewModels()
    val vmPlaylistDetail: PlaylistDetailViewModel by viewModels()

    private var _binding: FragmentPlaylistDetailBinding? = null
    val binding get() = _binding!!

    val callback :DragReorderCallback by lazy { DragReorderCallback(adapter, vmPlaylistDetail) }
    val itemTouchHelper: ItemTouchHelper by lazy { ItemTouchHelper(callback) }

    private var combinedImageReadyFlag = false
    //private var focusedImageReadyFlag = false

    val adapter: PlaylistDetailAdapter by lazy {
        PlaylistDetailAdapter(
            mutableListOf(),
            mutableSetOf(),
            object: PlaylistDetailAdapter.OnClickListener {
                override fun onItemClick(
                    holder: PlaylistDetailAdapter.ViewHolder,
                    favorite: Favorite,
                ) {
                    val args = Bundle().apply {
                        putParcelable(MusicInfoFragment.ARGUMENTS_KEY, favorite)
                        putString(
                            MusicInfoFragment.TRANSITION_NAME_KEY,
                            holder.artworkImage.transitionName
                        )
                    }
                    val extras = FragmentNavigatorExtras(
                        holder.artworkImage to holder.artworkImage.transitionName,
                        holder.titleTextView to holder.titleTextView.transitionName,
                        holder.artistNameTextView to holder.artistNameTextView.transitionName,
                        holder.albumNameTextView to holder.albumNameTextView.transitionName,
                        holder.durationLayout to holder.durationLayout.transitionName,
                        holder.releaseDateLayout to holder.releaseDateLayout.transitionName
                    )
                    findNavController().navigate(R.id.musicInfoFragment, args, null, extras)
                    binding.trackRecyclerView.post { vmPlaylistDetail.focusedTrackId = favorite.track.trackId
                    }
                }

                override fun onPlayButtonClick(trackList: List<Favorite>, position: Int) {
                    val playlist = vmPlaylistDetail.playlist.value
                    playlist?.let {
                        val copied = it.deepCopy()
                        copied.favorites = copied.favorites.subList(
                            position,
                            copied.numberOfTracks()
                        ) + copied.favorites.subList(0, position)
                        vmMainActivity.setPlaylistFromPlaylist(copied, 0)
                    }

                }

                override fun onDragHandleButtonClick(viewHolder: RecyclerView.ViewHolder) {
                    val rv = viewHolder.itemView.parent as? RecyclerView ?: return
                    if (!rv.isAttachedToWindow) return
                    itemTouchHelper.startDrag(viewHolder)
                }


                override fun onItemSelected(selectedIds: Set<String>) {
                    vmPlaylistDetail.updateSelectedIdSet(selectedIds)
                }

                override fun onImageReady(trackId: String) {


                    val focusedTrackId = vmPlaylistDetail.focusedTrackId

                    if (focusedTrackId == trackId) {
                        Log.d(TAG, "image ready for id: " + trackId)
                    }
                    /*
                   if (focusedTrackId == null || focusedTrackId == trackId) {

                       focusedImageReadyFlag = true
                       if (focusedImageReadyFlag && combinedImageReadyFlag) {
                           startPostponedEnterTransition()
                       }
                   }

                     */





                }

                override fun onMoreButtonClick(anchorView: View, favorite: Favorite) {
                    val playlist = vmPlaylistDetail.playlist.value
                    playlist?.let { playlist ->
                        val anchorId = favorite.track.trackId
                        var targetIds = setOf<String>()
                        if (anchorId in (vmPlaylistDetail.selectedIdsSet.value ?: emptySet()))
                            targetIds = vmPlaylistDetail.selectedIdsSet.value ?: emptySet()
                        else
                            targetIds = setOf(anchorId)

                        val popupView =
                            layoutInflater.inflate(R.layout.popup_menu_for_playlist_item, null)
                        val popupWindowWidth = 450
                        val popupWindow = PopupWindow(
                            popupView,
                            popupWindowWidth,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                        ).apply {
                            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            isOutsideTouchable = true
                            isFocusable = false
                            elevation = 16f
                        }

                        val addToTemporaryPlaylist =
                            popupView.findViewById<LinearLayout>(R.id.add_to_temporary_playlist_layout)
                        val musicShuffleLayout =
                            popupView.findViewById<LinearLayout>(R.id.music_shuffle_here_layout)
                        val createNewPlaylistLayout =
                            popupView.findViewById<LinearLayout>(R.id.create_new_playlist)
                        val showContainingPlaylistLayout =
                            popupView.findViewById<LinearLayout>(R.id.show_playlists_for_track)
                        val removeTrackLayout =
                            popupView.findViewById<LinearLayout>(R.id.remove_from_playlist_layout)
                        val removeTrackTextView =
                            popupView.findViewById<TextView>(R.id.remove_from_playlist_text)
                        removeTrackTextView.text = "[${playlist.playlistName}] 에서 제거"

                        val isNonEditable = vmPlaylistDetail.isNonEditable
                        if (isNonEditable) {
                            removeTrackLayout.visibility = View.GONE
                        }

                        anchorView.post {
                            val anchorLocation = IntArray(2)
                            anchorView.getLocationOnScreen(anchorLocation)
                            popupView.measure(
                                View.MeasureSpec.UNSPECIFIED,
                                View.MeasureSpec.UNSPECIFIED
                            )
                            val popupViewHeight = popupView.measuredHeight
                            val anchorX = anchorLocation[0]
                            val anchorY = anchorLocation[1]
                            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                            var x = screenWidth - 7 * anchorX - popupWindowWidth
                            var y =  anchorY
                            popupWindow.showAtLocation(
                                anchorView.rootView,
                                Gravity.NO_GRAVITY,
                                x,
                                y
                            )
                        }

                        removeTrackLayout.setOnClickListener {
                            val dialog = Dialog(requireContext()).apply {
                                setContentView(R.layout.dialog_custom)
                                window?.setBackgroundDrawableResource(android.R.color.transparent)
                                setCanceledOnTouchOutside(true)
                                setCancelable(true)
                            }
                            val title = dialog.findViewById<TextView>(R.id.title)
                            val subText = dialog.findViewById<TextView>(R.id.subtext)
                            val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                            val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)
                            title.text = "Track 제거"
                            confirmButton.text = "제거"
                            if (targetIds.size == 1) {
                                subText.text =
                                    "정말 ${favorite.title} 을 [${playlist.playlistName}] 에서 제거하시겠습니까?"
                            }
                            else{
                                subText.text =
                                    "정말 ${favorite.title} 외 ${targetIds.size-1}곡을 [${playlist.playlistName}] 에서 제거하시겠습니까?"
                            }
                            cancelButton.setOnClickListener { dialog.dismiss() }
                            confirmButton.setOnClickListener {
                                popupWindow.dismiss()
                                dialog.dismiss()
                                vmPlaylistDetail.removeTracks(
                                    playlist.playlistId,
                                    targetIds
                                )
                            }
                            dialog.show()
                        }

                        addToTemporaryPlaylist.setOnClickListener {
                            popupWindow.dismiss()
                            vmPlaylistDetail.viewModelScope.launch(Dispatchers.IO) {
                                val favorites = vmMainActivity.favoriteSongRepository.getFavoritesByIds(targetIds.toList())
                                withContext(Dispatchers.Main) {
                                    vmMainActivity.addPlaylist(favorites)
                                }
                            }
                        }

                        musicShuffleLayout.setOnClickListener {
                            popupWindow.dismiss()

                            val shuffled = playlist.favorites.shuffled().toMutableList()
                            val index = shuffled.indexOfFirst { it.track.trackId == favorite.track.trackId }
                            if (index > 0) {
                                val target = shuffled.removeAt(index) // 꺼내고
                                shuffled.add(0, target)          // 맨 앞에 삽입
                            }
                            vmMainActivity.setPlaylistFromPlaylistWithShuffle(
                                playlist
                                    .deepCopy()
                                    .apply { favorites = shuffled }
                            )

                        }
                        showContainingPlaylistLayout.setOnClickListener {
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
                                    override fun onItemClick(playlist: Playlist) {
                                        dialog.dismiss()
                                        vmPlaylistDetail._playlist.value = playlist
                                    }

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
                            vmPlaylistDetail.viewModelScope.launch(Dispatchers.IO) {
                                val containingPlaylists = vmPlaylistDetail.findContainingPlaylist(favorite.track.trackId)
                                withContext(Dispatchers.Main) { containingPlaylistAdapter.updateList(containingPlaylists) }
                            }

                            dismissButton.setOnClickListener {
                                dialog.dismiss()
                            }
                            dialog.show()
                            dialog.setOnDismissListener { containingPlaylistRecyclerView.adapter = null }
                        }
                    }
                }



            }
        )
    }

    val arc = ArcMotion().apply {
        minimumHorizontalAngle = 60f // 좌우 곡률 최소 각도
        minimumVerticalAngle = 80f   // 상하 곡률 최소 각도
        maximumAngle = 90f           // 전체 곡률 최대 각도
    }
    fun buildTextTransition(targetName: String): TransitionSet {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform()) // 스케일/회전 변환 추가
            setPathMotion(arc)
            duration = 435L
            interpolator = AccelerateDecelerateInterpolator()
            addTarget(targetName) // transitionName
        }
    }
    fun buildImageTransition(targetName: String): TransitionSet {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            setPathMotion(ArcMotion())
            duration = 350L
            interpolator = AccelerateDecelerateInterpolator()
            addTarget(targetName)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playlist = requireArguments()
            .getParcelable<Playlist>(ARGUMENT_KEY)!!
        val playlistId = playlist.playlistId


        val nameTrans = buildTextTransition("name_$playlistId")
        val countTrans = buildTextTransition("count_$playlistId")
        val durationTrans = buildTextTransition("duration_$playlistId")
        val imageTrans = buildImageTransition( "combined_artworks_$playlistId")

        sharedElementEnterTransition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(imageTrans)
            addTransition(nameTrans)
            addTransition(countTrans)
            addTransition(durationTrans)
        }

        sharedElementReturnTransition = (sharedElementEnterTransition as TransitionSet).clone()

        // ✅ 2. 나머지 뷰를 위한 전환 설정
        enterTransition = Slide(Gravity.BOTTOM).setDuration(275L)
        returnTransition = null

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()


        vmPlaylistDetail.viewModelScope.launch {
            delay(200)
            startPostponedEnterTransition()
        }


        receiveBundle()
        bind()
        setObserver()
        setClickEvent()
    }

    private fun receiveBundle() {
        val receivedPlaylist = arguments?.getParcelable<Playlist>(ARGUMENT_KEY)
        if (vmPlaylistDetail.playlist.value == null) {
            vmPlaylistDetail._playlist.value = receivedPlaylist
        }
        vmPlaylistDetail.synchronizePlaylist()
    }

    private fun bind() {
        if ((vmPlaylistDetail.playlist.value?.playlistId
                ?: "undefined") == PlaylistRepository.PLAYLIST_ID_RECENTLY_PLAYED
        ) {
            binding.editLocationFavorites.visibility = View.GONE
        } else binding.editLocationFavorites.visibility = View.VISIBLE
        binding.trackRecyclerView.adapter = adapter
        binding.trackRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        itemTouchHelper.attachToRecyclerView(binding.trackRecyclerView)
        val spacingDp = -11
        val spacingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spacingDp.toFloat(),
            binding.trackRecyclerView.resources.displayMetrics
        ).toInt()

        binding.trackRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(spacingPx))
        updatePlaylistInformation()
    }



    private fun updatePlaylistInformation() {
        val playlist = vmPlaylistDetail.playlist.value
        playlist?.let { setTransitionName(it.playlistId) }

        binding.playlistName.text = playlist?.playlistName ?: "플레이리스트 정보 없음"
        binding.playlistTotalDuration.text = playlist?.getDurationStr() ?: "0분"
        binding.playlistCount.text = "${playlist?.trackIds?.size ?: "0"}곡"

        vmPlaylistDetail.viewModelScope.launch(Dispatchers.IO) {
            val urls = playlist?.favorites?.map { it.track.artworkUrl }?.take(4) ?: emptyList()
            val bitmap = ImageCollageUtil.make2x2(
                context = requireContext(),
                urls = urls,
                size = 480,
                R.drawable.ic_round_playlist_play
            )
            withContext(Dispatchers.Main) {
                binding.combinedArtworks.setImageBitmap(bitmap)
                startPostponedEnterTransition()
                /*
                combinedImageReadyFlag = true
                if (focusedImageReadyFlag && combinedImageReadyFlag) {
                    startPostponedEnterTransition()
                }

                 */
            }
        }
    }

    private fun setTransitionName(playlistId: String) {
        // 2) transitionName을 '출발 프래그먼트'에서 준 값과 동일하게 세팅
        binding.combinedArtworks.transitionName = "combined_artworks_$playlistId"
        binding.combinedArtworksCard.transitionName = "artworks_$playlistId"
        binding.playlistName.transitionName = "name_$playlistId"
        binding.playlistCount.transitionName = "count_$playlistId"
        binding.playlistTotalDuration.transitionName = "duration_$playlistId"
    }

    private fun setObserver() {
        vmPlaylistDetail.playlist.observe(viewLifecycleOwner) { playlist ->
            adapter.updateList(
                playlist.favorites.toList(),
                vmPlaylistDetail.selectedIdsSet.value?.toSet() ?: emptySet()
            )
            updatePlaylistInformation()
        }

        vmPlaylistDetail.selectedIdsSet.observe(viewLifecycleOwner) {newSet ->
            adapter.updateSelectedIdSet(newSet)
        }

        /** 최근 재생한 플레이리스트 면 mainActivityViewModel 의 플레이리스트에 observe */
        if (vmPlaylistDetail.playlist.value?.playlistId == PlaylistRepository.PLAYLIST_ID_RECENTLY_PLAYED) {
            vmMainActivity.currentlyPlayedPlaylist.observe(viewLifecycleOwner) { recentlyPlayedPlaylist ->
                vmPlaylistDetail._playlist.value = recentlyPlayedPlaylist
            }
        }
    }

    private fun setClickEvent() {
       val playlist = vmPlaylistDetail.playlist.value
        if (playlist?.isEmpty() != false) {
            binding.musicPlay.alpha = 0.4f
            binding.musicShuffle.alpha = 0.4f
            binding.musicPlay.setOnClickListener(null)
            binding.musicShuffle.setOnClickListener(null)
        } else {
            binding.musicPlay.setOnClickListener {
                vmMainActivity.setPlaylistFromPlaylistInOrder(playlist, 0)
            }
            binding.musicShuffle.setOnClickListener {
                vmMainActivity.setPlaylistFromPlaylistWithShuffle(
                    playlist
                        .deepCopy()
                        .apply { shuffle() }
                )
            }
        }
        if (vmPlaylistDetail.playlist.value?.playlistId?.equals(PlaylistRepository.PLAYLIST_ID_RECENTLY_PLAYED) ?: false) {
            binding.rename.visibility = View.GONE
            binding.addTracks.visibility = View.GONE
        }
        else {
            binding.rename.setOnClickListener {
                vmPlaylistDetail.playlist.value?.let { playlist ->

                    vmPlaylistDetail.viewModelScope.launch(Dispatchers.IO) {
                        val alreadyExistNameSet =
                            vmPlaylistDetail.playlistRepository.getAllPlaylistNameSet()
                        withContext(Dispatchers.Main) {
                            val dialog = Dialog(requireContext()).apply {
                                setContentView(R.layout.dialog_custom_edittext_with_notice)
                                window?.setBackgroundDrawableResource(android.R.color.transparent)
                                setCancelable(true)
                            }
                            val titleTextView = dialog.findViewById<TextView>(R.id.title)
                            val editText = dialog.findViewById<EditText>(R.id.edit_text)

                            val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                            val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)
                            confirmButton.alpha = 0.5f
                            titleTextView.text = "이름 변경"
                            editText.setText("")
                            editText.hint = "변경할 제목"
                            editText.setSingleLine(true)
                            val nameDuplicationNoticeTextView =
                                dialog.findViewById<TextView>(R.id.name_duplication_notice)
                            nameDuplicationNoticeTextView.visibility = View.INVISIBLE

                            editText.addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    s: CharSequence?,
                                    start: Int,
                                    count: Int,
                                    after: Int
                                ) {
                                }

                                override fun onTextChanged(
                                    s: CharSequence?,
                                    start: Int,
                                    before: Int,
                                    count: Int
                                ) {
                                    val trimmed = s?.trim().toString().replace("\n", " ")

                                    if (s.isNullOrEmpty()) {
                                        nameDuplicationNoticeTextView.visibility = View.INVISIBLE
                                        confirmButton.alpha = 0.5f
                                        confirmButton.setOnClickListener(null)
                                    } else if (trimmed in alreadyExistNameSet) {
                                        confirmButton.alpha = 0.5f
                                        confirmButton.setOnClickListener(null)
                                        nameDuplicationNoticeTextView.visibility = View.VISIBLE
                                        if (trimmed.equals("최근 재생한 음악"))
                                            nameDuplicationNoticeTextView.text = "사용할 수 없는 이름입니다"
                                        else if (trimmed.equals(playlist.playlistName))
                                            nameDuplicationNoticeTextView.text =
                                                "같은 이름으로 변경할 수 없습니다"
                                        else
                                            nameDuplicationNoticeTextView.text = "이미 사용중인 이름입니다"
                                    } else {
                                        nameDuplicationNoticeTextView.visibility = View.INVISIBLE
                                        confirmButton.alpha = 1f
                                        confirmButton.setOnClickListener {
                                            val newListName = trimmed
                                            vmPlaylistDetail.renamePlaylist(
                                                playlist = playlist,
                                                newName = newListName
                                            )
                                            dialog.dismiss()
                                        }
                                    }
                                }

                                override fun afterTextChanged(s: Editable?) {}

                            })
                            cancelButton.setOnClickListener { dialog.dismiss() }

                            dialog.show()

                        }
                    }
                }
            }
        }

        binding.addTracks.setOnClickListener {
            val pli = vmPlaylistDetail.playlist.value
            pli?.let {
                val args = Bundle().apply {
                    putParcelable(SearchPlaylistFragment.PLAYLIST_KEY, pli)
                }
                findNavController().navigate(R.id.fragment_search_playlist, args)
            }
        }

        binding.selectFavorites.setOnClickListener {
            if (!adapter.isSelectionMode) {
                adapter.changeSelectionMode(true)
                adapter.updateList(
                    vmPlaylistDetail.playlist.value?.favorites ?: emptyList() ,
                    vmPlaylistDetail.selectedIdsSet.value?.toSet() ?: emptySet()
                )
            }
            else {
                adapter.changeSelectionMode(false)
                adapter.updateList(
                    vmPlaylistDetail.playlist.value?.favorites ?: emptyList() ,
                    vmPlaylistDetail.selectedIdsSet.value?.toSet() ?: emptySet()
                )
            }
        }

        binding.editLocationFavorites.setOnClickListener {
            if (!adapter.isEditMode) {
                adapter.changeEditMode(true)
                adapter.updateList(
                    vmPlaylistDetail.playlist.value?.favorites ?: emptyList() ,
                    vmPlaylistDetail.selectedIdsSet.value?.toSet() ?: emptySet()
                )
            }
            else {
                adapter.changeEditMode(false)
                adapter.updateList(
                    vmPlaylistDetail.playlist.value?.favorites ?: emptyList() ,
                    vmPlaylistDetail.selectedIdsSet.value?.toSet() ?: emptySet()
                )
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.trackRecyclerView.adapter = null
        _binding = null
    }

    companion object {
        const val ARGUMENT_KEY = "playlist"
        const val TAG = "PlaylistDetailFragment"
    }

}
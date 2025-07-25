package com.example.mymusic.ui.playlist.searchPlaylist

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentSearchPlaylistBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import com.example.mymusic.util.FavoritesSearchUtils
import com.example.mymusic.util.SortFilterUtil

@UnstableApi
class SearchPlaylistFragment : Fragment() {

    private val mainActivityViewModel: MainActivityViewModel  by activityViewModels()
    private val searchPlaylistViewModel: SearchPlaylistViewModel by viewModels()

    private var _binding : FragmentSearchPlaylistBinding? = null
    private val favoriteAdapter: SearchPlaylistAdapter by lazy {
        SearchPlaylistAdapter(
            emptyList(),
            emptySet(),
            emptySet(),
            object : SearchPlaylistAdapter.OnClickListener {
                override fun onPlayButtonClick(favorite: Favorite, position: Int) {
                    val playlist = searchPlaylistViewModel.favoriteList.value?.let {
                        it.subList(position, it.size) + it.subList(0, position)
                    }
                    if (!playlist.isNullOrEmpty())
                        mainActivityViewModel.setPlaylist(playlist, 0)
                }

                override fun onToggleSelection(favorite: Favorite, checked: Boolean) {
                    searchPlaylistViewModel.toggleSelection(favorite)
                }

            })
    }

    private val relatedAdapter: SearchPlaylistAdapter by lazy {
        SearchPlaylistAdapter(
            emptyList(),
            emptySet(),
            emptySet(),
            object : SearchPlaylistAdapter.OnClickListener {
                override fun onPlayButtonClick(favorite: Favorite, position: Int) {
                    val playlist = searchPlaylistViewModel.relatedList.value?.let {
                        it.subList(position, it.size) + it.subList(0, position)
                    }
                    if (!playlist.isNullOrEmpty())
                        mainActivityViewModel.setPlaylist(playlist, 0)
                }

                override fun onToggleSelection(favorite: Favorite, checked: Boolean) {
                    searchPlaylistViewModel.toggleSelection(favorite)
                }

            })
    }

    private val bottomSheet: FilterBottomSheetFragment by lazy { FilterBottomSheetFragment() }
    private var isDescending = true
    val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        receiveArgument()
        bind()
        setObserver()
        setClickEvent()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun receiveArgument() {
        val receivedPlaylist = arguments?.getParcelable(PLAYLIST_KEY, Playlist::class.java)
        searchPlaylistViewModel.playlist = receivedPlaylist
        val alreadyExistSet = receivedPlaylist?.trackIds?.toMutableSet() ?: mutableSetOf()
        if ((searchPlaylistViewModel.alreadyExistSet.value?.isEmpty() ?: true && !alreadyExistSet.isEmpty())){
            searchPlaylistViewModel.initializeAlreadyExistSet(alreadyExistSet)

            binding.trackRecyclerView.postDelayed({
                relatedAdapter.notifyExistChangedByIds(alreadyExistSet)
                favoriteAdapter.notifyExistChangedByIds(alreadyExistSet)
            },80)
        }
    }

    private fun bind() {
        searchPlaylistViewModel.loadFavorites()
        binding.trackRecyclerView.adapter = favoriteAdapter
        binding.trackRelatedRecyclerView.adapter = relatedAdapter
        binding.trackRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.trackRelatedRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.addSelectedFavorites.alpha = 0.6f
        binding.cancelSelectionMode.alpha = 0.6f
        binding.keywordSearchedCount.visibility = View.INVISIBLE
        binding.inOrderButtonDropUp.visibility = View.INVISIBLE
        binding.inOrderButtonDropDown.visibility = View.VISIBLE
    }
    private fun updateSelectionButtonsState() {
        if (searchPlaylistViewModel.selectedList.isEmpty()) {
            binding.addSelectedFavorites.alpha = 0.6f
            binding.cancelSelectionMode.alpha = 0.6f
            binding.cancelSelectionMode.setOnClickListener(null)
            binding.addSelectedFavorites.setOnClickListener(null)
        }
        else {
            binding.addSelectedFavorites.alpha = 1f
            binding.cancelSelectionMode.alpha = 1f
            binding.cancelSelectionMode.setOnClickListener {
                searchPlaylistViewModel.unselectAll()
            }
            binding.addSelectedFavorites.setOnClickListener {
                val dialog = Dialog(requireContext())
                    .apply {
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                        setCancelable(true)
                        setContentView(R.layout.dialog_custom)
                    }
                val title = dialog.findViewById<TextView>(R.id.title)
                val subText = dialog.findViewById<TextView>(R.id.subtext)
                val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)

                title.text = "${searchPlaylistViewModel.playlist?.playlistName ?: "playlist 제목 없음"}"
                val selectedList = searchPlaylistViewModel.selectedList
                if (selectedList.size > 3) {
                    subText.text = "정말\n${
                        selectedList.take(3).map { it.title }.joinToString(", ")
                    }\n외 ${selectedList.size - 3}곡을 추가하시겠습니까?"
                }
                else {
                    subText.text = "정말\n${
                        selectedList.take(3).map { it.title }.joinToString(", ")
                    }\n${selectedList.size}곡을 추가하시겠습니까?"
                }
                cancelButton.setOnClickListener { dialog.dismiss() }
                confirmButton.setOnClickListener {
                    dialog.dismiss()
                    searchPlaylistViewModel.addTracksToPlaylist()
                }
                dialog.show()
            }
        }
    }

    private fun sortAndFilteringList() {
        val prefs = requireActivity().getSharedPreferences("filter_prefs_in_search_playlist_track", Context.MODE_PRIVATE)
        val sortOpt = prefs.getString("sort_option", "ADDED_DATE")
        val filterOpt = prefs.getString("filter_option", "ALL")
        val sortAndFiltered = SortFilterUtil.sortAndFilterFavoritesList(
            requireContext(),
            searchPlaylistViewModel.rawList.value,
            filterOpt,
            sortOpt,
            isDescending)
        searchPlaylistViewModel.favoriteList.value = sortAndFiltered
    }

    private fun setObserver(){
        bottomSheet.setApplyListener(object : FilterBottomSheetFragment.OnApplyListener{
            override fun onApply() {
               sortAndFilteringList()
            }
        })
        searchPlaylistViewModel.rawList.observe(viewLifecycleOwner) { rawList ->
            sortAndFilteringList()
        }
        searchPlaylistViewModel.favoriteList.observe(viewLifecycleOwner) { favoriteList ->
            favoriteList?.let {
                favoriteAdapter.updateList(it, searchPlaylistViewModel.alreadyExistSet.value?.toSet() ?: setOf(), searchPlaylistViewModel.selectedIds)
                val count = favoriteList.size
                binding.emptyFavoriteSong.visibility = if (count == 0) View.VISIBLE else View.GONE
                if (count == 0) {
                    binding.favoritesLoadedCount.text = "No"
                    binding.elementCount.text = "Songs"
                }
                else if (count == 1) {
                    binding.favoritesLoadedCount.text = "A"
                    binding.elementCount.text = "Song"
                }
                else {
                    binding.favoritesLoadedCount.text = count.toString()
                    binding.elementCount.text = "Songs"
                }
            }
        }
        searchPlaylistViewModel.relatedList.observe(viewLifecycleOwner) { relatedList ->
            updateSelectionButtonsState()
            if (!relatedList.isNullOrEmpty()) {
                Log.d(TAG, "selected : " + searchPlaylistViewModel.selectedList)
                relatedAdapter.updateList(
                    relatedList,
                    searchPlaylistViewModel.alreadyExistSet.value?.toSet() ?: setOf(),
                    searchPlaylistViewModel.selectedIds
                )
                searchPlaylistViewModel.favoriteList.value?.let {
                    favoriteAdapter.updateList(
                        it,
                        searchPlaylistViewModel.alreadyExistSet.value?.toSet() ?: setOf(),
                        searchPlaylistViewModel.selectedIds
                    )
                }
            }
        }
        searchPlaylistViewModel.alreadyExistSet.observe(viewLifecycleOwner) { existSet ->
            Log.d(TAG, existSet.toString())
            val relatedList = searchPlaylistViewModel.relatedList.value
            relatedList?.let {
                relatedAdapter.notifyExistChangedByIds(existSet.toSet())
            }

            val favList = searchPlaylistViewModel.favoriteList.value
            favList?.let {
                favoriteAdapter.notifyExistChangedByIds(existSet.toSet())
            }
        }
    }

    private fun updateHighlightedPositionList(newKeyword: String?, scrolling: Boolean) {
        if (newKeyword == null || newKeyword.trim().isEmpty()) {
            binding.keywordSearchedCount.visibility = View.GONE
            favoriteAdapter.setKeyword(null)
            return
        }
        val favorites = searchPlaylistViewModel.favoriteList.value
        favorites.let {
            favoriteAdapter.setKeyword(newKeyword.trim())
            searchPlaylistViewModel.highlightedPositions = null
            val highlightedPositions: List<Int> =
                FavoritesSearchUtils.getContainPositions(newKeyword, it) { favorite ->
                    favorite.title + favorite.artistName
                }
            if (highlightedPositions.isNotEmpty()) {
                if (scrolling) binding.trackRecyclerView.smoothScrollToPosition(highlightedPositions[0])
                searchPlaylistViewModel.highlightedPositions = highlightedPositions
                val countText = "1/" + highlightedPositions.size
                binding.keywordSearchedCount.visibility = View.VISIBLE
                binding.keywordSearchedCount.text = countText
            }
            else {
                binding.keywordSearchedCount.text = ""
                binding.keywordSearchedCount.visibility = View.INVISIBLE
            }


        }
    }

    private fun scrollToPreviousSearched() {
        val highlightPositions = searchPlaylistViewModel.highlightedPositions
        if (!highlightPositions.isNullOrEmpty()) {
            val currentPosition = searchPlaylistViewModel.scrolledHighlightedPosition
            val indexOfCurrentPosition = highlightPositions.indexOf(currentPosition)
            val index =(indexOfCurrentPosition + highlightPositions.size - 1) % highlightPositions.size
            val previousPosition = highlightPositions[index]
            searchPlaylistViewModel.scrolledHighlightedPosition = previousPosition

            val layoutManager = binding.trackRecyclerView.layoutManager as LinearLayoutManager?
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(previousPosition, 0)
            } else {
                binding.trackRecyclerView.smoothScrollToPosition(previousPosition)
            }
            val count = (index + 1).toString() + "/" + highlightPositions.size
            binding.keywordSearchedCount.text = count
        }
    }
    private fun scrollToNextSearched() {
        val highlightPositions = searchPlaylistViewModel.highlightedPositions
        if (!highlightPositions.isNullOrEmpty()) {
            val currentPosition = searchPlaylistViewModel.scrolledHighlightedPosition
            val indexOfCurrentPosition = highlightPositions.indexOf(currentPosition)
            val index =(indexOfCurrentPosition + highlightPositions.size + 1) % highlightPositions.size
            val previousPosition = highlightPositions[index]
            searchPlaylistViewModel.scrolledHighlightedPosition = previousPosition

            val layoutManager = binding.trackRecyclerView.layoutManager as LinearLayoutManager?
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(previousPosition, 0)
            } else {
                binding.trackRecyclerView.smoothScrollToPosition(previousPosition)
            }
            val count = (index + 1).toString() + "/" + highlightPositions.size
            binding.keywordSearchedCount.text = count
            Log.d("RV", "itemCount=${binding.trackRecyclerView.adapter?.itemCount} " +
                    "pos=$previousPosition " +
                    "canScroll=${binding.trackRecyclerView.canScrollVertically(1) || binding.trackRecyclerView.canScrollVertically(-1)} " +
                    "attached=${binding.trackRecyclerView.isAttachedToWindow}")

        }
    }


    private fun setClickEvent() {
        binding.searchKeyword.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (searchPlaylistViewModel.favoriteList.value.isNullOrEmpty()) return
                searchPlaylistViewModel.keyword = s?.trim().toString()
                updateHighlightedPositionList(
                    newKeyword = s?.trim().toString(),
                    scrolling = true
                )
            }

        })
        binding.previousKeyword.setOnClickListener {
            if (binding.keywordSearchedCount.isVisible)
                scrollToPreviousSearched()
            else {
                binding.trackRecyclerView.smoothScrollToPosition(0)
            }
        }

        binding.nextKeyword.setOnClickListener {
            if (binding.keywordSearchedCount.isVisible)
                scrollToNextSearched()
            else
                binding.trackRecyclerView.smoothScrollToPosition((searchPlaylistViewModel.favoriteList.value?.size?.minus(1)) ?: 0)
        }

        binding.filterButton.setOnClickListener {
            if (!bottomSheet.isAdded && !bottomSheet.isVisible) {
                bottomSheet.show(getParentFragmentManager(), "FilterBottomSheet")
            }
        }
        binding.inOrderButtonDropUp.setOnClickListener {
            it.visibility = View.INVISIBLE
            binding.inOrderButtonDropDown.visibility = View.VISIBLE
            isDescending = true
            sortAndFilteringList()

        }
        binding.inOrderButtonDropDown.setOnClickListener {
            it.visibility = View.INVISIBLE
            binding.inOrderButtonDropUp.visibility = View.VISIBLE
            isDescending = false
            sortAndFilteringList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.trackRecyclerView.adapter = null
        binding.trackRelatedRecyclerView.adapter = null
        _binding = null
    }

    companion object {
        const val TAG = "SearchPlaylistFragment"
        const val PLAYLIST_KEY = "playlist"
    }
}
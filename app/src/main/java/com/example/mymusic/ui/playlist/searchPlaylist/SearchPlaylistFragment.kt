package com.example.mymusic.ui.playlist.searchPlaylist

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentSearchPlaylistBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private fun setObserver(){
        searchPlaylistViewModel.favoriteList.observe(viewLifecycleOwner) { favoriteList ->
            favoriteList?.let {
                favoriteAdapter.updateList(it, searchPlaylistViewModel.alreadyExistSet.value?.toSet() ?: setOf(), searchPlaylistViewModel.selectedIds)
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.trackRecyclerView.adapter = null
        binding.trackRelatedRecyclerView.adapter = null
        _binding = null
    }

    companion object {
        const val TAG = "SearchPlaylistFragment"
        const val PLAYLIST_KEY = "playlist"
        const val ALREADY_EXIST_LIST_KEY = "already_exist_list"
    }
}
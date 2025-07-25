package com.example.mymusic.ui.playlist

import android.app.Dialog
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout

import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.room.util.query
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentPlaylistLibraryBinding
import com.example.mymusic.model.Playlist
import com.example.mymusic.ui.playlist.searchPlaylist.SearchPlaylistFragment
import java.util.ArrayList
import androidx.core.graphics.drawable.toDrawable
import coil.request.Tags
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.ui.playlist.playlistDetail.PlaylistDetailFragment

@UnstableApi
class PlaylistLibraryFragment : Fragment() {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val playlistLibraryViewModel: PlaylistLibraryViewModel by viewModels()
    private var _binding : FragmentPlaylistLibraryBinding? = null
    val binding get() = _binding!!

    val adapter: PlaylistLibraryAdapter by lazy { PlaylistLibraryAdapter(
        emptyList(),
        playlistLibraryViewModel.viewModelScope,
        object : PlaylistLibraryAdapter.OnClickListener {
            override fun onItemClick(playlist: Playlist) {
                val args = Bundle().apply {
                    putParcelable(PlaylistDetailFragment.ARGUMENT_KEY, playlist)
                }
                findNavController().navigate(R.id.action_navPlaylistLibrary_to_fragPlaylistDetail, args)
            }

            override fun onPlayButtonClick(playlist: Playlist) {
                val list = playlist.favorites
                if (list.isEmpty()) return
                //mainActivityViewModel.setPlaylist(list, 0)
                mainActivityViewModel.setPlaylistFromPlaylistInOrder(playlist, 0)
            }

            override fun onShuffleButtonClick(playlist: Playlist) {
                playlist.favorites.isNotEmpty().let {
                    mainActivityViewModel.setPlaylistFromPlaylistWithShuffle(
                        playlist
                            .deepCopy()
                            .apply { shuffle() }
                    )
                }
            }

            override fun onMenuClick(anchorView: View, playlist: Playlist) {
                val popupView = layoutInflater.inflate(R.layout.popup_menu_for_playlist, null)
                val popupWindowWidth = 360
                val popupWindow = PopupWindow(
                    popupView,
                    popupWindowWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    isOutsideTouchable = true
                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    isFocusable = false
                    elevation = 16f
                }

                val addNowPlayingView = popupView.findViewById<LinearLayout>(R.id.add_now_playing_layout)
                val modifyPlaylistNameView = popupView.findViewById<LinearLayout>(R.id.modify_layout)
                val addTracksToPlaylistView = popupView.findViewById<LinearLayout>(R.id.add_tracks_to_playlist_layout)
                val deletePlaylist = popupView.findViewById<LinearLayout>(R.id.delete_layout)


                val isNonEditable = playlist.playlistId == PlaylistRepository.PLAYLIST_ID_RECENTLY_PLAYED
                if (isNonEditable) {
                    modifyPlaylistNameView.visibility = View.GONE
                    addTracksToPlaylistView.visibility = View.GONE
                    deletePlaylist.visibility = View.GONE
                }

                anchorView.post {
                    val anchorLocation = IntArray(2)
                    anchorView.getLocationOnScreen(anchorLocation)
                    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    val popupViewHeight = popupView.measuredHeight
                    val anchorX = anchorLocation[0]
                    val anchorY = anchorLocation[1]
                    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                    var x = screenWidth - 7*anchorX - popupWindowWidth
                    var y = if (isNonEditable) anchorY + popupViewHeight else anchorY
                    popupWindow.showAtLocation(anchorView.rootView, Gravity.NO_GRAVITY, x, y)
                }

                addNowPlayingView.setOnClickListener {
                    popupWindow.dismiss()
                    mainActivityViewModel.addPlaylist(playlist.favorites)
                }

                modifyPlaylistNameView.setOnClickListener {
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
                    val nameDuplicationNoticeTextView = dialog.findViewById<TextView>(R.id.name_duplication_notice)
                    nameDuplicationNoticeTextView.visibility = View.INVISIBLE

                    editText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {}

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
                            }
                            else if (trimmed in playlistLibraryViewModel.playlistNameSet) {
                                confirmButton.alpha = 0.5f
                                confirmButton.setOnClickListener(null)
                                nameDuplicationNoticeTextView.visibility = View.VISIBLE
                                if (trimmed.equals("최근 재생한 음악"))
                                    nameDuplicationNoticeTextView.text = "사용할 수 없는 이름입니다"
                                else if (trimmed.equals(playlist.playlistName))
                                    nameDuplicationNoticeTextView.text = "같은 이름으로 변경할 수 없습니다"
                                else
                                    nameDuplicationNoticeTextView.text = "이미 사용중인 이름입니다"
                            }
                            else {
                                nameDuplicationNoticeTextView.visibility = View.INVISIBLE
                                confirmButton.alpha = 1f
                                confirmButton.setOnClickListener {
                                    val newListName = trimmed
                                    playlistLibraryViewModel.renamePlaylist(playlist = playlist, newName = newListName)
                                    dialog.dismiss()
                                    popupWindow.dismiss()
                                }
                            }
                        }

                        override fun afterTextChanged(s: Editable?) {}

                    })
                    cancelButton.setOnClickListener { dialog.dismiss() }

                    dialog.show()
                }
                deletePlaylist.setOnClickListener {
                    val dialog = Dialog(requireContext()).apply {
                        setCancelable(true)
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                        setContentView(R.layout.dialog_custom)
                    }
                    val title = dialog.findViewById<TextView>(R.id.title)
                    val subText = dialog.findViewById<TextView>(R.id.subtext)
                    val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                    val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)
                    title.text = "Playlist 삭제"
                    confirmButton.text = "삭제"
                    subText.text = "정말 '${playlist.playlistName}' 플레이리스트를 삭제하시겠습니까?"
                    cancelButton.setOnClickListener { dialog.dismiss() }
                    confirmButton.setOnClickListener {
                        popupWindow.dismiss()
                        dialog.dismiss()
                        playlistLibraryViewModel.deletePlaylist(playlistId = playlist.playlistId, playlistName = playlist.playlistName)
                    }
                    dialog.show()
                }
                addTracksToPlaylistView.setOnClickListener {
                    popupWindow.dismiss()
                    val args = Bundle().apply {
                        putParcelable(SearchPlaylistFragment.PLAYLIST_KEY, playlist)
                    }
                    findNavController().navigate(R.id.action_navPlaylistLibrary_to_fragSearchPlaylist, args)
                }
            }

            override fun onAddNewPlaylist() {
                val dialog = Dialog(requireContext())
                dialog.setContentView(R.layout.dialog_custom_edittext_with_notice)
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.setCancelable(true)
                val titleTextView = dialog.findViewById<TextView>(R.id.title)
                val editText = dialog.findViewById<EditText>(R.id.edit_text)
                val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)
                val nameDuplicationNoticeTextView = dialog.findViewById<TextView>(R.id.name_duplication_notice)
                nameDuplicationNoticeTextView.visibility = View.INVISIBLE
                confirmButton.alpha = 0.5f
                titleTextView.text = "플레이리스트 추가"
                editText.setText("")
                editText.hint = "제목"
                editText.setSingleLine(true)
                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

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
                        }
                        else if (trimmed in playlistLibraryViewModel.playlistNameSet) {
                            confirmButton.alpha = 0.5f
                            confirmButton.setOnClickListener(null)
                            nameDuplicationNoticeTextView.visibility = View.VISIBLE
                            if (trimmed.equals("최근 재생한 음악"))
                                nameDuplicationNoticeTextView.text = "사용할 수 없는 이름입니다"
                            else
                                nameDuplicationNoticeTextView.text = "이미 사용중인 이름입니다"
                        }
                        else {
                            nameDuplicationNoticeTextView.visibility = View.INVISIBLE
                            confirmButton.alpha = 1f
                            confirmButton.setOnClickListener {
                                val newListName = trimmed
                                val newPlaylist = Playlist(playlistName = newListName)

                                playlistLibraryViewModel.createNewPlaylist(playlist = newPlaylist)

                                dialog.dismiss()
                                val args = Bundle().apply {
                                    putParcelable(SearchPlaylistFragment.PLAYLIST_KEY, newPlaylist)
                                }
                                findNavController().navigate(R.id.action_navPlaylistLibrary_to_fragSearchPlaylist, args)
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}

                })
                cancelButton.setOnClickListener { dialog.dismiss() }

                dialog.show()

            }

        }
    ) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistLibraryViewModel.loadPlaylists()
        bind()
        setObserve()
    }

    private fun bind() {
        binding.playlistRecyclerView.adapter = adapter
        binding.playlistRecyclerView.layoutManager = LinearLayoutManager(context)
    }
    private fun setObserve() {
        playlistLibraryViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.updateList(playlists)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playlistRecyclerView.adapter = null
    }

    companion object {
        const val TAG = "PlaylistLibraryFragment"
    }
}
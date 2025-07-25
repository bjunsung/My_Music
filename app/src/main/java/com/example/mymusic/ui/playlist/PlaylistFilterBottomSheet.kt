package com.example.mymusic.ui.playlist

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.mymusic.databinding.FragmentPlaylistFilterBottomSheetBinding
import com.example.mymusic.util.DarkModeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaylistFilterBottomSheet : BottomSheetDialogFragment() {

    private val vmPlaylistLibrary: PlaylistLibraryViewModel by activityViewModels()
    private val sortTextViewMap: MutableMap<String, TextView> = mutableMapOf()
    var sortOpt: String? = null

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true      // 선택: 바로 펼치고 싶으면
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener(OnShowListener { dialogInterface: DialogInterface? ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout?>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
        })

        return bottomSheetDialog
    }

    private var _binding: FragmentPlaylistFilterBottomSheetBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        setUp()
    }

    private fun bindView() {
        sortTextViewMap.put(
            PlaylistLibraryViewModel.ADDED_DATE,
            binding.addedDate
        )
        sortTextViewMap.put(
            PlaylistLibraryViewModel.RECENTLY_PLAYED,
            binding.recentlyPlayed
        )
        sortTextViewMap.put(
            PlaylistLibraryViewModel.DURATION,
            binding.duration
        )
        sortTextViewMap.put(
            PlaylistLibraryViewModel.PLAY_COUNT,
            binding.playCount
        )
        binding.applyButton.setOnClickListener {
            sortOpt?.let { vmPlaylistLibrary.setSortOption(it) }
            dismiss()
        }
    }

    private fun setUp() {
        val initialSortOpt = vmPlaylistLibrary.sortOption.value
        initialSortOpt?.let { setUpHighlight(initialSortOpt) }
        setupClickListeners()
    }

    private fun setUpHighlight(sortOpt: String) {
        // 모든 텍스트뷰 초기화
        for (tv in sortTextViewMap.values) {
            tv.setTypeface(null, Typeface.NORMAL)
            tv.setTextColor(Color.GRAY)
        }
        val selectedSort = sortTextViewMap[sortOpt]
        selectedSort?.let {
            // 선택된 항목만 Bold 처리
            it.setTypeface(null, Typeface.BOLD)
            if (DarkModeUtils.isDarkMode(requireContext())) {
                val color = "#DBDBDB".toColorInt()
                it.setTextColor(color)
            } else {
                it.setTextColor(Color.DKGRAY)
            }
        }
    }

    private fun setupClickListeners() {
        for (entry in sortTextViewMap.entries) {
            val key: String = entry.key
            val tv = entry.value
            tv.setOnClickListener(View.OnClickListener { v: View? ->
                setUpHighlight(key)
                sortOpt = key
            })
        }
    }

}

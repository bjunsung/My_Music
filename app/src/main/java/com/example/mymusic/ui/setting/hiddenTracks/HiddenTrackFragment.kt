package com.example.mymusic.ui.setting.hiddenTracks

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymusic.databinding.FragmentHiddenTrackBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.R

class HiddenTrackFragment: Fragment() {

    var _binding: FragmentHiddenTrackBinding? = null
    val binding get () = _binding!!

    val vmHiddenTrack : HiddenTrackViewModel by viewModels()
    val adapter: HiddenTrackAdapter by lazy { HiddenTrackAdapter(
        emptyList(),
        object : HiddenTrackAdapter.OnClickListener {
            override fun onDeleteButtonClick(favorite: Favorite) {
                val dialog  = Dialog(requireContext()).apply {
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    setContentView(R.layout.dialog_custom)
                    setCancelable(true)
                }
                val titleTextView = dialog.findViewById<TextView>(R.id.title)
                val subTextView = dialog.findViewById<TextView>(R.id.subtext)
                val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)

                titleTextView.text = "삭제"
                subTextView.text = "정말 ${favorite.title} - ${favorite.artistName} 을(를) 삭제하시겠습니까?"
                cancelButton.setOnClickListener { dialog.dismiss() }
                confirmButton.setOnClickListener {
                    dialog.dismiss()
                    vmHiddenTrack.deleteTrack(favorite)
                }
                dialog.show()
            }

            override fun onRestoreVisibilityButtonClick(favorite: Favorite) {
                val dialog  = Dialog(requireContext()).apply {
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    setContentView(R.layout.dialog_custom)
                    setCancelable(true)
                }
                val titleTextView = dialog.findViewById<TextView>(R.id.title)
                val subTextView = dialog.findViewById<TextView>(R.id.subtext)
                val cancelButton = dialog.findViewById<TextView>(R.id.cancel_button)
                val confirmButton = dialog.findViewById<TextView>(R.id.confirm_button)

                titleTextView.text = "숨김 취소"
                subTextView.text = "정말 ${favorite.title} - ${favorite.artistName} 을(를) 숨김 취소 하시겠습니까?"
                cancelButton.setOnClickListener { dialog.dismiss() }
                confirmButton.setOnClickListener {
                    dialog.dismiss()
                    vmHiddenTrack.restoreVisibility(favorite)
                }
                dialog.show()
            }

        }
    ) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHiddenTrackBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
        val saved = vmHiddenTrack.hiddenTracks.value
        if (saved == null || saved.isEmpty())
            vmHiddenTrack.loadHiddenTracks()
        setObserver()

    }

    private fun bind() {
        binding.trackRecyclerView.adapter = adapter
        binding.trackRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setObserver() {
        vmHiddenTrack.hiddenTracks.observe(viewLifecycleOwner) { hiddenTracks ->
            if (hiddenTracks.isEmpty()) {
                binding.trackRecyclerView.visibility = View.GONE
                binding.emptyStatement.visibility = View.VISIBLE
            }
            else {
                binding.trackRecyclerView.visibility = View.VISIBLE
                binding.emptyStatement.visibility = View.GONE
            }
            adapter.updateList(hiddenTracks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.trackRecyclerView.adapter = null
        _binding = null
    }

}
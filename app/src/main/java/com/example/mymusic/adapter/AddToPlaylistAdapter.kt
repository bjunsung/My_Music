package com.example.mymusic.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.model.Playlist

class AddToPlaylistAdapter(
    var playlists: List<Playlist>,
    var selectedSet: MutableSet<String>,
    val listener: OnClickListener,
) : RecyclerView.Adapter<AddToPlaylistAdapter.ViewHolder>() {

    interface OnClickListener{
        fun onItemClick(playlist: Playlist)
        fun onPlayButtonClick(playlist: Playlist)
        fun onShuffleButtonClick(playlist: Playlist)
        fun onToggleSelection(playlist: Playlist, checked: Boolean)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_containing_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            bindFull(holder, playlists[position])
        } else {
            if (payloads.contains(PAYLOAD_SELECTION)) {
                val item = playlists[position]
                // ✅ 리스너 해제 후 체크 상태만 업데이트
                holder.checkBox.setOnCheckedChangeListener(null)
                holder.checkBox.isChecked = item.playlistId in selectedSet
                holder.checkBox.setOnCheckedChangeListener { _, checked ->
                    toggleSelectionInternal(item, checked, holder.bindingAdapterPosition)
                }
            }
        }
    }

    private fun bindFull(holder: ViewHolder, item: Playlist) {
        holder.checkBox.visibility = View.VISIBLE
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.playlistId in selectedSet
        holder.checkBox.setOnCheckedChangeListener { _, checked ->
            toggleSelectionInternal(item, checked, holder.bindingAdapterPosition)
        }
        holder.playlistName.text = item.playlistName
        holder.trackCountTextView.text = "${item.size} 곡"
        holder.playTimeTextView.text = item.getDurationStr()

        holder.shuffleButton.setOnClickListener { listener.onShuffleButtonClick(item) }
        holder.playButton.setOnClickListener { listener.onPlayButtonClick(item) }

        holder.itemView.setOnClickListener {
            holder.checkBox.toggle()
        }
    }


    private fun toggleSelectionInternal(item: Playlist, checked: Boolean, pos: Int) {
        if (pos == RecyclerView.NO_POSITION) return
        if (checked) selectedSet.add(item.playlistId) else selectedSet.remove(item.playlistId)
        notifyItemChanged(pos, PAYLOAD_SELECTION)
        listener.onToggleSelection(item, checked)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val item = playlists[position]
        bindFull(holder, item)
    }

    override fun getItemCount(): Int = playlists.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Playlist>, newSelectedSet: MutableSet<String>) {
        val oldList = playlists
        val oldSelected = selectedSet

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = playlists.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int,
            ): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return oldItem.playlistId == newItem.playlistId
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int,
            ): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                val playlistId = oldItem.playlistId
                return newItem.playlistId == playlistId
                        && oldSelected.contains(playlistId) == newSelectedSet.contains(playlistId)
            }

        })

        this.playlists = newList
        this.selectedSet = newSelectedSet
        diff.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val playlistName = view.findViewById<TextView>(R.id.playlist_name)
        val trackCountTextView = view.findViewById<TextView>(R.id.track_count_text)
        val playTimeTextView = view.findViewById<TextView>(R.id.playlist_play_time_text)
        val shuffleButton = view.findViewById<ImageButton>(R.id.playlist_shuffle_button)
        val playButton = view.findViewById<ImageButton>(R.id.playlist_play_button)
        val checkBox = view.findViewById<AppCompatCheckBox>(R.id.check_box)
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection"
    }
}
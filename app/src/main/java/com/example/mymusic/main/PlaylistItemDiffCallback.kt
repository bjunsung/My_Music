package com.example.mymusic.model


import androidx.recyclerview.widget.DiffUtil

class PlaylistItemDiffCallback(
    private val oldList: List<Favorite>,
    private val newList: List<Favorite>,
    backgroundColor: Integer,
    playingPosition: Int
) :
    DiffUtil.Callback() {

    private var backgroundColor = -1
    private var playingPosition = 0

    init {
        this.backgroundColor = backgroundColor.toInt()
        this.playingPosition = playingPosition
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].track.trackId == newList[newItemPosition].track.trackId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.backgroundColor == backgroundColor
                && oldItem.playingPosition == playingPosition

    }
}
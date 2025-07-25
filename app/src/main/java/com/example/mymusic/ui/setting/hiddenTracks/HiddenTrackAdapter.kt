package com.example.mymusic.ui.setting.hiddenTracks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.model.Favorite
import com.example.mymusic.R

class HiddenTrackAdapter(
    private var hiddenTracks: List<Favorite>,
    private val listener: OnClickListener
): RecyclerView.Adapter<HiddenTrackAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onDeleteButtonClick(favorite: Favorite)
        fun onRestoreVisibilityButtonClick(favorite: Favorite)
    }

    private lateinit var viewGroupContext: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        viewGroupContext = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_hidden, parent, false)
        return ViewHolder(view);
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = hiddenTracks.get(position)
        holder.titleTextView.text = item.title
        holder.albumNameTextView.text = item.track.albumName
        holder.artistNameTextView.text = item.artistName
        holder.durationTextView.text = item.durationStr
        holder.releaseDateTextView.text = item.releaseDate

        val size = viewGroupContext.resources.getDimensionPixelSize(R.dimen.artwork_override_small)
        Glide.with(holder.itemView)
            .load(item.track.artworkUrl)
            .override(size, size)
            .error(R.drawable.ic_image_not_found_foreground)
            .into(holder.artworkImage)

        holder.deleteButton.setOnClickListener { listener.onDeleteButtonClick(item) }
        holder.restoreVisibilityButton.setOnClickListener { listener.onRestoreVisibilityButtonClick(item) }
    }

    override fun getItemCount(): Int = hiddenTracks.size

    fun updateList(newList: List<Favorite>) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int = hiddenTracks.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                val oldItem =  hiddenTracks.get(oldItemPosition)
                val newItem = newList.get(newItemPosition)
                return oldItem.track.trackId == newItem.track.trackId
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                val oldItem =  hiddenTracks.get(oldItemPosition)
                val newItem = newList.get(newItemPosition)
                return oldItem.track.trackId == newItem.track.trackId &&
                        oldItem.isHidden == newItem.isHidden
            }
        })
        hiddenTracks = newList
        diff.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val artworkImage = view.findViewById<ImageView>(R.id.artwork_image)
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val albumNameTextView = view.findViewById<TextView>(R.id.album_name)
        val artistNameTextView = view.findViewById<TextView>(R.id.artist_name)
        val durationTextView = view.findViewById<TextView>(R.id.duration)
        val releaseDateTextView = view.findViewById<TextView>(R.id.release_date)
        val deleteButton = view.findViewById<ImageButton>(R.id.delete)
        val restoreVisibilityButton = view.findViewById<ImageButton>(R.id.restore_visibility)
    }
}
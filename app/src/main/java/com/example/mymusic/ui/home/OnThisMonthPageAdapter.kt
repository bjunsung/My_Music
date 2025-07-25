package com.example.mymusic.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.model.Favorite
import com.example.mymusic.R
import com.example.mymusic.ui.musicInfo.MusicInfoFragment

class OnThisMonthPageAdapter(
    private var items: List<Favorite>,
    private val listener :OnClickListener
) : RecyclerView.Adapter<OnThisMonthPageAdapter.ViewHolder>(){


    interface OnClickListener {
        fun onItemClick(holder: ViewHolder, favorite: Favorite)
        fun onPlayButtonClick(list: List<Favorite>, position: Int)
    }

    lateinit var viewGroupContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        viewGroupContext = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_pager_with_inner_text_and_button, parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.get(position)
        val trackId = item.track.trackId
        holder.artworkImage.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_ARTWORK_IMAGE + trackId
        holder.titleTextView.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_TITLE + trackId
        holder.artistNameTextView.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_ARTIST +  trackId
        holder.releaseDateTextView.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_RELEASE_DATE + trackId

        Glide.with(holder.itemView)
            .load(item.track.artworkUrl)
            .error(R.drawable.ic_image_not_found_foreground)
            .into(holder.artworkImage)

        holder.playButton.visibility = if (item.audioUri.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.playButton.setOnClickListener { listener.onPlayButtonClick(items, holder.bindingAdapterPosition) }
        holder.titleTextView.text = item.title ?: "Title 정보 없음"
        holder.artistNameTextView.text = item.artistName ?: "Artist 정보 없음"
        holder.releaseDateTextView.text = item.releaseDate

        holder.itemView.setOnClickListener { listener.onItemClick(holder, item) }
    }

    fun updateList(newList: List<Favorite>) {
        items = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artworkImage = itemView.findViewById<ImageView>(R.id.artwork_image)
        val playButton = itemView.findViewById<ImageButton>(R.id.music_play_button)
        val titleTextView = itemView.findViewById<TextView>(R.id.title)
        val artistNameTextView = itemView.findViewById<TextView>(R.id.artist_name)
        val releaseDateTextView = itemView.findViewById<TextView>(R.id.release_date)
    }
}
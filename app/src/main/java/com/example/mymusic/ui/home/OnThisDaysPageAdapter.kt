package com.example.mymusic.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.model.Favorite
import com.example.mymusic.R

class OnThisDaysPageAdapter(
    private var items: List<Favorite>,
    private val listener :OnClickListener
) : RecyclerView.Adapter<OnThisDaysPageAdapter.ViewHolder>(){


    public interface OnClickListener {
        fun onPlayButtonClick(list: List<Favorite>, position: Int)
    }

    lateinit var viewGroupContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        viewGroupContext = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_pager_with_play_button, parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.get(position)
        Glide.with(viewGroupContext)
            .load(item.track.artworkUrl)
            .error(R.drawable.ic_image_not_found_foreground)
            .into(holder.artworkImage)

        holder.playButton.visibility = if (item.audioUri.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.playButton.setOnClickListener { listener.onPlayButtonClick(items, holder.bindingAdapterPosition) }
    }

    fun updateList(newList: List<Favorite>) {
        items = newList
        notifyDataSetChanged()
    }

    fun getItemByPosition(position: Int) : Favorite? {
        if (!(position in 0..items.size-1)) return null
        return items.get(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artworkImage = itemView.findViewById<ImageView>(R.id.artwork_image)
        val playButton = itemView.findViewById<ImageButton>(R.id.music_play_button)
    }
}
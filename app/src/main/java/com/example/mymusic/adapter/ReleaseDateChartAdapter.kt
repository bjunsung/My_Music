package com.example.mymusic.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.R
import com.example.mymusic.adapter.ReleaseDateChartAdapter.OnItemEventListener
import com.example.mymusic.model.Favorite

class ReleaseDateChartAdapter (private var list: List<Favorite>, private val listener: OnItemEventListener)
    : RecyclerView.Adapter<ReleaseDateChartAdapter.ReleaseDateViewHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReleaseDateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artwork_mini, parent, false)
        context = parent.context
        return ReleaseDateViewHolder(view)
    }

    interface OnItemEventListener {
        fun onItemClick(view: View, item: Favorite, position: Int)
        fun onImageReady()
        fun onImageLoadFailed()
    }


    private lateinit var context: Context
    private var visibleCount = 3

    override fun onBindViewHolder(
        holder: ReleaseDateViewHolder,
        position: Int
    ) {
        val fav: Favorite = list.get(position)

        holder.itemView.transitionName =
            "transitionNameFromReleaseDateChart_" + fav.track.trackId + "_" + fav.track.artworkUrl

        Glide.with(context)
            .asBitmap()
            .load(fav.track.artworkUrl)
            .override(145, 145)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.artwork.setImageBitmap(resource)
                    listener.onImageReady()
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

            })

        //holder.artwork

        holder.itemView.setOnClickListener {
            listener.onItemClick(it, fav, holder.bindingAdapterPosition)
        }
    }

    fun updateVisibleCount(visibleCount: Int) {
        this.visibleCount = visibleCount
        notifyDataSetChanged()
    }

    fun updateList(newList: List<Favorite>) {
        this.list = newList
        this.visibleCount = 3
        notifyDataSetChanged()
    }

    fun getFavoriteList(): List<Favorite> = list

    override fun getItemCount(): Int {
        return minOf(list.size, visibleCount)
    }


    class ReleaseDateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artwork: ImageView = view.findViewById(R.id.artwork_image)
    }
}


package com.example.mymusic.ui.home.trackStat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.contracts.contract
import kotlin.math.max


class TrackStatAdapter(
    private var list: List<Pair<Favorite, Int>>,
    var maxCount: Int,
    val listener: OnClickListener
) : RecyclerView.Adapter<TrackStatAdapter.TrackStatViewHolder>(){

    interface OnClickListener{
        fun onItemClick(holder: TrackStatViewHolder, favorite: Favorite)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_stat, parent, false)
        return TrackStatViewHolder(view)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(
        holder: TrackStatViewHolder,
        position: Int
    ) {
        val pair = list[position]
        val favorite = pair.first
        val recentCount = pair.second
        val totalCount = favorite.playCount

        holder.artworkCard.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_ARTWORK_IMAGE + favorite.track.trackId

        if (maxCount > 0) {
            holder.barTotal.progress = ((totalCount / maxCount.toFloat()) * 100).toInt()
            holder.barRecent.progress = ((recentCount / maxCount.toFloat()) * 100).toInt()
        }
        holder.totalCountLabel.text = totalCount.toString()
        holder.recentCountLabel.text = recentCount.toString()

        Glide.with(holder.artworkImage)
            .load(favorite.track.artworkUrl)
            .error(R.drawable.ic_image_not_found_foreground)
            .override(160, 160)
            .centerCrop()
            .into(holder.artworkImage)

        holder.artworkCard.setOnClickListener { listener.onItemClick(holder, favorite) }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<Pair<Favorite, Int>>, newMaxCount: Int) {
        this.list = newList
        this.maxCount = newMaxCount
        notifyDataSetChanged()
    }

    inner class TrackStatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artworkCard = view.findViewById<MaterialCardView>(R.id.artwork_card)
        val artworkImage = view.findViewById<ImageView>(R.id.artwork_image)
        val barTotal = view.findViewById<LinearProgressIndicator>(R.id.bar_total)
        val barRecent = view.findViewById<LinearProgressIndicator>(R.id.bar_recent)
        val totalCountLabel = view.findViewById<TextView>(R.id.label_total)
        val recentCountLabel = view.findViewById<TextView>(R.id.label_recent)
    }
}
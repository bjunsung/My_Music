package com.example.mymusic.ui.home.trackStat

import android.annotation.SuppressLint
import android.util.Log
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
import java.time.LocalDate


class TrackDropStatAdapter(
    private var list: List<Favorite>,
    var dropWindowPresetWeek: Int,
    var maxCountDiff: Int,
    val listener: OnClickListener
) : RecyclerView.Adapter<TrackDropStatAdapter.TrackStatViewHolder>(){

    interface OnClickListener{
        fun onItemClick(holder: TrackStatViewHolder, favorite: Favorite)
    }

    val today = LocalDate.now()


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_stat, parent, false)
        return TrackStatViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(
        holder: TrackStatViewHolder,
        position: Int
    ) {
        val startDate = today.minusYears(1)

        val baselineDate = today.minusWeeks (dropWindowPresetWeek.toLong())

        val favorite = list[position]
        val previousCount = favorite.playCountByDay.filterKeys { startDate <= it && it <= baselineDate }.values.sum()
        val recentCount = favorite.playCountByDay.filterKeys { baselineDate < it && it <= today }.values.sum()
        val total = previousCount + recentCount
        val pct = if (total > 0) (-1*previousCount.toDouble() / total) * 100 else 0.0
        val pctTrunc = kotlin.math.floor(pct * 10) / 10   // 소수점 첫째 자리까지 자름
        Log.d(TAG, "titie: ${favorite.title} previous: $previousCount recent: $recentCount")

        holder.artworkCard.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_ARTWORK_IMAGE + favorite.track.trackId

        holder.barAbs.max = maxCountDiff
        holder.barAbs.progress = (previousCount - recentCount).toInt()
        holder.barRelative.progress = (-1*pct).toInt()

        holder.absLabel.text = (recentCount - previousCount).toString()
        holder.relativeLabel.text = "$pctTrunc%"

        Glide.with(holder.artworkImage)
            .load(favorite.track.artworkUrl)
            .error(R.drawable.ic_image_not_found_foreground)
            .override(160, 160)
            .centerCrop()
            .into(holder.artworkImage)

        holder.artworkCard.setOnClickListener { listener.onItemClick(holder, favorite) }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<Favorite>, newDropWindowPresetWeek: Int, newMaxCountDiff: Int) {
        this.list = newList
        this.dropWindowPresetWeek = newDropWindowPresetWeek
        this.maxCountDiff = newMaxCountDiff
        notifyDataSetChanged()
    }

    inner class TrackStatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val artworkCard = view.findViewById<MaterialCardView>(R.id.artwork_card)
        val artworkImage = view.findViewById<ImageView>(R.id.artwork_image)
        val barAbs = view.findViewById<LinearProgressIndicator>(R.id.bar_total)
        val barRelative = view.findViewById<LinearProgressIndicator>(R.id.bar_recent)
        val absLabel = view.findViewById<TextView>(R.id.label_total)
        val relativeLabel = view.findViewById<TextView>(R.id.label_recent)
    }

    companion object {
        const val TAG = "TrackDropStatAdapter"
    }
}
package com.example.mymusic.ui.chart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.text.intl.Locale
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import java.time.LocalDate
import kotlin.contracts.contract
import kotlin.math.acos

class PlayTimeChartAdapter(private var list: List<Pair<Favorite, Int>>, private var chartOption: Int, private val listener: OnItemEventListener) : RecyclerView.Adapter<PlayTimeChartAdapter.PlayTimeChartViewHolder>() {


    interface OnItemEventListener {
        fun onItemClick(anchorView: View, item: Favorite, position: Int)
        fun onImageReady(transitionName: String)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private lateinit var context: Context
    private var specificYear: Int = 2024

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayTimeChartViewHolder {
        val view = LayoutInflater.from(parent.context).
        inflate(R.layout.item_favorite_rank, parent, false)
        context = parent.context
        return PlayTimeChartViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayTimeChartViewHolder, position: Int) {
        val pair = list.get(position)
        val favorite = pair.first
        Log.d(TAG, "onBindViewHolder of " + favorite.title)
        holder.rankingTextView.text = pair.second.toString()
        holder.titleTextView.text = favorite.title
        holder.titleTextView.isSelected = true
        val transitionName = favorite.title + "_" + pair.second
        holder.itemView.transitionName = transitionName

        Glide.with(context)
            .asBitmap()
            .load(favorite.track.artworkUrl)
            .override(120, 120)
            .into(object: CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.artworkImage.setImageBitmap(resource)
                    Log.d(TAG, "image ready for " + favorite.title)
                    listener.onImageReady(transitionName)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        holder.itemView.setOnClickListener {
            listener.onItemClick(holder.itemView, favorite, holder.bindingAdapterPosition)
        }

        when (chartOption) {
            PlayCountChartViewModel.LAST_MONTH -> {
                holder.playCountTextView.text =
                    favorite.playCountByDay
                    .filterKeys { it >= LocalDate.now().minusMonths(1) }
                    .values
                    .sum()
                    .toString()
            }
            PlayCountChartViewModel.LAST_3_MONTH -> {
                holder.playCountTextView.text =
                    favorite.playCountByDay
                        .filterKeys { it >= LocalDate.now().minusMonths(3) }
                        .values
                        .sum()
                        .toString()
            }
            PlayCountChartViewModel.LAST_YEAR -> {
                holder.playCountTextView.text =
                    favorite.playCountByDay
                        .filterKeys { it >= LocalDate.now().minusYears(1) }
                        .values
                        .sum()
                        .toString()
            }
            PlayCountChartViewModel.SPECIFIC_YEAR -> {
                val startDate = LocalDate.of(specificYear, 1 ,1)
                val endDate = LocalDate.of(specificYear, 12, 31)
                holder.playCountTextView.text =
                    favorite.playCountByDay
                        .filterKeys { it in  startDate..endDate}
                        .values
                        .sum()
                        .toString()
            }
        }

    }

    fun setSpecificYear(year: Int) {
        specificYear = year
    }


    fun updateData(newList: List<Pair<Favorite, Int>>, chartOption: Int) {
        Log.d(TAG, "update data and size is " + newList.size)
        this.list = newList
        this.chartOption = chartOption
        Log.d(TAG, this.itemCount.toString())
        notifyDataSetChanged()
    }

    class PlayTimeChartViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val artworkImage = view.findViewById<ImageView>(R.id.artwork_image)
        val rankingTextView = view.findViewById<TextView>(R.id.ranking)
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val playCountTextView = view.findViewById<TextView>(R.id.play_count)
    }

    companion object {
        const val TAG = "PlayTimeChartAdapter"
    }
}
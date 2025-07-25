package com.example.mymusic.ui.myCalendar

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.util.DateUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import com.kizitonwose.calendar.core.yearMonth
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Objects

class DayEventAdapter(
    private var events: List<Favorite>,
    private val date: LocalDate,
    private val listener: OnClickListener
) : RecyclerView.Adapter<DayEventAdapter.EventViewHolder>() {


    interface OnClickListener{
        fun onItemClick(holder: EventViewHolder, favorite: Favorite)
        fun onDataReady()
    }
    private lateinit var context: Context
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        context = parent.context
        return EventViewHolder(view)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        var fav: Favorite = events[position]


        holder.itemView.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_ARTWORK_IMAGE + fav.track.trackId
        //Log.d(TAG, "track: ${fav.title} transition name: ${holder.itemView.transitionName}")

        holder.eventText.text = fav.metadata?.title
            ?.takeIf { it.isNotEmpty() }
            ?: fav.title

        holder.eventText.isSelected = true

        val formatted = LocalDate.parse(fav.releaseDate, formatter)

        val period = Period.between(formatted, date)
        var diff = period.years.toString()
        if (diff == "0") {
            diff = "new"
            holder.yearDiff.textSize = 9f
            holder.yearDiff.setTypeface(null, Typeface.BOLD)
        }
        holder.yearDiff.text = diff


        ImageColorAnalyzer.analyzePrimaryColor(
            context,
            fav.track.artworkUrl,
            object : ImageColorAnalyzer.OnPrimaryColorAnalyzedListener {
                override fun onSuccess(
                    dominantColor: Int,
                    primaryColor: Int,
                    selectedColor: Int,
                    unselectedColor: Int
                ) {
                    val darkenColor = MyColorUtils.darkenHslColor(primaryColor, 0.5f)
                    val darkenEnsureWhiteText = MyColorUtils.adjustForWhiteText(darkenColor)
                    holder.eventColor.setBackgroundColor(darkenEnsureWhiteText)
                }

                override fun onFailure() {}
            })

        holder.itemView.setOnClickListener { listener.onItemClick(holder, fav) }

        listener.onDataReady()
    }

    override fun getItemCount(): Int = events.size

    fun submitList(newEvents: List<Favorite>) {
        val oldList = events
        val sortedNew = newEvents.sortedByDescending {
            LocalDate.parse(it.releaseDate, formatter)
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = events.size

            override fun getNewListSize(): Int = sortedNew.size

            override fun areItemsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = sortedNew[newItemPosition]
                return Objects.equals(oldItem.track.trackId, newItem.track.trackId)
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = sortedNew[newItemPosition]
                return Objects.equals(oldItem.track.primaryColor, newItem.track.primaryColor) &&
                        Objects.equals(oldItem.title, newItem.title) &&
                        Objects.equals(oldItem.track.releaseDate, newItem.track.releaseDate)

            }

        })
        events = sortedNew
        diff.dispatchUpdatesTo(this)
    }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventText: TextView = view.findViewById(R.id.calendar_event_text)
        val eventColor: FrameLayout = view.findViewById(R.id.calendar_event_color_primary)
        val yearDiff: TextView = view.findViewById(R.id.year_diff)
    }


    companion object {
        const val TAG = "DayEventAdapter"
    }

}

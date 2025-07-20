package com.example.mymusic.ui.myCalendar

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import com.example.mymusic.util.DateUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class DayEventAdapter(private var events: List<Favorite>, private val date: LocalDate) :
    RecyclerView.Adapter<DayEventAdapter.EventViewHolder>() {

        private lateinit var context: Context
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_event, parent, false)
            context = parent.context
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            var fav: Favorite = events[position]

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


            ImageColorAnalyzer.analyzePrimaryColor(context, fav.track.artworkUrl, object: ImageColorAnalyzer.OnPrimaryColorAnalyzedListener{
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
            } )
        }

        override fun getItemCount(): Int = events.size

        fun submitList(newEvents: List<Favorite>) {
            events = newEvents.sortedByDescending { LocalDate.parse(it.releaseDate, formatter) }
            notifyDataSetChanged()
        }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventText: TextView = view.findViewById(R.id.calendar_event_text)
        val eventColor: FrameLayout = view.findViewById(R.id.calendar_event_color_primary)
        val yearDiff: TextView = view.findViewById(R.id.year_diff)
    }


    }

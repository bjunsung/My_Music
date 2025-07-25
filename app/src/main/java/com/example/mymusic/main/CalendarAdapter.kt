package com.example.mymusic.main

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.request.Tags
import com.example.mymusic.R
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.channels.ticker
import java.time.LocalDate

class CalendarAdapter(private var days: List<ContributionDay>) :
    RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private val colors = listOf(
        Color.parseColor("#EBEDF0"), // 0
        Color.parseColor("#C6E48B"), // 1
        Color.parseColor("#7BC96F"), // 2
        Color.parseColor("#239A3B"), // 3
        Color.parseColor("#196127")  // 4+
    )

    private var itemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(date: LocalDate, playCount: Int, position: Int)
    }

    fun setOnItemClickListener(itemClickListener: OnItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val background: MaterialCardView = view.findViewById(R.id.dayBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_day_for_play_time, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        val colorIndex = day.count.coerceIn(0, 4)
        holder.background.setCardBackgroundColor(colors[colorIndex])
        holder.background.alpha = if (colorIndex == 0) 0.35f else 1f

        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(day.date, day.count, holder.bindingAdapterPosition)
        }
    }


    override fun getItemCount() = days.size



    fun updateData(newDays: List<ContributionDay>) {
        Log.d(TAG, "play time calendar adapter updated")
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = days.size
            override fun getNewListSize() = newDays.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return days[oldItemPosition].date == newDays[newItemPosition].date
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return days[oldItemPosition].count == newDays[newItemPosition].count
            }
        })
        days = newDays.toList() // 새 리스트
        diff.dispatchUpdatesTo(this)
    }

    /*
    fun updateData(newDays: List<ContributionDay>) {
        days = newDays.toList() // 새 리스트
        notifyDataSetChanged()
    }

     */


    companion object {
        const val TAG = "CalendarAdapter"
    }

}

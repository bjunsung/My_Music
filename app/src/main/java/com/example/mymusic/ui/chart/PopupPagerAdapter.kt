package com.example.mymusic.ui.chart

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import com.example.mymusic.util.DateUtils
import java.time.LocalDate

class PopupPagerAdapter (val item: Favorite, val listener: OnClickEventListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    public interface OnClickEventListener{
        fun onTitleClick(item: Favorite)
        fun onAlbumNameClick(item: Favorite)
        fun onArtistNameClick(item: Favorite)
    }

    override  fun getItemCount(): Int = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = when (viewType) {
            0 -> inflater.inflate(R.layout.page_favorite_detail_info, parent, false)
            1 -> inflater.inflate(R.layout.page_favorite_detail_lyrics, parent, false)
            else -> throw IllegalArgumentException("Invalid viewType")
        }
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (position) {
            0 -> { // 상세 정보 페이지
                val view = holder.itemView

                val titleView = view.findViewById<TextView>(R.id.track_title)
                if (item.metadata != null && !item.metadata.title.isNullOrEmpty()) {
                    titleView.text = item.metadata.title
                } else {
                    titleView.text = item.title
                }

                val artistNameTextView = view.findViewById<TextView>(R.id.artist_name)
                artistNameTextView.text = item.artistName

                val albumNameTextView = view.findViewById<TextView>(R.id.album_name)
                albumNameTextView.text = item.track.albumName

                val releaseDateTextView = view.findViewById<TextView>(R.id.release_date)
                releaseDateTextView.text = item.releaseDate

                val durationTextView = view.findViewById<TextView>(R.id.duration_ms)
                val durationSec = (item.duration.toDouble() / 1000).toInt()
                durationTextView.text = "${durationSec / 60}분 ${durationSec % 60}초"

                val daysBetween = view.findViewById<TextView>(R.id.days_between)
                daysBetween.text = DateUtils.calculateDateDiffrence(
                    item.releaseDate,
                    LocalDate.now().toString()
                ).toString()

                val vocalistsLayout = view.findViewById<LinearLayout>(R.id.vocalists_layout)
                val lyricistsLayout = view.findViewById<LinearLayout>(R.id.lyricists_layout)
                val composersLayout = view.findViewById<LinearLayout>(R.id.composers_layout)

                val vocalistsTextView = view.findViewById<TextView>(R.id.vocalists)
                val lyricistsTextView = view.findViewById<TextView>(R.id.lyricists)
                val composersTextView = view.findViewById<TextView>(R.id.composers)

                val addedDateTextView = view.findViewById<TextView>(R.id.added_date)
                addedDateTextView.text = item.addedDate

                if (item.metadata != null) {
                    val metadata = item.metadata
                    if (!metadata.vocalists.isNullOrEmpty()) {
                        vocalistsLayout.visibility = View.VISIBLE
                        vocalistsTextView.text = metadata.vocalistsToString()
                    }
                    if (!metadata.lyricists.isNullOrEmpty()) {
                        lyricistsLayout.visibility = View.VISIBLE
                        lyricistsTextView.text = metadata.lyricists.joinToString(", ")
                    }
                    if (!metadata.composers.isNullOrEmpty()) {
                        composersLayout.visibility = View.VISIBLE
                        composersTextView.text = metadata.composers.joinToString(", ")
                    }
                }

                titleView.setOnClickListener { listener.onTitleClick(item) }
                albumNameTextView.setOnClickListener { listener.onAlbumNameClick(item) }
                artistNameTextView.setOnClickListener { listener.onArtistNameClick(item) }
            }
            1 -> { // 가사 페이지
                val view = holder.itemView
                view.findViewById<TextView>(R.id.lyrics).text =
                    item.metadata?.lyrics ?: "가사 정보가 없습니다."
            }
        }
    }


    override fun getItemViewType(position: Int): Int = position

    companion object{
        const val TAG = "PopupPagerAdapter"
    }
}
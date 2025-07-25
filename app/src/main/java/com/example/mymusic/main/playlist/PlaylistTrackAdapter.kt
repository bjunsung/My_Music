package com.example.mymusic.main.playlist

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.ui.musicInfo.MusicInfoViewModel
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.ImageColorAnalyzer.OnPrimaryColorAnalyzedListener
import com.example.mymusic.util.MyColorUtils
import com.google.android.material.card.MaterialCardView


class PlaylistTrackAdapter(
    private var context: Context?,
    var favoritesList: MutableList<Favorite>,
    private val itemClickListener: OnItemClickListener,
    favoritesColorUnification: Boolean
) : RecyclerView.Adapter<PlaylistTrackAdapter.FavoritesWithCardViewHolder?>() {
    private val TAG = "FavoritesWithCardViewAdapter"
    private val invalidColor = -2
    private var textColor = invalidColor
    private var backgroundColor = invalidColor
    private var favoritesColorUnification = false

    fun setTextColor(textColor: Int) {
        this.textColor = textColor
    }

    private var nowPlayingTrackId: String? = ""


    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onMoreButtonClick(holder: FavoritesWithCardViewHolder, favorite: Favorite)
        fun onDragHandleTouch(holder: RecyclerView.ViewHolder)
    }

    fun setPrimaryBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    init {
        this.favoritesColorUnification = favoritesColorUnification
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesWithCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_with_cardview_for_playlist, parent, false)
        context = parent.context
        return FavoritesWithCardViewHolder(view)
    }

    private fun setBackgroundColor(holder: FavoritesWithCardViewHolder, primaryColor: Int) {
        val darkenColor: Int
        if (DarkModeUtils.isDarkMode(context)) {
            darkenColor = MyColorUtils.darkenHslColor(
                MyColorUtils.ensureContrastWithWhite(primaryColor),
                0.45f
            )
        } else {
            darkenColor = MyColorUtils.darkenHslColor(
                MyColorUtils.ensureContrastWithWhite(primaryColor),
                0.9f
            )
        }
        val adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor)
        val blended = MyColorUtils.blendColors(adjustedForWhiteText, backgroundColor, 0.3141592f)
        holder.containerCardView.setCardBackgroundColor(blended)
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: FavoritesWithCardViewHolder, position: Int) {
        val favorite = favoritesList.get(position)
        favorite.recyclerViewPosition = holder.getAdapterPosition()

        val track = favorite.track
        val trackId = track.trackId
        ViewCompat.setTransitionName(
            holder.image,
            MusicInfoFragment.TRANSITION_NAME_FORM_ARTWORK_IMAGE + trackId
        )
        ViewCompat.setTransitionName(
            holder.title,
            MusicInfoFragment.TRANSITION_NAME_FORM_TITLE + trackId
        )
        ViewCompat.setTransitionName(
            holder.album,
            MusicInfoFragment.TRANSITION_NAME_FORM_ALBUM + trackId
        )
        ViewCompat.setTransitionName(
            holder.artist,
            MusicInfoFragment.TRANSITION_NAME_FORM_ARTIST + trackId
        )
        ViewCompat.setTransitionName(
            holder.durationLayout,
            MusicInfoFragment.TRANSITION_NAME_FORM_DURATION + trackId
        )
        ViewCompat.setTransitionName(
            holder.releaseDateLayout,
            MusicInfoFragment.TRANSITION_NAME_FORM_RELEASE_DATE + trackId
        )




        if (context != null) {
            if (favoritesColorUnification) {
                //holder.containerCardView.setCardBackgroundColor(backgroundColor);
                setBackgroundColor(holder, backgroundColor)
                favorite.backgroundColor = backgroundColor
            } else if (track.primaryColor != null) {
                setBackgroundColor(holder, track.primaryColor)
            } else {
                ImageColorAnalyzer.analyzePrimaryColor(
                    context,
                    track.artworkUrl,
                    object : OnPrimaryColorAnalyzedListener {
                        override fun onSuccess(
                            dominantColor: Int,
                            primaryColor: Int,
                            selectedColor: Int,
                            unselectedColor: Int
                        ) {
                            setBackgroundColor(holder, primaryColor)
                        }

                        override fun onFailure() {
                            Log.d(TAG, "Fail to analyze primary color")
                        }
                    })
            }
        }


        Glide.with(holder.itemView)
            .load(track.artworkUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(holder.image)


        holder.title.text = favorite.title
        holder.title.setTextColor(textColor)

        holder.artist.setText(track.artistName)
        if (textColor != invalidColor) {
            holder.artist.setTextColor(textColor)
        }
        holder.album.setText(track.albumName)
        if (textColor != invalidColor) {
            holder.album.setTextColor(textColor)
        }
        val durationSec = track.durationMs.toDouble().toInt() / 1000
        val durationStr = (durationSec / 60).toString() + "분 " + durationSec % 60 + "초"
        holder.duration.setText(durationStr)
        if (textColor != invalidColor) {
            holder.duration.setTextColor(textColor)
        }
        holder.addedDate.setText(favorite.addedDate)
        if (textColor != invalidColor) {
            holder.addedDate.setTextColor(textColor)
        }
        holder.releasedDate.setText(track.releaseDate)
        if (textColor != invalidColor) {
            holder.releasedDate.setTextColor(textColor)
        }


        if (textColor != invalidColor) {
            holder.textReleaseDate.setTextColor(textColor)
            holder.textDuration.setTextColor(textColor)
            holder.textDash.setTextColor(textColor)
        }

        val isPlayingTrack = favorite.track.trackId == nowPlayingTrackId

        if (!isPlayingTrack) {
            updateAlpha(holder, 0.8f, 0.4f, 350)
        } else {
            updateAlpha(holder, 0.2f, 1f, 700)
        }

        holder.moreButton.setColorFilter(textColor)
        holder.dragHandleButton.setColorFilter(textColor)
        holder.moreButton.setOnClickListener { itemClickListener.onMoreButtonClick(holder, favorite) }
        holder.dragHandleButton.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemClickListener.onDragHandleTouch(holder)
                true
            } else false
        }

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            itemClickListener.onItemClick(
                holder.getBindingAdapterPosition()
            )
        })
    }

    private fun animateAlpha(view: View, from: Float, to: Float, duration: Long) {
        val animator = ValueAnimator.ofFloat(from, to)
        animator.setDuration(duration)
        animator.setInterpolator(DecelerateInterpolator())
        animator.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
            val value = animation!!.getAnimatedValue() as Float
            view.setAlpha(value)
        })
        animator.start()
    }

    private fun updateAlpha(
        holder: FavoritesWithCardViewHolder,
        from: Float,
        to: Float,
        duration: Long
    ) {
        //animateAlpha(holder.containerCardView, from, to, duration);

        animateAlpha(holder.title, from, to, duration)
        animateAlpha(holder.artist, from, to, duration)
        animateAlpha(holder.album, from, to, duration)
        animateAlpha(holder.duration, from, to, duration)
        animateAlpha(holder.releasedDate, from, to, duration)
        animateAlpha(holder.textDuration, from, to, duration)
        animateAlpha(holder.textReleaseDate, from, to, duration)
        animateAlpha(holder.textDash, from, to, duration)
        animateAlpha(holder.image, from, to, duration)
    }

    fun updateColors() {
        notifyItemRangeChanged(0, getItemCount())
    }


    override fun getItemCount(): Int {
        return favoritesList.size
    }

    /**
     drag handle 관련
     */
    fun onItemMove(from: Int, to: Int) {
        if (from == to) return
        if (from !in favoritesList.indices || to !in favoritesList.indices) return
        val moved = favoritesList.removeAt(from)
        favoritesList.add(to, moved)
        notifyItemMoved(from, to)
    }
    fun currentOrder():List<Favorite> = favoritesList.toList()

    fun updateData(newList: MutableList<Favorite>, newNowPlayingTrackId: String?, newColor: Int) {
        val diff = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return favoritesList.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = favoritesList.get(oldItemPosition)
                    val newItem = newList.get(newItemPosition)
                    return oldItem.track.trackId == newItem.track.trackId
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = favoritesList.get(oldItemPosition)
                    val newItem = newList.get(newItemPosition)
                    return oldItem.track.trackId == newItem.track.trackId &&
                            oldItem.track.trackId == this@PlaylistTrackAdapter.nowPlayingTrackId == (newItem.track.trackId == newNowPlayingTrackId) &&
                            this@PlaylistTrackAdapter.backgroundColor == newColor
                }
            }
        )
        this.nowPlayingTrackId = newNowPlayingTrackId
        this.favoritesList = newList
        //this.backgroundColor = newColor;
        //diffResult.dispatchUpdatesTo(this);
        diff.dispatchUpdatesTo(this)
    }

    inner class FavoritesWithCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById<ImageView>(R.id.imageView)
        val title: TextView = itemView.findViewById<TextView>(R.id.titleTextView)
        val artist: TextView = itemView.findViewById<TextView>(R.id.artistTextView)
        val album: TextView = itemView.findViewById<TextView>(R.id.albumTextView)
        val duration: TextView = itemView.findViewById<TextView>(R.id.durationTextView)
        val releasedDate: TextView = itemView.findViewById<TextView>(R.id.releaseDateTextView)
        val addedDate: TextView = itemView.findViewById<TextView>(R.id.addedDateTextView)
        val textDash: TextView = itemView.findViewById<TextView>(R.id.text_dash)
        val textDuration: TextView = itemView.findViewById<TextView>(R.id.text_duration)
        val textReleaseDate: TextView = itemView.findViewById<TextView>(R.id.text_release_date)
        val containerCardView: MaterialCardView = itemView.findViewById<MaterialCardView>(R.id.item_container_card_view)
        val moreButton: ImageButton = itemView.findViewById<ImageButton>(R.id.more_button)
        val dragHandleButton: ImageButton = itemView.findViewById<ImageButton>(R.id.drag_handle)

        val durationLayout: LinearLayout = itemView.findViewById<LinearLayout>(R.id.duration_layout)
        val releaseDateLayout: LinearLayout = itemView.findViewById<LinearLayout>(R.id.release_date_layout)

    }
}

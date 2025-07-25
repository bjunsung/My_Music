package com.example.mymusic.ui.playlist.playlistDetail

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.google.common.base.Objects
import java.util.Locale

class PlaylistDetailAdapter(
    private var trackList: MutableList<Favorite>,
    private var selectedIds: MutableSet<String>,
    private val listener: OnClickListener
    ): RecyclerView.Adapter<PlaylistDetailAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onItemClick(holder: PlaylistDetailAdapter.ViewHolder, favorite: Favorite)
        fun onPlayButtonClick(trackList: List<Favorite>, position: Int)
        fun onMoreButtonClick(anchorView: View ,favorite: Favorite)
        fun onDragHandleButtonClick(viewHolder: RecyclerView.ViewHolder)
        fun onItemSelected(selectedIds: Set<String>)
        fun onImageReady(trackId: String)
    }

    var isEditMode : Boolean = false
    var isEditModeLast: Boolean = false

    fun changeEditMode(isEditMode: Boolean) {
        isEditModeLast = this.isEditMode
        this.isEditMode = isEditMode
    }

    var isSelectionMode: Boolean = false
    var isSelectionModeLast: Boolean = false

    fun changeSelectionMode(isSelectionMode: Boolean) {
        isSelectionModeLast = this.isSelectionMode
        this.isSelectionMode = isSelectionMode
    }

    private lateinit var viewGroupContext: Context
    private var keyword: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        viewGroupContext = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_track, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = trackList.size

    @OptIn(UnstableApi::class)
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = trackList.get(position)
        //bindVisualStateOnly(holder, position)
        holder.itemView.translationX = 0f
        holder.itemView.translationY = 0f
        holder.itemView.elevation = 0f
        holder.itemView.alpha = 1f

        holder.checkBox.setOnCheckedChangeListener(null)
        // 항상 데이터 기반으로 체크 상태 세팅
        holder.checkBox.isChecked = item.track.trackId in selectedIds

        if (isEditMode) {
            holder.dragHandleButton.visibility = View.VISIBLE
            holder.moreButton.visibility = View.GONE
        }
        else {
            holder.dragHandleButton.visibility = View.GONE
            holder.moreButton.visibility = View.VISIBLE
        }
        if (isSelectionMode) {
            holder.checkBox.visibility = View.VISIBLE
        }
        else {
            holder.checkBox.visibility = View.GONE
        }

        val trackId = item.track.trackId

        ViewCompat.setTransitionName(
            holder.artworkImage,
            MusicInfoFragment.TRANSITION_NAME_FORM_ARTWORK_IMAGE + trackId
        )
        ViewCompat.setTransitionName(
            holder.titleTextView,
            MusicInfoFragment.TRANSITION_NAME_FORM_TITLE + trackId
        )
        holder.artistNameTextView.transitionName = MusicInfoFragment.TRANSITION_NAME_FORM_ARTIST + trackId
        ViewCompat.setTransitionName(
            holder.albumNameTextView,
            MusicInfoFragment.TRANSITION_NAME_FORM_ALBUM + trackId
        )
        ViewCompat.setTransitionName(
            holder.durationLayout,
            MusicInfoFragment.TRANSITION_NAME_FORM_DURATION + trackId
        )
        ViewCompat.setTransitionName(
            holder.releaseDateLayout,
            MusicInfoFragment.TRANSITION_NAME_FORM_RELEASE_DATE + trackId
        )

        /*
        Glide.with(holder.itemView)
            .asBitmap()
            .load(item.track.artworkUrl)
            .dontAnimate()
            .error(R.drawable.ic_image_not_found_foreground)
            .override(160, 160)
            .centerCrop()
            .into(object: CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.artworkImage.setImageBitmap(resource)
                    listener.onImageReady(trackId)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    listener.onImageReady(trackId)
                }
            })

         */

        Glide.with(holder.itemView)
            .load(item.track.artworkUrl)
            .override(160, 160)
            .centerCrop()
            .error(R.drawable.ic_image_not_found_foreground)
            .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onImageReady(trackId)
                    return false // 이미지뷰에 기본 에러 드로어블 계속 세팅
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    listener.onImageReady(trackId)
                    return false // 이미지뷰에 리소스 계속 세팅
                }
            })
            .into(holder.artworkImage)


        holder.titleTextView.text = item.title
        holder.albumNameTextView.text = item.track.albumName
        holder.artistNameTextView.text = item.artistName
        holder.durationTextView.text = item.durationStr
        holder.releaseDateTextView.text = item.releaseDate

        keyword?.let {
            highlightText(holder.titleTextView, it)
            highlightText(holder.artistNameTextView, it)
        }

        holder.dragHandleButton.setOnTouchListener{ _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listener.onDragHandleButtonClick(holder)
                true
            } else false
        }

        holder.playButton.setOnClickListener { listener.onPlayButtonClick(trackList, holder.bindingAdapterPosition) }
        holder.moreButton.setOnClickListener { listener.onMoreButtonClick(holder.itemView, item) }
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(item.track.trackId)
                notifyItemChanged(holder.bindingAdapterPosition) // 데이터 변경 후 UI 갱신
                listener.onItemSelected(selectedIds)
            }
            else {
                listener.onItemClick(holder, item)
            }
        }

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(item.track.trackId)
            } else {
                selectedIds.remove(item.track.trackId)
            }
            listener.onItemSelected(selectedIds)
        }


    }

    fun setKeyword(keyword: String?) {
        this.keyword = keyword
        Log.d(TAG, "keyword: $keyword")
        updateList(this.trackList, this.selectedIds)
        trackList.forEach { it.keyword = keyword }
    }

    private fun highlightText(textView: TextView, keyword: String) {
        if (keyword.trim().isEmpty()) return
        val original = textView.text.toString()
        val normalizedTitle = original.replace("\\s+".toRegex(), "").lowercase(Locale.getDefault())
        val normalizedKeyword = keyword.replace("\\s+".toRegex(), "").lowercase(Locale.getDefault())

        if (normalizedTitle.contains(normalizedKeyword)){
            var start = normalizedTitle.indexOf(normalizedKeyword)
            var end = start + normalizedKeyword.length - 1
            val cleaned = original.replace('\u00A0', ' ')
            Log.d(TAG, "textview: ${textView.text} contains keyword: $keyword  start: $start end: $end")
            for (i in 0..start) {
                if (cleaned.get(i) == ' ') {
                    start++
                }
            }
            for (i in 0..end) {
                if (cleaned[i] == ' ') {
                    end++
                }
            }

            val spannable = SpannableString(original)
            spannable.setSpan(
                BackgroundColorSpan("#4682b4".toColorInt()),
                start,
                end + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(Color.WHITE),
                start,
                end + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textView.text = spannable
            return
        }
        textView.text = original
    }

    /**
     * drag handle 관련
     *
     */

    // 단일 이동만 지원
    fun onItemMove(from: Int, to: Int) {
        if (from == to) return
        if (from !in trackList.indices || to !in trackList.indices) return
        val moved = trackList.removeAt(from)
        trackList.add(to, moved)
        notifyItemMoved(from, to)
        //updateList(this.trackList, selectedIds)
    }

    // 필요 시 외부에서 현재 순서 확인
    fun currentOrder(): List<Favorite> = trackList.toList()



    fun toggleSelection(clickedId: String) {
        if (clickedId in selectedIds) {
            selectedIds.remove(clickedId)
        }
        else {
            selectedIds.add(clickedId)
        }
    }

    fun updateSelectedIdSet(newIds: Set<String>) {
        if (selectedIds == newIds) return
        selectedIds = newIds.toMutableSet()
    }


    fun updateList(
        newList: List<Favorite>,
        newSelectedIds: Set<String>
    ) {
        val oldSelected = this.selectedIds.toSet()
        val newSelected = newSelectedIds.toSet()
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int = trackList.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = trackList.get(oldItemPosition)
                val newItem = newList.get(newItemPosition)
                return oldItem.track.trackId == newItem.track.trackId

            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = trackList.get(oldItemPosition)
                val newItem = newList.get(newItemPosition)
                val oldItemId = oldItem.track.trackId
                val newItemId = newItem.track.trackId
                return Objects.equal(oldItemId, newItemId) &&
                        Objects.equal(oldItemId in oldSelected, newItemId in newSelected)
                        && isEditMode == isEditModeLast
                        && isSelectionMode == isSelectionModeLast
                        && oldItem.keyword == keyword

            }
        })

        trackList = newList.toMutableList()
        diff.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
        val artworkImage = view.findViewById<ImageView>(R.id.artwork_image)
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val albumNameTextView = view.findViewById<TextView>(R.id.album_name)
        val artistNameTextView = view.findViewById<TextView>(R.id.artist_name)
        val durationTextView = view.findViewById<TextView>(R.id.duration)
        val releaseDateTextView = view.findViewById<TextView>(R.id.release_date)
        val playButton = view.findViewById<ImageButton>(R.id.music_play)
        val moreButton = view.findViewById<ImageButton>(R.id.more_button)
        val dragHandleButton = view.findViewById<ImageButton>(R.id.drag_handle)

        val durationLayout = view.findViewById<LinearLayout>(R.id.duration_layout)
        val releaseDateLayout = view.findViewById<LinearLayout>(R.id.release_date_layout)
    }

    companion object {
        const val TAG = "PlaylistDetailAdapter"
    }

}
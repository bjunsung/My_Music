package com.example.mymusic.ui.playlist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.support.annotation.DrawableRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.R
import com.example.mymusic.model.Playlist
import com.google.common.base.Objects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.ListUpdateCallback
import com.example.mymusic.util.ImageCollageUtil
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import kotlin.math.roundToInt


class PlaylistLibraryAdapter (
    private var playlists: List<Playlist>,
    private val viewModelScope: CoroutineScope,
    private val listener: OnClickListener
    ) : RecyclerView.Adapter<PlaylistLibraryAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onItemClick(holder:
                        PlaylistLibraryAdapter.ViewHolder, playlist: Playlist)
        fun onPlayButtonClick(playlist: Playlist)
        fun onShuffleButtonClick(playlist: Playlist)
        fun onMenuClick(anchorView: View, playlist: Playlist)
        fun onAddNewPlaylist()
        fun onImageReady(playlistId: String)
    }


    private lateinit var viewGroupContext: Context
    private val jobs = mutableMapOf<RecyclerView.ViewHolder, Job>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        viewGroupContext = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = playlists.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_DUMMY else VIEW_TYPE_NORMAL
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_DUMMY) bindDummy(holder)
        else bindNormal(holder, position)
    }

    private fun bindDummy(holder: ViewHolder) {
        holder.playButton.visibility = View.GONE
        holder.shuffleButton.visibility = View.GONE
        holder.hamburgerButton.visibility = View.GONE
        holder.playlistCountTextVIew.visibility = View.GONE
        holder.playlistPlayTimeTextView.visibility = View.GONE
        holder.playlistNameTextView.text = "플레이리스트 추가하기"
        val dummyBitmap = rasterizeDrawableCentered(viewGroupContext, R.drawable.ic_round_playlist_add, 150, 150)
        holder.combinedArtworks.setImageBitmap(dummyBitmap)
        holder.itemView.setOnClickListener { listener.onAddNewPlaylist() }
    }
    private fun bindNormal(holder: ViewHolder, position: Int) {
        val playlist = playlists.get(holder.bindingAdapterPosition - 1)
        if (playlist.trackIds.isEmpty()) {
            holder.shuffleButton.alpha = 0.3f
            holder.playButton.alpha = 0.3f
        }

        holder.playlistNameTextView.text = playlist.playlistName
        holder.playlistCountTextVIew.text = "${playlist.trackIds.size} 곡"
        holder.playlistPlayTimeTextView.text = playlist.getDurationStr()

        val playlistId = playlist.playlistId
        holder.combinedArtworks.transitionName = "combined_artworks_${playlistId}"
        holder.playlistNameTextView.transitionName = "name_${playlistId}"
        holder.playlistCountTextVIew.transitionName = "count_${playlistId}"
        holder.playlistPlayTimeTextView.transitionName = "duration_${playlistId}"

        jobs[holder]?.cancel()
        holder.combinedArtworks.setImageResource(R.drawable.ic_round_playlist_play)
        val urls: List<String> = playlist.getUrls().take(4)
        if (urls.isEmpty()) listener.onImageReady(playlistId = playlist.playlistId)

        val job = viewModelScope.launch {
            if (urls.isEmpty()) return@launch
            val bmp = ImageCollageUtil.make2x2(
                holder.itemView.context,
                urls,
                180,
                R.drawable.ic_round_playlist_play
            )
            withContext(Dispatchers.Main) {
                // 뷰가 재활용되지 않았는지 방어
                if (holder.bindingAdapterPosition == position) {
                    // 한 번 더 방어
                    holder.combinedArtworks.imageTintList = null
                    holder.combinedArtworks.colorFilter = null
                    holder.combinedArtworks.setImageBitmap(bmp)
                    listener.onImageReady(playlistId = playlist.playlistId)
                }
            }
        }
        jobs[holder] = job


        holder.itemView.setOnClickListener { listener.onItemClick(holder, playlist) }
        holder.playButton.setOnClickListener { listener.onPlayButtonClick(playlist) }
        holder.shuffleButton.setOnClickListener { listener.onShuffleButtonClick(playlist) }
        holder.hamburgerButton.setOnClickListener { listener.onMenuClick(holder.itemView, playlist) }
    }



    fun updateList(newList: List<Playlist>) {
        val oldList = playlists

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return Objects.equal(oldItem.playlistId, newItem.playlistId)
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return Objects.equal(oldItem.playlistId, newItem.playlistId) &&
                        Objects.equal(oldItem.playlistName, newItem.playlistName) &&
                        Objects.equal(oldItem.lastPlayedTimeMs, newItem.lastPlayedTimeMs) &&
                        Objects.equal(oldItem.playCount, newItem.playCount) &&
                        Objects.equal(oldItem.trackIds.size, newItem.trackIds.size) &&
                        Objects.equal(oldItem.totalDurationSec, newItem.totalDurationSec)
            }
        })

        playlists = newList

        // position +1 보정용 콜백
        val offsetCallback = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position + 1, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position + 1, count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition + 1, toPosition + 1)
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                notifyItemRangeChanged(position + 1, count, payload)
            }
        }

        diff.dispatchUpdatesTo(offsetCallback)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val combinedArtworks = view.findViewById<ImageView>(R.id.combined_artworks)
        val playlistNameTextView = view.findViewById<TextView>(R.id.playlist_name)
        val playlistCountTextVIew = view.findViewById<TextView>(R.id.track_count_text)
        val playButton = view.findViewById<ImageButton>(R.id.playlist_play_button)
        val shuffleButton = view.findViewById<ImageButton>(R.id.playlist_shuffle_button)
        val hamburgerButton = view.findViewById<ImageButton>(R.id.hamburger_button)
        val playlistPlayTimeTextView = view.findViewById<TextView>(R.id.playlist_play_time_text)
    }


    companion object {
        private const val VIEW_TYPE_DUMMY = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

}


/*
suspend fun combineFourImagesFromUrls(
    context: Context,
    urls: List<String?>,
    size: Int,
    placeholderResId: Int
): Bitmap = withContext(Dispatchers.IO) {
    val half = size / 2
    val slots = (0 until 4).map { urls.getOrNull(it) }

    fun loadBitmap(any: Any?): Bitmap =
        try {
            Glide.with(context)
                .asBitmap()
                .load(any)
                .submit() // 원본 비트맵
                .get()
        } catch (e: Exception) {
            rasterizeDrawable(context, placeholderResId, size, size)
        }

    val bitmaps = slots.map { url ->
        val bmp = if (url.isNullOrBlank()) {
            rasterizeDrawable(context, placeholderResId, size, size)
        } else {
            loadBitmap(url)
        }
        centerCropBitmap(bmp, half, half) // 🔹 여기서 강제 CenterCrop
    }

    val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawBitmap(bitmaps[0], 0f, 0f, paint)
    canvas.drawBitmap(bitmaps[1], half.toFloat(), 0f, paint)
    canvas.drawBitmap(bitmaps[2], 0f, half.toFloat(), paint)
    canvas.drawBitmap(bitmaps[3], half.toFloat(), half.toFloat(), paint)

    result
}

 */


fun rasterizeDrawableCentered(
    context: Context,
    @DrawableRes resId: Int,
    width: Int,
    height: Int,
    scale: Float = 0.8f,
    @ColorInt tint: Int? = null,              // null이면 기본 어두운 회색을 자동 적용
    @ColorInt background: Int = 0xFF121212.toInt()
): Bitmap {

    val d = AppCompatResources.getDrawable(context, R.drawable.ic_round_playlist_add)?.mutate()
        ?: return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    // ❗ 호출자가 tint를 안 주면(=null) 기본 어두운 회색을 적용
    val effectiveTint = tint ?: defaultDarkIconColor(context)
    DrawableCompat.setTint(d, effectiveTint)

    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(background)

    val iw = if (d.intrinsicWidth > 0) d.intrinsicWidth else width
    val ih = if (d.intrinsicHeight > 0) d.intrinsicHeight else height
    val availW = (width * scale).roundToInt()
    val availH = (height * scale).roundToInt()
    val factor = Math.min(availW.toFloat() / iw, availH.toFloat() / ih)

    val w = (iw * factor).roundToInt()
    val h = (ih * factor).roundToInt()
    val left = (width - w) / 2
    val top = (height - h) / 2

    d.setBounds(left, top, left + w, top + h)
    d.draw(canvas)


// ❗ 아이콘을 흰색으로 강제
    DrawableCompat.setTintMode(d, PorterDuff.Mode.SRC_IN)
    DrawableCompat.setTint(d, Color.WHITE)

// 배경은 원하시는 대로 (예: 더 어두운 회색)

// canvas.drawColor(Color.parseColor("#121212")) // 선택
// 크기/중앙 정렬 계산했다면 setBounds(...) 후 draw
    d.setBounds(left, top, left + w, top + h)
    d.draw(canvas)
    return bmp
}



private fun defaultDarkIconColor(context: Context): Int {
    // 머티리얼 테마의 표준 "어두운 아이콘" 계열
    return MaterialColors.getColor(
        context,
        com.google.android.material.R.attr.colorOnSurfaceVariant,
        0xFF616161.toInt() // 테마에 없을 때 fallback: #616161
    )
}

private fun centerCropBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    val scale: Float = maxOf(
        targetWidth.toFloat() / src.width,
        targetHeight.toFloat() / src.height
    )

    val scaledWidth = (src.width * scale).toInt()
    val scaledHeight = (src.height * scale).toInt()

    val scaledBitmap = Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true)

    val xOffset = (scaledWidth - targetWidth) / 2
    val yOffset = (scaledHeight - targetHeight) / 2

    return Bitmap.createBitmap(scaledBitmap, xOffset, yOffset, targetWidth, targetHeight)
}



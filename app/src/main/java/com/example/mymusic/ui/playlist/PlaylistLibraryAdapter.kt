package com.example.mymusic.ui.playlist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.ListUpdateCallback
import com.example.mymusic.util.ImageCollageUtil
import com.google.android.material.card.MaterialCardView

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
        val dummyBitmap = rasterizeDrawableNoTint(viewGroupContext, R.drawable.ic_round_playlist_add, 150, 150)
        holder.artwork_images.setImageBitmap(dummyBitmap)
        holder.itemView.setOnClickListener { listener.onAddNewPlaylist() }
    }
    private fun bindNormal(holder: ViewHolder, position: Int) {
        val playlist = playlists.get(holder.bindingAdapterPosition - 1)
        holder.playlistNameTextView.text = playlist.playlistName
        holder.playlistCountTextVIew.text = "${playlist.trackIds.size} 곡"
        holder.playlistPlayTimeTextView.text = playlist.getDurationStr()

        val id = playlist.playlistId
        holder.artworkImageCardHolder.transitionName = "artworks_${id}"
        holder.playlistNameTextView.transitionName = "name_${id}"
        holder.playlistCountTextVIew.transitionName = "count_${id}"
        holder.playlistPlayTimeTextView.transitionName = "duration_${id}"

        jobs[holder]?.cancel()
        holder.artwork_images.setImageResource(R.drawable.ic_round_playlist_play)
        val urls: List<String> = playlist.getUrls().take(4)

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
                    holder.artwork_images.imageTintList = null
                    holder.artwork_images.colorFilter = null
                    holder.artwork_images.setImageBitmap(bmp)
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
        val artwork_images = view.findViewById<ImageView>(R.id.artwork_images)
        val playlistNameTextView = view.findViewById<TextView>(R.id.playlist_name)
        val playlistCountTextVIew = view.findViewById<TextView>(R.id.track_count_text)
        val playButton = view.findViewById<ImageButton>(R.id.playlist_play_button)
        val shuffleButton = view.findViewById<ImageButton>(R.id.playlist_shuffle_button)
        val hamburgerButton = view.findViewById<ImageButton>(R.id.hamburger_button)
        val playlistPlayTimeTextView = view.findViewById<TextView>(R.id.playlist_play_time_text)
        val artworkImageCardHolder = view.findViewById<MaterialCardView>(R.id.combined_artworks_card)
    }


    companion object {
        private const val VIEW_TYPE_DUMMY = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

}



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
            rasterizeDrawableNoTint(context, placeholderResId, size, size)
        }

    val bitmaps = slots.map { url ->
        val bmp = if (url.isNullOrBlank()) {
            rasterizeDrawableNoTint(context, placeholderResId, size, size)
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


private fun rasterizeDrawableNoTint(
    context: Context,
    @androidx.annotation.DrawableRes resId: Int,
    width: Int,
    height: Int
): Bitmap {
    // 테마 tint 영향 제거를 위해 theme=null 로 로드
    val raw = androidx.core.content.res.ResourcesCompat.getDrawable(
        context.resources, resId, /* theme = */ null
    ) ?: return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // wrap + mutate 로 안전하게 조작
    val drawable = androidx.core.graphics.drawable.DrawableCompat.wrap(raw).mutate()

    // 🔴 모든 tint/필터 제거
    androidx.core.graphics.drawable.DrawableCompat.setTintList(drawable, null)
    androidx.core.graphics.drawable.DrawableCompat.setTintMode(drawable, null)
    drawable.colorFilter = null

    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bmp
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



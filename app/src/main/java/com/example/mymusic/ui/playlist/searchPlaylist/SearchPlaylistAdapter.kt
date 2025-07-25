package com.example.mymusic.ui.playlist.searchPlaylist

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.InspectableProperty
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.R
import com.example.mymusic.model.Favorite
import java.util.Locale

class SearchPlaylistAdapter(
    private var favoriteList: List<Favorite>,
    private var alreadyExistSet: Set<String>,
    private var selectedIds: Set<String>,                 // ✅ Set 기반 선택상태
    private val listener: OnClickListener
) : RecyclerView.Adapter<SearchPlaylistAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onPlayButtonClick(favorite: Favorite, position: Int)
        /** checked: 사용자가 최종적으로 원하는 상태 (true=선택, false=해제) */
        fun onToggleSelection(favorite: Favorite, checked: Boolean)
    }

    private lateinit var viewGroupContext: Context
    private var keyword: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        viewGroupContext = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = favoriteList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_EXIST_CHANGED)) {
            // 🔹 exist 상태만 갱신
            val trackId = favoriteList[position].track.trackId
            val alreadyExist = alreadyExistSet.contains(trackId)

            // 알파값/활성 상태만 변경
            holder.itemView.alpha = if (alreadyExist) 0.4f else 1f
            holder.selectionCheckBox.isEnabled = !alreadyExist
            return
        }
        else if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_KEYWORD_CHANGED)) {
            keyword?.let {
                highlightText(holder.titleTextView, it)
                highlightText(holder.artistNameTextView, it)
            }
        }


        // payload 없으면 풀 바인딩 실행
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = favoriteList[position]
        val trackId = item.track.trackId

        // ✅ 재활용 대비: 리스너 끊고 상태 세팅 → 리스너 재연결
        holder.selectionCheckBox.setOnCheckedChangeListener(null)

        // ✅ 체크 상태는 오직 Set 기준
        val isCheckedNow = selectedIds.contains(trackId)
        holder.selectionCheckBox.isChecked = isCheckedNow

        val alreadyExist = alreadyExistSet.contains(trackId)
        if (alreadyExist) holder.itemView.alpha = 0.4f else holder.itemView.alpha = 1f
        // (선택 불가 처리 필요하면) 이미 플레이리스트에 있는 항목 비활성화 예시
        holder.selectionCheckBox.isEnabled = !alreadyExist

        holder.selectionCheckBox.setOnCheckedChangeListener { _, checked ->
            if (!alreadyExist)
                listener.onToggleSelection(item, checked)
        }
        holder.itemView.setOnClickListener {
            // 체크박스 토글 → 리스너에서 VM에 반영
            if (!alreadyExist)
                holder.selectionCheckBox.toggle()
        }

        // 썸네일/텍스트 바인딩
        Glide.with(viewGroupContext)
            .load(item.track.artworkUrl)
            .error(R.drawable.ic_image_not_found_foreground)
            .override(160, 160)
            .into(holder.artworkImage)

        holder.titleTextView.text = item.title
        holder.albumNameTextView.text = item.track.albumName
        holder.artistNameTextView.text = item.artistName
        holder.durationTextView.text = item.durationStr
        holder.releaseDateTextViwe.text = item.releaseDate

        keyword?.let {
            highlightText(holder.titleTextView, it)
            highlightText(holder.artistNameTextView, it)
        }

        holder.playButton.setOnClickListener { listener.onPlayButtonClick(item, holder.bindingAdapterPosition) }
    }

    fun setKeyword(keyword: String?) {
        this.keyword = keyword
        Log.d(TAG, "keyword: $keyword")
        for (i in 0 until favoriteList.size) {
            val item = favoriteList[i]
            notifyItemChanged(i, PAYLOAD_KEYWORD_CHANGED)
            item.keyword = keyword
        }

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
     * 리스트/세트가 바뀔 때 호출. (VM에서 selectedIds/related/alreadyExist 변동 후)
     */
    fun updateList(
        newList: List<Favorite>,
        newAlreadyExistSet: Set<String>,
        newSelectedIds: Set<String>
    ) {
        // 1) 반드시 스냅샷(복사본) 준비
        val oldList = this.favoriteList
        val oldExist = this.alreadyExistSet.toSet()
        val oldSelected = this.selectedIds.toSet()       // 🔴 스냅샷
        val newExist = newAlreadyExistSet.toSet()
        val newSelected = newSelectedIds.toSet()        // 🔴 스냅샷

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(o: Int, n: Int): Boolean {
                return oldList[o].track.trackId == newList[n].track.trackId
            }

            override fun areContentsTheSame(o: Int, n: Int): Boolean {
                val oldId = oldList[o].track.trackId
                val newId = newList[n].track.trackId

                val oldChecked = oldSelected.contains(oldId)   // 스냅샷 사용
                val newChecked = newSelected.contains(newId)   // 스냅샷 사용
                val oldHas = oldExist.contains(oldId)
                val newHas = newExist.contains(newId)

                return (oldChecked == newChecked) &&
                        (oldHas == newHas)
                        && oldList[o].keyword == keyword
            }
        })

        // 2) 필드 갱신은 diff 계산 후
        this.favoriteList = newList
        this.alreadyExistSet = newExist
        this.selectedIds = newSelected

        diff.dispatchUpdatesTo(this)
    }

    fun notifyExistChangedByIds(newAlreadyExistSet: Set<String>) {
        //todo
        this.alreadyExistSet = newAlreadyExistSet.toSet()
        notifyDataSetChanged()

        /*
        for (trackId: String in newAlreadyExistSet) {
            val pos = favoriteList.indexOfFirst { it.track.trackId == trackId }
            if (pos != -1) notifyItemChanged(pos, PAYLOAD_EXIST_CHANGED)
        }
         */




    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectionCheckBox: CheckBox = view.findViewById(R.id.selection_checkbox)
        val artworkImage: ImageView = view.findViewById(R.id.artwork_image)
        val titleTextView: TextView = view.findViewById(R.id.title)
        val albumNameTextView: TextView = view.findViewById(R.id.album_name)
        val artistNameTextView: TextView = view.findViewById(R.id.artist_name)
        val durationTextView: TextView = view.findViewById(R.id.duration)
        val releaseDateTextViwe: TextView = view.findViewById(R.id.release_date)
        val playButton: ImageButton = view.findViewById(R.id.play_button)
    }

    companion object {
        private const val PAYLOAD_EXIST_CHANGED = "exist_state"
        private const val PAYLOAD_KEYWORD_CHANGED = "keyword"
        const val TAG = "SearchPlaylistAdapter"
    }
}

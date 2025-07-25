package com.example.mymusic.ui.playlist.playlistDetail

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DragReorderCallback(
    private val adapter: PlaylistDetailAdapter,
    private val viewModel: PlaylistDetailViewModel, // repo 호출용
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
        adapter.onItemMove(from.bindingAdapterPosition, to.bindingAdapterPosition) // 어댑터 내부 리스트 변경
        return true
    }

    override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
        super.clearView(rv, vh)
        // 드래그가 끝난 시점에 한 번만 저장
        val trackIds = adapter.currentOrder().map { it.track.trackId }
        viewModel.playlist.value?.let { viewModel.saveOrder(it.playlistId, trackIds) }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
    override fun isLongPressDragEnabled(): Boolean = false
}

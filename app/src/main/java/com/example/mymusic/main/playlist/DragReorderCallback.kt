package com.example.mymusic.main.playlist

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragReorderCallback(
    private val onMoveInAdapter: (from: Int, to: Int) -> Unit,
    private val readCurrentIds: () -> List<String>,
    private val onCommit: (List<String>) -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0){


    override fun onMove(
        rv: RecyclerView,
        from: RecyclerView.ViewHolder,
        to: RecyclerView.ViewHolder
    ): Boolean {
        val fromPos = from.bindingAdapterPosition
        val toPos = to.bindingAdapterPosition
        if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION || fromPos == toPos) return false
        onMoveInAdapter(fromPos, toPos)
        return true
    }
    override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
        super.clearView(rv, vh)
        val ids = readCurrentIds()       // 현 리스트 스냅샷(id 리스트)
        onCommit(ids)                    // 화면/세션별로 주입한 저장 로직 호출
    }
    override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}
    override fun isLongPressDragEnabled(): Boolean = false
}
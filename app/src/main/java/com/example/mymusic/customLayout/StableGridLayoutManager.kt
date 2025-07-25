package com.example.mymusic.customLayout

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StableGridLayoutManager(
    context: Context,
    spanCount: Int,
    orientation: Int,
    reverseLayout: Boolean
) : GridLayoutManager(context, spanCount, orientation, reverseLayout) {

    private var pendingScrollPosition = RecyclerView.NO_POSITION
    private var pendingOffset = 0

    fun saveScrollState(recyclerView: RecyclerView) {
        val firstView = recyclerView.getChildAt(0)
        pendingScrollPosition = findFirstVisibleItemPosition()
        pendingOffset = firstView?.let { getDecoratedTop(it) - paddingTop } ?: 0
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        if (pendingScrollPosition != RecyclerView.NO_POSITION) {
            scrollToPositionWithOffset(pendingScrollPosition, pendingOffset)
            pendingScrollPosition = RecyclerView.NO_POSITION
        }
    }
}

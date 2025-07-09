package com.example.mymusic.util;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpaceHeight;

    public VerticalSpaceItemDecoration(int verticalSpaceHeight) {
        this.verticalSpaceHeight = verticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter() != null ? parent.getAdapter().getItemCount() : 0;

        // 마지막 아이템이면 간격 없음 또는 양수로 보정
        if (position == itemCount - 1) {
            outRect.bottom = 0; // 또는 Math.max(0, spacingPx)
        } else {
            outRect.bottom = verticalSpaceHeight; // 음수 가능
        }
    }
}

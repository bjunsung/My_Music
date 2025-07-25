// src/main/java/com/example/mymusic/util/ViewPager2Ktx.java
package com.example.mymusic.extension;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import java.util.WeakHashMap;

public final class ViewPager2Animator {

    private ViewPager2Animator() {}

    // 같은 pager에 이미 도는 애니메이터가 있으면 중복 방지용으로 취소
    private static final WeakHashMap<ViewPager2, Animator> RUNNING = new WeakHashMap<>();

    private static void cancelRunning(@NonNull ViewPager2 pager) {
        Animator a = RUNNING.remove(pager);
        if (a != null) a.cancel();
    }

    /** Kotlin 확장함수 setCurrentItemWithDuration의 Java 버전 (동일 동작) */
    public static void setCurrentItemWithDuration(
            @NonNull ViewPager2 pager,
            int targetItem,
            long durationMs
    ) {
        if (durationMs <= 0L || targetItem == pager.getCurrentItem()) {
            pager.setCurrentItem(targetItem, true);
            return;
        }

        int pagesToMove = targetItem - pager.getCurrentItem();
        if (pagesToMove == 0) return;

        // ✅ 코틀린보다 안정적인 결과: padding 제외한 "콘텐츠 폭" 사용
        int contentWidth = Math.max(
                1,
                pager.getWidth() - pager.getPaddingLeft() - pager.getPaddingRight()
        );
        float totalDrag = contentWidth * pagesToMove;

        // 중복 애니메이션 방지
        cancelRunning(pager);

        if (!pager.beginFakeDrag()) {
            pager.setCurrentItem(targetItem, true);
            return;
        }

        final float[] previousDrag = new float[]{0f};

        ValueAnimator animator = ValueAnimator.ofFloat(0f, totalDrag);
        RUNNING.put(pager, animator);

        animator.setDuration(durationMs);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            float dragOffset = (float) a.getAnimatedValue() - previousDrag[0];
            // 음수 = 오른쪽→왼쪽, 양수 = 왼쪽→오른쪽
            try {
                pager.fakeDragBy(-dragOffset);
            } catch (Throwable ignore) {
                // 드문 케이스 보호
            }
            previousDrag[0] += dragOffset;
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                try { pager.endFakeDrag(); } catch (Throwable ignored) {}
                RUNNING.remove(pager);
                // 드래그 부동 오차로 페이지가 안 맞는 상황에 대비해 스냅 보정 (옵션)
                // pager.setCurrentItem(targetItem, false);
            }
            @Override public void onAnimationCancel(Animator animation) {
                try { pager.endFakeDrag(); } catch (Throwable ignored) {}
                RUNNING.remove(pager);
            }
        });
        animator.start();
    }
}

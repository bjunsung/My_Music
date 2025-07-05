package com.example.mymusic.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

public class EdgeSwipeBackGestureHelper {

    private boolean shouldNavigateBack = false;
    private boolean isEdgeSwipe = false;
    private boolean isEdgeSwiping = false;
    /**
     * ✅ 기존 애니메이션 포함 버전
     */
    @SuppressLint("ClickableViewAccessibility")
    public void attachToViewWithMotion(View gestureOverlay, View swipeContent, Fragment fragment) {
        Context context = fragment.requireContext();

        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                isEdgeSwipe = e != null && e.getX() < screenWidth / 30f;
                return isEdgeSwipe;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!isEdgeSwipe || e1 == null || e2 == null) return false;

                float deltaX = e2.getX() - e1.getX();
                long duration = e2.getEventTime() - e1.getEventTime();
                float velocity = deltaX / (duration + 1f); // ZeroDivision 방지

                if (deltaX <= 0) return false;

                swipeContent.setTranslationX(deltaX);
                shouldNavigateBack = deltaX > 150 && velocity > 1.35f;
                return true;
            }
        });

        gestureOverlay.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (isEdgeSwipe) {
                    if (shouldNavigateBack) {
                        swipeContent.animate()
                                .translationX(swipeContent.getWidth())
                                .setDuration(200)
                                .withEndAction(() -> fragment.requireActivity()
                                        .getOnBackPressedDispatcher()
                                        .onBackPressed())
                                .start();
                    } else {
                        swipeContent.animate()
                                .translationX(0f)
                                .setDuration(200)
                                .start();
                    }
                }

                isEdgeSwipe = false;
                shouldNavigateBack = false;
            }

            return isEdgeSwipe;
        });
    }
}



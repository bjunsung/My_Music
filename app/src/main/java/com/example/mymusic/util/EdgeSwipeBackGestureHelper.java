package com.example.mymusic.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

public class EdgeSwipeBackGestureHelper {

    private boolean shouldNavigateBack = false;
    private boolean isEdgeSwipe = false;

    /**
     * ✅ 기존 애니메이션 포함 버전
     */
    @SuppressLint("ClickableViewAccessibility")
    public void attachToView(View gestureOverlay, View swipeContent, Fragment fragment) {
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

    /**
     * ✅ 애니메이션 없는 간단한 뒤로가기 버전
     */
    @SuppressLint("ClickableViewAccessibility")
    public void attachToView(View gestureOverlay, Fragment fragment) {
        Context context = fragment.requireContext();

        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                return e != null && e.getX() < screenWidth / 30f;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null) return false;

                float deltaX = e2.getX() - e1.getX();
                long duration = e2.getEventTime() - e1.getEventTime();
                float velocity = deltaX / (duration + 1f);

                if (deltaX > 150 && velocity > 1.0f) {
                    fragment.requireActivity()
                            .getOnBackPressedDispatcher()
                            .onBackPressed();
                    return true;
                }

                return false;
            }
        });

        gestureOverlay.setOnTouchListener((v, event) -> {
            boolean gestureDetected = gestureDetector.onTouchEvent(event);

            // DOWN일 때만 제스처 시작 감지
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gestureDetected = gestureDetector.onTouchEvent(event);
            }

            // 스와이프 제스처가 감지되었을 때만 터치 이벤트 소비
            if (gestureDetected) return true;

            // 그 외는 false로 반환해서 아래 뷰들이 터치 받을 수 있게 함
            return false;
        });

    }
}

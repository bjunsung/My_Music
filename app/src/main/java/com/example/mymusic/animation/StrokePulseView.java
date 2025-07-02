package com.example.mymusic.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

public class StrokePulseView extends View {

    private Paint paint;
    private float radius;
    private float centerX, centerY;
    private AnimatorSet animatorSet;

    public StrokePulseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE); // 선으로 그리기 모드
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(15f); // 선 두께
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 설정된 투명도와 반지름으로 원을 그림
        canvas.drawCircle(centerX, centerY, radius, paint);

    }

    public void startPulse(float x, float y) {
        this.centerX = x;
        this.centerY = y;

        // 혹시 이전 애니메이션이 실행 중이면 취소
        if (animatorSet != null && animatorSet.isRunning()) {
            animatorSet.cancel();
        }

        // 1. 반지름을 0에서 500까지 키우는 애니메이션
        ObjectAnimator radiusAnimator = ObjectAnimator.ofFloat(this, "radius", 0f, 250f);

        // 2. 투명도를 100%에서 0%로 바꾸는 애니메이션 (fade out)
        ObjectAnimator alphaAnimator = ObjectAnimator.ofInt(paint, "alpha", 255, 0);

        // 두 애니메이션을 함께 실행
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(radiusAnimator, alphaAnimator);
        animatorSet.setDuration(500); // 0.4초 동안 실행
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 애니메이션이 끝나면 반지름과 투명도를 초기화
                setRadius(0);
                paint.setAlpha(255);
            }
        });
        animatorSet.start();
    }

    // ObjectAnimator가 호출할 세터(setter) 메서드
    public void setRadius(float radius) {
        this.radius = radius;
        invalidate(); // 뷰를 다시 그리도록 요청
    }
}
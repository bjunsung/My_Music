package com.example.mymusic.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class StrokeTextView extends androidx.appcompat.widget.AppCompatTextView {
    private boolean isDrawing = false;

    public StrokeTextView(Context context) {
        super(context);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isDrawing) {
            isDrawing = true;
            // 1. Draw outline
            setTextColor(Color.WHITE); // 테두리 색
            getPaint().setStyle(Paint.Style.STROKE);
            getPaint().setStrokeWidth(2); // 두께
            super.onDraw(canvas);

            // 2. Draw fill
            setTextColor(Color.BLACK); // 텍스트 본 색
            getPaint().setStyle(Paint.Style.FILL);
            super.onDraw(canvas);
            isDrawing = false;
        }
    }
}

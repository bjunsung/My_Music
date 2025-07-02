package com.example.mymusic.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;


import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import com.example.mymusic.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class ImageColorAnalyzer {

    // 결과를 비동기적으로 반환받기 위한 콜백 인터페이스
    public interface OnColorAnalyzedListener {
        void onSuccess(int dominantColor, boolean isLight);
        void onFailure();
    }

    public interface OnPrimaryColorAnalyzedListener{
        void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor);
        void onFailure();
    }

    // 생성자를 private으로 막아서 객체 생성을 방지 (유틸리티 클래스)
    private ImageColorAnalyzer() {}

    /**
     * 이미지 URL의 우측 하단 영역의 주요 색상을 분석합니다.
     * @param context Context 객체
     * @param imageUrl 분석할 이미지의 URL
     * @param listener 분석 결과를 전달받을 리스너
     */
    public static void analyzeBottomRightColor(Context context, String imageUrl, @NonNull OnColorAnalyzedListener listener) {
        // Picasso를 사용해 URL로부터 이미지를 Bitmap으로 로드합니다.
        Picasso.get().load(imageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {
                    // --- 1. 이미지 우측 하단 영역만 잘라내기 ---
                    int fullWidth = bitmap.getWidth();
                    int fullHeight = bitmap.getHeight();
                    int cropSize = 50; // 분석할 영역 크기 (50x50px)

                    int cropX = Math.max(0, fullWidth - cropSize);
                    int cropY = Math.max(0, fullHeight - cropSize);
                    int finalCropWidth = Math.min(cropSize, fullWidth);
                    int finalCropHeight = Math.min(cropSize, fullHeight);

                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, finalCropWidth, finalCropHeight);

                    // --- 2. 잘라낸 이미지의 색상 분석 (Palette) ---
                    Palette.from(croppedBitmap).generate(palette -> {
                        if (palette != null) {
                            int dominantColor = palette.getDominantColor(Color.TRANSPARENT);
                            if (dominantColor == Color.TRANSPARENT) {
                                listener.onFailure();
                                return;
                            }
                            // 색상의 밝기 판단 (0.5 이상이면 밝은 색으로 간주)
                            boolean isLight = ColorUtils.calculateLuminance(dominantColor) > 0.5;
                            listener.onSuccess(dominantColor, isLight);
                        } else {
                            listener.onFailure();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onFailure();
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                e.printStackTrace();
                listener.onFailure();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // 로드 준비 중
            }
        });
    }


    public static void analyzePrimaryColor(Context context, String imageUrl, @NonNull OnPrimaryColorAnalyzedListener listener) {
        // Picasso를 사용해 URL로부터 이미지를 Bitmap으로 로드합니다.
        Picasso.get().load(imageUrl).into(new Target() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {

                    // 이미지의 색상 분석 (Palette) ---
                    Palette.from(bitmap).generate(palette -> {
                        if (palette != null) {
                            int dominantColor = palette.getDominantColor(Color.TRANSPARENT);
                            if (dominantColor == Color.TRANSPARENT) {
                                listener.onFailure();
                                return;
                            }

                            boolean isBackgroundLight = ColorUtils.calculateLuminance(dominantColor) > 0.5;
                            int selectedColor;
                            int unselectedColor;
                            if (isBackgroundLight) {
                                // 배경이 밝으면 -> 아이콘/텍스트는 어둡게
                                selectedColor =  Color.DKGRAY;
                                unselectedColor = Color.GRAY;
                            } else {
                                // 배경이 어두우면 -> 아이콘/텍스트는 밝게
                                selectedColor = Color.WHITE;
                                unselectedColor = Color.LTGRAY; // 밝은 회색
                            }

                            listener.onSuccess(dominantColor, dominantColor, selectedColor, unselectedColor);
                        } else {
                            listener.onFailure();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onFailure();
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                e.printStackTrace();
                listener.onFailure();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // 로드 준비 중
            }
        });
    }


}
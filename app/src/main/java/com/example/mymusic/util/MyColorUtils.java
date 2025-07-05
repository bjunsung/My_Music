package com.example.mymusic.util;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

public class MyColorUtils {

    public static int ensureContrastWithWhite(int color) {
        double contrast = androidx.core.graphics.ColorUtils.calculateContrast(Color.WHITE, color);
        if (contrast < 4.5) {
            return darkenHslColor(color, 0.6f);
        } else {
            return color;
        }
    }

    public static int darkenHslColor(int color, float lightnessFactor) {
        float[] hsl = new float[3];
        androidx.core.graphics.ColorUtils.colorToHSL(color, hsl);

        // Lightness 줄이기
        hsl[2] = Math.max(0f, Math.min(hsl[2] * lightnessFactor, 1f));

        return androidx.core.graphics.ColorUtils.HSLToColor(hsl);
    }

    public static int adjustForWhiteText(int color) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);

        float l = hsl[2]; // 밝기: 0.0 (어두움) ~ 1.0 (밝음)


        if (l < 0.20f) {
            hsl[2] += 0.075f; // 아주 어두운 경우 → 살짝만 밝게
        } else if (l < 0.28f) {
            hsl[2] += 0.075f; // 어두운 회색 계열도 약간 보정
        } else {
            // 나머지는 건드리지 않음 → 밝거나 파란 계열은 그대로 사용
        }

        return ColorUtils.HSLToColor(hsl);
    }



    public static int getSoftWhiteTextColor(int backgroundColor) {
        double luminance = ColorUtils.calculateLuminance(backgroundColor); // 0.0 ~ 1.0

        if (luminance < 0.15) {
            return Color.rgb(240, 240, 240); // 더 밝은 회색 (#F0F0F0)
        } else if (luminance < 0.25) {
            return Color.rgb(250, 250, 250); // 거의 흰색 (#FAFAFA)
        } else {
            return Color.WHITE; // 완전 흰색
        }
    }



}

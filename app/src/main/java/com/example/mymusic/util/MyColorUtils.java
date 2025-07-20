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

    public static int[] generateContrastColors(
            int primaryColor,
            float lightenFactor,    // 예: 1.15f
            float darkenFactor,     // 예: 0.42f
            float minLightness,     // 예: 0.1f
            float maxLightness,     // 예: 0.9f
            float minDifference     // 예: 0.3f
    ) {
        float[] baseHsl = new float[3];
        ColorUtils.colorToHSL(primaryColor, baseHsl);

        // 밝은 색과 어두운 색용 HSL 복사
        float[] brightHsl = baseHsl.clone();
        float[] darkHsl = baseHsl.clone();

        // 초기 조정
        brightHsl[2] = clamp(brightHsl[2] * lightenFactor, minLightness, maxLightness);
        darkHsl[2] = clamp(darkHsl[2] * darkenFactor, minLightness, maxLightness);

        float diff = Math.abs(brightHsl[2] - darkHsl[2]);

        // 최소 차이보다 작으면 보정
        if (diff < minDifference) {
            float desiredBright = clamp(darkHsl[2] + minDifference, minLightness, maxLightness);
            float desiredDark = clamp(brightHsl[2] - minDifference, minLightness, maxLightness);

            // 우선 bright 쪽을 위로 띄워보기
            if (desiredBright <= maxLightness) {
                brightHsl[2] = desiredBright;
            } else if (desiredDark >= minLightness) {
                darkHsl[2] = desiredDark;
            }
            // 둘 다 불가능하면 가능한 한 벌리기
            else {
                brightHsl[2] = maxLightness;
                darkHsl[2] = minLightness;
            }
        }

        return new int[]{
                ColorUtils.HSLToColor(brightHsl),
                ColorUtils.HSLToColor(darkHsl)
        };
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
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

    public static int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;

        float a = (Color.alpha(from) * inverseRatio) + (Color.alpha(to) * ratio);
        float r = (Color.red(from) * inverseRatio) + (Color.red(to) * ratio);
        float g = (Color.green(from) * inverseRatio) + (Color.green(to) * ratio);
        float b = (Color.blue(from) * inverseRatio) + (Color.blue(to) * ratio);

        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

}

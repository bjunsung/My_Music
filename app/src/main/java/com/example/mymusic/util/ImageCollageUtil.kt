package com.example.mymusic.util

// ImageCollageUtil.kt
import android.content.Context
import android.graphics.*
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageCollageUtil {

    /** 2x2 콜라주 생성 (size x size) */
    suspend fun make2x2(
        context: Context,
        urls: List<String?>,
        size: Int,
        @DrawableRes placeholderResId: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val safeSize = size.coerceAtLeast(2)
        val half = safeSize / 2
        val slots = (0 until 4).map { urls.getOrNull(it) }

        fun loadBitmap(any: Any?): Bitmap =
            try {
                Glide.with(context.applicationContext) // 💡 앱컨텍스트 권장
                    .asBitmap()
                    .load(any)
                    .submit()
                    .get()
            } catch (_: Exception) {
                rasterizeDrawableNoTint(context, placeholderResId, size, size)
            }

        val bitmaps = slots.map { url ->
            val bmp = if (url.isNullOrBlank()) {
                rasterizeDrawableNoTint(context, placeholderResId, size, size)
            } else {
                loadBitmap(url)
            }
            centerCropBitmap(bmp, half, half)
        }

        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawBitmap(bitmaps[0], 0f, 0f, paint)
        canvas.drawBitmap(bitmaps[1], half.toFloat(), 0f, paint)
        canvas.drawBitmap(bitmaps[2], 0f, half.toFloat(), paint)
        canvas.drawBitmap(bitmaps[3], half.toFloat(), half.toFloat(), paint)
        result
    }

    private fun rasterizeDrawableNoTint(
        context: Context,
        @DrawableRes resId: Int,
        width: Int,
        height: Int
    ): Bitmap {
        val raw = ResourcesCompat.getDrawable(context.resources, resId, null)
            ?: return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val drawable = DrawableCompat.wrap(raw).mutate()
        DrawableCompat.setTintList(drawable, null)
        DrawableCompat.setTintMode(drawable, null)
        drawable.colorFilter = null

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bmp
    }

    private fun centerCropBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        // 0) 입력 가드
        if (targetWidth <= 0 || targetHeight <= 0) {
            // 최소 1px 보장
            return Bitmap.createBitmap(maxOf(1, targetWidth), maxOf(1, targetHeight), Bitmap.Config.ARGB_8888)
        }
        if (src.width <= 0 || src.height <= 0) {
            return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        }

        // 1) 스케일: 반올림 내림으로 인해 sw/sh가 target보다 작아지는 것 방지 (ceil 사용)
        val scale = maxOf(
            targetWidth.toFloat() / src.width.toFloat(),
            targetHeight.toFloat() / src.height.toFloat()
        )
        val sw = kotlin.math.ceil(src.width * scale).toInt().coerceAtLeast(targetWidth)
        val sh = kotlin.math.ceil(src.height * scale).toInt().coerceAtLeast(targetHeight)

        // 2) 리사이즈 (최소 target 이상)
        val scaled = Bitmap.createScaledBitmap(src, sw, sh, true)

        // 3) 중앙 크롭 좌표 계산 + 범위 클램프
        val maxX = (scaled.width - targetWidth).coerceAtLeast(0)
        val maxY = (scaled.height - targetHeight).coerceAtLeast(0)
        val x = ((scaled.width - targetWidth) / 2).coerceIn(0, maxX)
        val y = ((scaled.height - targetHeight) / 2).coerceIn(0, maxY)

        // 4) 최종 크롭
        return Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
    }

}

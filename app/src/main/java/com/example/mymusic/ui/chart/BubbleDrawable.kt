package com.example.mymusic.ui.chart

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class BubbleDrawable(
    private val context: Context,
    @ColorInt private val bubbleColor: Int,
    private var cornerRadius: Float = 12f,
    private var tailHeight: Float = 12f,
    private var tailWidth: Float = 24f,
    private val tailPosition: TailPosition = TailPosition.CENTER,
    private val dx: Float
) : Drawable() {

    enum class TailPosition { LEFT, CENTER, RIGHT }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bubbleColor
    }

    override fun draw(canvas: Canvas) {

        if (!isTablet(context)) {
            cornerRadius = 40f
            tailHeight = 20f
            tailWidth = 40f
        }

        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        // 본체 영역 (꼬리 높이만큼 위로)
        val body = RectF(0f, tailHeight, width, height)
        canvas.drawRoundRect(body, cornerRadius, cornerRadius, paint)

        // 꼬리 Path
        val path = Path()
        val tailY = 0f
        val centerX = when (tailPosition) {
            TailPosition.LEFT ->  dx
            TailPosition.CENTER -> width / 2
            TailPosition.RIGHT -> width - dx
        }

        path.moveTo(centerX - tailWidth / 2, tailHeight)
        path.lineTo(centerX + tailWidth / 2, tailHeight)
        path.lineTo(centerX, tailY)
        path.close()

        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT


    fun isTablet(context: Context): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }



}

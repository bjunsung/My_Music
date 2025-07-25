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

    enum class TailPosition { LEFT, CENTER, RIGHT, LEFT_TOP }

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
        val offsetX = if (tailPosition == TailPosition.LEFT_TOP) tailHeight else 0f
        val body = RectF(offsetX, tailHeight, width, height)
        canvas.drawRoundRect(body, cornerRadius, cornerRadius, paint)

        //val body = RectF(0f, tailHeight, width, height)
        //canvas.drawRoundRect(body, cornerRadius, cornerRadius, paint)

        // 꼬리 Path
// 꼬리 Path

        val path = Path()
        when (tailPosition) {
            TailPosition.LEFT, TailPosition.CENTER, TailPosition.RIGHT -> {
                val tailY = 0f
                val centerX = when (tailPosition) {
                    TailPosition.LEFT -> dx
                    TailPosition.CENTER -> width / 2
                    TailPosition.RIGHT -> width - dx
                    else -> width / 2
                }

                path.moveTo(centerX - tailWidth / 2, tailHeight)
                path.lineTo(centerX + tailWidth / 2, tailHeight)
                path.lineTo(centerX, tailY)
                path.close()
            }

            TailPosition.LEFT_TOP -> {
                // 꼬리를 왼쪽 위로 향하게
                val tailX = 0f
                val centerY = dx  // ← 이건 상단으로부터 얼마나 떨어져 있는지

                path.moveTo(tailX + tailHeight, centerY - tailWidth / 2)
                path.lineTo(tailX + tailHeight, centerY + tailWidth / 2)
                path.lineTo(tailX, centerY)
                path.close()
            }
        }

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

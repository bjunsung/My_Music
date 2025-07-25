package com.example.mymusic.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var baseWaveform: FloatArray
    private var targetAmplitude = 0f
    private var currentAmplitude = 0f

    // 애니메이션 상태를 관리할 변수
    private var isAnimating = false
    // '워블' 효과를 위한 시간 변수
    private var startTime = System.currentTimeMillis()


    init {
        baseWaveform = generateBaseWaveform(200)
    }

    private fun generateBaseWaveform(count: Int): FloatArray {
        val waveform = FloatArray(count)
        val peaks = 2f

        for (i in 0 until count) {
            val envelope = sin(i.toFloat() / count.toFloat() * Math.PI * peaks) * 0.75f
            val detailWave = sin(i.toFloat() / count.toFloat() * Math.PI * 15f) * 0.2f
            waveform[i] = (abs(envelope) + detailWave).coerceIn(0.0, 1.0).toFloat()
        }
        return waveform
    }

    fun updateWaveform(newData: ByteArray) {
        val totalAbsAmplitude = newData.sumOf { abs(it.toInt()) }
        val averageAmplitude = if (newData.isNotEmpty()) (totalAbsAmplitude / newData.size).toFloat() else 0f
        targetAmplitude = (averageAmplitude / 128f).coerceIn(0f, 1.0f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        currentAmplitude += (targetAmplitude - currentAmplitude) * 1.8f

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val centerY = viewHeight / 2f

        val barCount = 60
        val spaceRatio = 0.5f
        val totalRatio = barCount + (barCount - 1) * spaceRatio
        val barWidth = viewWidth / totalRatio
        val spaceWidth = barWidth * spaceRatio

        // 시간에 따라 계속 변하는 값을 계산 (0.75초 주기로 반복)
        val time = (System.currentTimeMillis() - startTime) / 1250f

        for (i in 0 until barCount) {
            val x = i * (barWidth + spaceWidth)
            val waveIndex = (i * (baseWaveform.size / barCount)) % baseWaveform.size
            val baseHeight = baseWaveform[waveIndex]
            val wobbleFactor = 1.0f + (sin(time * 2 * Math.PI.toFloat() + i * 0.5f) * 0.1f)

            // --- 여기부터 양 끝 페이드 효과 로직 ---
            val fadeWidth = 6f // 몇 개의 막대에 걸쳐 페이드 효과를 줄지 결정 (예: 3개)
            var fadeMultiplier = 1.0f

            if (i < fadeWidth) {
                // 왼쪽 끝: 점진적으로 높이를 키움 (0.0 -> 1.0)
                fadeMultiplier = i / fadeWidth
            } else if (i > (barCount - 1) - fadeWidth) {
                // 오른쪽 끝: 점진적으로 높이를 줄임 (1.0 -> 0.0)
                fadeMultiplier = (barCount - 1 - i) / fadeWidth
            }
            // --- 페이드 효과 로직 끝 ---

            // 최종 높이에 fadeMultiplier를 곱해줍니다.
            val barHeight = baseHeight * currentAmplitude * (viewHeight / 2f) * wobbleFactor * fadeMultiplier

            // 페이드 효과로 완전히 사라지는 부분은 최소 높이를 0으로, 나머지는 4f로 설정
            val minHeight = if (fadeMultiplier <= 0.01f) 0f else 4f
            val finalBarHeight = barHeight.coerceAtLeast(minHeight)

            canvas.drawRoundRect(
                x,
                centerY - finalBarHeight,
                x + barWidth,
                centerY + finalBarHeight,
                barWidth,
                barWidth,
                paint
            )
        }

        // 애니메이션이 켜져 있으면 뷰를 계속 다시 그리도록 요청
        if (isAnimating) {
            postInvalidateOnAnimation()
        }
    }

    fun startAnimation() {
        if (isAnimating) return
        isAnimating = true
        startTime = System.currentTimeMillis()
        postInvalidateOnAnimation() // 애니메이션 프레임 시작
    }

    fun stopAnimation() {
        isAnimating = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
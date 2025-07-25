// WaveformView.kt
package com.example.mymusic.customView

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.mymusic.MainActivityViewModel
import kotlin.math.abs
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // 파형 막대를 그리기 위한 Paint 설정
    private val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // 오디오 파형 원본 데이터 (Visualizer에서 가져옴)
    private var waveform: ByteArray? = null
    private var visualizer: Visualizer? = null

    // 막대 개수와 각 막대 높이를 저장
    private val barCount = 50
    private var barHeights = FloatArray(barCount)
    private var isAnimating = false  // 현재 애니메이션 실행 여부

    // 파형 멈출 때 부드럽게 줄이는 애니메이션
    private var shrinkAnimator: ValueAnimator? = null
    // 랜덤 피크(파도처럼 움직이는 포인트) 리스트
    private val peaks = mutableListOf<Peak>()

    // 재생 여부를 관찰하기 위한 ViewModel
    private val viewModel = ViewModelProvider(context as ViewModelStoreOwner).get(MainActivityViewModel::class.java)
    private var playingObserver: Observer<Boolean>? = null

    // 피크 데이터 클래스 (파형의 특정 높이를 강조하기 위한 가상의 포인트)
    data class Peak(
        var x: Float,                // 현재 X 위치
        var targetX: Float,          // 목표 X 위치
        var height: Float,           // 현재 높이
        var targetHeight: Float,     // 목표 높이
        var alive: Boolean,          // 활성 상태 여부
        var lastUpdateTime: Long = System.currentTimeMillis()  // 마지막 업데이트 시간
    )

    // 피크 갱신 간격 (밀리초 단위, 17ms = 1프레임 수준)
    private var lastPeakUpdateTime = System.currentTimeMillis()
    private val peakUpdateIntervalMs = 17L

    // === Visualizer 연결 (오디오 세션 ID로 파형 데이터 가져오기) ===
    fun linkToSession(audioSessionId: Int) {
        visualizer?.release()
        visualizer = Visualizer(audioSessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?, waveformData: ByteArray?, samplingRate: Int
                ) {
                    waveformData?.let { updateWaveform(it) } // 새 오디오 데이터 업데이트
                }
                override fun onFftDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            enabled = true
        }
    }

    // === 외부에서 오디오 파형 데이터 수신 ===
    fun updateWaveform(data: ByteArray) {
        waveform = data.copyOf()
        postInvalidateOnAnimation()  // 다시 그리기 요청 (부드러운 애니메이션용)
    }

    // === 애니메이션 시작 (재생 중) ===
    private fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            visualizer?.enabled = true
            postInvalidateOnAnimation()
        }
    }

    // === 애니메이션 정지 (멈출 때 막대를 부드럽게 줄이기) ===
    private fun stopAnimation() {
        if (!isAnimating) return
        isAnimating = false
        visualizer?.enabled = false

        val currentHeights = barHeights.copyOf()
        val targetHeight = 2f
        shrinkAnimator?.cancel()
        // 막대를 300ms 동안 부드럽게 줄이는 애니메이션
        shrinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                for (i in barHeights.indices) {
                    barHeights[i] = currentHeights[i] + (targetHeight - currentHeights[i]) * progress
                }
                invalidate()
            }
            start()
        }
    }

    // === View가 화면에 붙을 때 ===
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        for (i in barHeights.indices) barHeights[i] = 2f
        invalidate()

        // ViewModel에서 재생 여부를 관찰해서 start/stop 호출
        playingObserver = Observer<Boolean> { playing ->
            if (playing) startAnimation() else stopAnimation()
        }
        viewModel.isPlaying.observeForever(playingObserver!!)
    }

    // === View가 화면에서 사라질 때 ===
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAnimating = false
        shrinkAnimator?.cancel()
        shrinkAnimator = null
        playingObserver?.let { viewModel.isPlaying.removeObserver(it) }
        playingObserver = null
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
        for (i in barHeights.indices) barHeights[i] = 2f
    }

    // === 피크 업데이트 (랜덤으로 움직이는 가상의 파도 포인트) ===
    private fun updatePeaks() {
        val targetCount = Random.nextInt(2, 5) // 피크 개수 랜덤 조정
        if (peaks.size < targetCount) {
            // 새 피크 추가
            val baseX = if (peaks.isNotEmpty()) peaks.random().x else Random.nextFloat() * barCount
            peaks.add(
                Peak(
                    baseX,
                    Random.nextFloat() * barCount,
                    0f,
                    Random.nextFloat() * 0.8f + 0.2f,
                    true,
                    System.currentTimeMillis()
                )
            )
        } else if (peaks.size > targetCount) {
            // 오래된 피크 제거 준비 (화면 밖으로 이동)
            peaks.firstOrNull()?.apply {
                targetX = if (Random.nextBoolean()) -5f else barCount + 5f
                targetHeight = 0f
                alive = false
                lastUpdateTime = System.currentTimeMillis()
            }
        }

        // 기존 피크를 랜덤하게 이동시킴
        for (peak in peaks) {
            if (peak.alive && Random.nextFloat() < 0.02f) {
                peak.targetX = Random.nextFloat() * barCount
                peak.targetHeight = Random.nextFloat() * 0.8f + 0.2f
                peak.lastUpdateTime = System.currentTimeMillis()
            }
        }

        // 350ms 경과하면 피크 크기를 줄임
        val now = System.currentTimeMillis()
        for (peak in peaks) {
            if (peak.alive && now - peak.lastUpdateTime > 350) {
                peak.targetHeight *= 0.2f
            }
        }

        // 피크 위치와 크기를 부드럽게 보간
        val speed = 0.25f
        for (peak in peaks) {
            peak.x += (peak.targetX - peak.x) * speed
            peak.height += (peak.targetHeight - peak.height) * speed
        }

        // 화면 밖으로 나간 피크 제거
        peaks.removeAll { !it.alive && (it.x < -2f || it.x > barCount + 2f) }
    }

    // === 특정 막대 위치에서의 피크 영향을 계산 ===
    private fun calculateSmoothHeight(i: Int): Float {
        if (peaks.isEmpty()) return 0f
        var maxVal = 0f
        for (peak in peaks) {
            val dist = abs(peak.x - i)
            val falloff = (1f - (dist / (barCount / 4f))).coerceIn(0f, 1f) // 거리에 따라 영향 감소
            maxVal = maxOf(maxVal, peak.height * falloff)
        }
        return maxVal
    }

    // === 실제 파형 그리기 ===
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = waveform ?: return

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f

        // 막대 폭과 간격 계산
        val spaceRatio = 0.5f
        val totalRatio = barCount + (barCount - 1) * spaceRatio
        val barWidth = w / totalRatio
        val spaceWidth = barWidth * spaceRatio

        if (isAnimating) {
            // 오디오 데이터 기반 막대 높이 보간
            val step = data.size / barCount
            for (i in 0 until barCount) {
                val sample = abs(data[i * step].toInt())
                val amplitude = (sample / 128f).coerceIn(0f, 1f)
                barHeights[i] += (amplitude * (h / 2f) - barHeights[i]) * 0.25f
            }

            // 피크 갱신 (밀리초 단위로)
            val now = System.currentTimeMillis()
            if (now - lastPeakUpdateTime >= peakUpdateIntervalMs) {
                updatePeaks()
                lastPeakUpdateTime = now
            }
        }

        // 막대 그리기
        for (i in 0 until barCount) {
            val offsetIndex = i - (barCount - 1) / 2f
            val x = centerX() + offsetIndex * (barWidth + spaceWidth)
            val peak = calculateSmoothHeight(i) * (h / 2f)

            val avgAmplitude = barHeights.average().toFloat() / (h / 2f)
            var volumeLimiter = if (avgAmplitude < 0.65f) 0.35f else 1f
            if (avgAmplitude < 0.5f) volumeLimiter = 0f


            val mixedHeight = (barHeights[i] * 0.3f + peak * 0.7f) * volumeLimiter
            val edgeFade = (1f - (abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f))).coerceIn(0f, 1f)
            val finalHeight = (mixedHeight * edgeFade).coerceIn(5f, 600f)
            val top = centerY - finalHeight
            val bottom = centerY + finalHeight

            canvas.drawRoundRect(x, top, x + barWidth, bottom, barWidth / 2, barWidth / 2, paint)
        }

        // 애니메이션 실행 중이면 계속 다시 그리기
        if (isAnimating) postInvalidateOnAnimation()
    }

    private fun centerX() = width / 2f
}


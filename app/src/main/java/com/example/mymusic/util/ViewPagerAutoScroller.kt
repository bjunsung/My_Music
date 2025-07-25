package com.example.mymusic.util

import android.os.Handler
import android.os.Looper
import androidx.viewpager2.widget.ViewPager2
import setCurrentItemWithDuration

class ViewPagerAutoScroller(
    private val pager: ViewPager2,
    private val autoDelayMs: Long,
    private val afterUserDelayMs: Long,
    private val extraCallbacks: List<ViewPager2.OnPageChangeCallback> = emptyList()
) {

    /**
     * 사용 방법
    val myCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageScrollStateChanged(state: Int) {
    // 네 추가 처리
    }
    override fun onPageSelected(position: Int) { /* ... */ }
    }

    val scroller = ViewPagerAutoScroller(
    pager = binding.imagePager,
    autoDelayMs = 5_000L,
    afterUserDelayMs = 7_500L,
    extraCallbacks = listOf(myCallback)
    )
    scroller.attach()
     */

    private val handler = Handler(Looper.getMainLooper())
    private var userDragging = false
    private var attached = false

    private val task = object : Runnable {
        override fun run() {
            val ad = pager.adapter ?: return
            val count = ad.itemCount
            if (count <= 1) return
            pager.setCurrentItemWithDuration((pager.currentItem + 1) % count, 350L)
            handler.postDelayed(this, autoDelayMs)
        }
    }

    private val internalCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            // 1) 자동 스크롤 제어 로직
            when (state) {
                ViewPager2.SCROLL_STATE_DRAGGING -> {
                    userDragging = true
                    stopInternal()
                }
                ViewPager2.SCROLL_STATE_IDLE -> {
                    if (!attached) return
                    val delay = if (userDragging) afterUserDelayMs else autoDelayMs
                    userDragging = false
                    startWithDelay(delay)
                }
            }
            // 2) 외부 콜백에도 전달
            extraCallbacks.forEach { it.onPageScrollStateChanged(state) }
        }

        override fun onPageSelected(position: Int) {
            extraCallbacks.forEach { it.onPageSelected(position) }
        }

        override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
            extraCallbacks.forEach { it.onPageScrolled(position, offset, offsetPixels) }
        }
    }

    fun attach() {
        if (attached) return
        pager.registerOnPageChangeCallback(internalCallback)
        attached = true
    }
    fun detach() {
        if (!attached) return
        stopInternal()
        pager.unregisterOnPageChangeCallback(internalCallback)
        attached = false
    }
    fun start() = startWithDelay(autoDelayMs)
    fun stop() = stopInternal()

    private fun startWithDelay(delay: Long) {
        stopInternal()
        handler.postDelayed(task, delay)
    }
    private fun stopInternal() {
        handler.removeCallbacks(task)
    }
}

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

fun ViewPager2.setCurrentItemWithDuration(targetItem: Int, durationMs: Long) {
    if (durationMs <= 0L || targetItem == currentItem) {
        setCurrentItem(targetItem, true)
        return
    }

    val pagesToMove = targetItem - currentItem
    if (pagesToMove == 0) return

    val pageWidth = width + paddingLeft + paddingRight
    val totalDrag = pageWidth * pagesToMove

    if (!beginFakeDrag()) {
        setCurrentItem(targetItem, true)
        return
    }

    var previousDrag = 0f
    ValueAnimator.ofFloat(0f, totalDrag.toFloat()).apply {
        duration = durationMs
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            val dragOffset = (animator.animatedValue as Float) - previousDrag
            fakeDragBy(-dragOffset) // 음수 = 오른쪽 → 왼쪽
            previousDrag += dragOffset
        }
        addListener(onEnd = { endFakeDrag() }, onCancel = { endFakeDrag() })
        start()
    }
}

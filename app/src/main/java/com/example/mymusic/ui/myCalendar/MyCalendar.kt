package com.example.mymusic.ui.myCalendar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentMyCalendarBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.DaySize
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthScrollListener
import com.kizitonwose.calendar.view.ViewContainer
import okhttp3.internal.notify
import java.time.DayOfWeek // 요청하신 import
import java.time.LocalDate // 요청하신 import
import java.time.Year
import java.time.YearMonth // 요청하신 import


class MyCalendar : Fragment() {
    private var _binding: FragmentMyCalendarBinding? = null
    private val binding get() = _binding!!
    private val calendarViewModel: CalendarViewModel by viewModels()

    private val today = LocalDate.now() // java.time.LocalDate 사용
    private lateinit var calendarView: CalendarView

    private var shownMonth: YearMonth? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        isExpanded = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        Handler(Looper.getMainLooper()).postDelayed(Runnable{ startPostponedEnterTransition() }, 50)

        bind()

        // 캘린더 데이터 로드 및 UI 갱신 시작
        calendarViewModel.loadFavoriteList(object : CalendarViewModel.FavoriteUpdateCallback {
            override fun onSuccess(favList: List<Favorite>) {
                Log.d(TAG, "Favorite list loaded/updated successfully. Updating calendar UI.")
                // ViewModel의 favoriteList와 eventsByDate 맵은 이미 loadFavoriteList 내부에서 업데이트됨

                // UI 업데이트는 메인 스레드에서 진행
                view.post { // 뷰가 완전히 준비된 후 캘린더 설정을 보장
                    setupCalendar() // 캘린더의 기본 설정 (setup, dayBinder 등)
                    calendarView.scrollToMonth(calendarViewModel.currentMonth) // java.time.YearMonth 사용
                    calendarView.notifyCalendarChanged() // 데이터가 변경되었음을 캘린더 뷰에 알림
                }
            }

            override fun onDuplicated() {
                // 데이터는 동일하지만, 프래그먼트가 다시 활성화되거나 UI 갱신이 필요할 때
                Log.d(TAG, "Favorite list duplicated. Notifying calendar changed to refresh UI.")
                view.post {
                    setupCalendar() // 캘린더의 기본 설정 (setup, dayBinder 등)
                    calendarView.scrollToMonth(calendarViewModel.currentMonth) // java.time.YearMonth 사용
                    calendarView.notifyCalendarChanged() // 캘린더 뷰 갱신
                }
            }

            override fun onFailure(message: String?) {
                Log.e(TAG, "Failed to load favorite list: $message")
                // 사용자에게 오류 메시지를 보여주는 등의 적절한 에러 처리
            }
        })
    }


    private fun bind() {
        val menuButton = binding.btnMenu
        menuButton.setOnClickListener { togglePanel() }
        val navToReleaseDateChartButton = binding.releaseDateChartText
        navToReleaseDateChartButton.setOnClickListener { findNavController().navigate(R.id.action_myCalendarFragment_to_releaseDateChart) }
        val moveTodayTextView = binding.moveTodayText
        moveTodayTextView.setOnClickListener{ calendarView.scrollToMonth(LocalDate.now().yearMonth) }
        val navToPlayCountChartButton = binding.playCountChartText
        navToPlayCountChartButton.setOnClickListener { findNavController().navigate(R.id.action_myCalendarFragment_to_playCountChart) }

    }

    private fun setupCalendar() {
        calendarView = binding.calendarView


        calendarView.monthScrollListener = { month ->
            shownMonth = month.yearMonth
            calendarViewModel.currentMonth = month.yearMonth
            val yearText = month.yearMonth.year.toString().toCharArray().joinToString("\n") // month.yearMonth는 YearMonth 사용
            val monthTextEn = month.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
            binding.yearText.text = yearText
            binding.monthText.text = monthTextEn
        }


        calendarView.dayViewResource = R.layout.calendar_day_layout
        calendarView.daySize = DaySize.Rectangle

        // 캘린더 뷰의 시작 월, 끝 월, 요일 시작 설정
        calendarView.setup(
            startMonth = YearMonth.from(today.minusMonths(11)),
            endMonth = YearMonth.from(today.plusMonths(11)),
            firstDayOfWeek = DayOfWeek.SUNDAY
        )


        // 날짜 셀 바인더: 각 날짜 셀에 데이터를 바인딩하는 로직
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data // CalendarDay는 내부적으로 LocalDate (java.time) 사용
                container.dayText.text = data.date.dayOfMonth.toString() // data.date는 LocalDate (java.time)


                // 오늘 날짜 하이라이트
                if (data.date == LocalDate.now()) { // LocalDate (java.time)
                    container.dayText.setBackgroundResource(R.drawable.calendar_today_background)
                    container.dayText.setTypeface(null, Typeface.BOLD)
                } else if (data.position != DayPosition.MonthDate) {
                    container.dayText.background = null
                    container.dayText.setTextColor(Color.LTGRAY)
                    container.dayText.setTypeface(null, Typeface.NORMAL)
                    return
                }
                else {
                    container.dayText.background = null
                    container.dayText.setTextColor(ContextCompat.getColor(context!!, R.color.text_secondary))
                    container.dayText.setTypeface(null, Typeface.NORMAL)
                }

                // ViewModel에서 미리 계산된 맵에서 해당 날짜의 이벤트 리스트를 가져옴
                var dateEvents = calendarViewModel.getEventsForDate(data.date)

                // 로그 출력 (이벤트 확인용)
                Log.d(TAG, "Binding date ${data.date}: found ${dateEvents.size} events.")
                dateEvents.takeIf { it.isNotEmpty() }?.get(0)?.title?.let {
                    Log.d(TAG, "First event for ${data.date}: $it")
                } ?: Log.d(TAG, "No events for ${data.date}")

                // 이벤트 RecyclerView 설정 및 어댑터에 데이터 전달
                // DayEventAdapter는 ListAdapter를 사용하는 것이 성능상 유리합니다.
                val adapter = DayEventAdapter(
                    dateEvents,
                    data.date,
                    object : DayEventAdapter.OnClickListener {
                        @OptIn(UnstableApi::class)
                        override fun onItemClick(
                            holder: DayEventAdapter.EventViewHolder,
                            favorite: Favorite
                        ) {
                            val args = Bundle().apply {
                                putParcelable(MusicInfoFragment.ARGUMENTS_KEY, favorite)
                                putString(MusicInfoFragment.TRANSITION_NAME_KEY, holder.itemView.transitionName)
                            }
                            val extras = FragmentNavigatorExtras(
                                holder.itemView to holder.itemView.transitionName
                            )
                            findNavController().navigate(R.id.musicInfoFragment, args, null, extras)
                        }

                        override fun onDataReady() {
                            startPostponedEnterTransition()
                        }
                    })
                container.eventRecyclerView.layoutManager = LinearLayoutManager(container.view.context)
                container.eventRecyclerView.adapter = adapter
                adapter.submitList(dateEvents) // ListAdapter를 사용한다면 submitList
            }
        }
    }




    private var isExpanded = false

    private fun togglePanel() {
        val panel = binding.cardSidePanel
        val menuText = binding.moveTodayText
        val releaseDateChart = binding.releaseDateChartText
        val targetWidth = if (isExpanded) 60 else 220
        val widthInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            targetWidth.toFloat(),
            resources.displayMetrics
        ).toInt()

        val animator = ValueAnimator.ofInt(panel.width, widthInPx)
        animator.addUpdateListener { valueAnimator ->
            val layoutParams = panel.layoutParams
            layoutParams.width = valueAnimator.animatedValue as Int
            panel.layoutParams = layoutParams
        }
        animator.duration = 220

        animator.start()

        // 텍스트 표시/숨김
        //menuText.visibility = if (isExpanded) View.GONE else View.VISIBLE
        //releaseDateChart.visibility = if (isExpanded) View.GONE else View.VISIBLE

        isExpanded = !isExpanded




    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MyCalendar"
    }

}

class DayViewContainer(view: View) : ViewContainer(view) {
    val root: View = view
    var eventRecyclerView: RecyclerView = view.findViewById(R.id.calendar_day_event_recycler_view)
    val dayText: TextView = view.findViewById(R.id.calendar_day_text)
    lateinit var day: CalendarDay
}
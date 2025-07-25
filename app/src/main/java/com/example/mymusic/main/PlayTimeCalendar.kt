package com.example.mymusic.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.databinding.FragmentPlayTimeCalendarBinding
import com.example.mymusic.model.Favorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.core.view.isVisible
import com.example.mymusic.customLayout.StableGridLayoutManager
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

class PlayTimeCalendar : Fragment() {

    private lateinit var adapter: CalendarAdapter
    private val today = LocalDate.now()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()
    private var daySize = 0
    private var _binding : FragmentPlayTimeCalendarBinding? = null
    private val binding get() = _binding!!
    private val recyclerView: RecyclerView by lazy { binding.calendarRecyclerView }
    private val selectedDateTextView: TextView by lazy { binding.selectedDate }
    private val playCountForDateTextView: TextView by lazy { binding.playedCountForDay }
    private var selectedDate: LocalDate? = null
    private var recyclerViewState: Parcelable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayTimeCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()

        mainActivityViewModel.totalPlayCountInARow.observe(viewLifecycleOwner) { count ->
            val favorite = mainActivityViewModel.currentTrack.value
            favorite?.let {
                mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
                    val favoriteWithPlayCount =
                        mainActivityViewModel.favoriteSongRepository.getFavoriteSongWithPlayCount(
                            favorite.track.trackId
                        )
                    Handler(Looper.getMainLooper()).post { updateUi(favoriteWithPlayCount) }
                }
            }
        }

        recyclerView.post { recyclerView.scrollToPosition(daySize - 1) }
    }

    private fun bind() {
        //val layoutManager = GridLayoutManager(requireContext(), 7, RecyclerView.HORIZONTAL, false)
        val layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW         // 가로로 나열
            flexWrap = FlexWrap.WRAP                 // 줄바꿈 허용 (wrap_content 느낌)
            justifyContent = JustifyContent.FLEX_START  // 왼쪽 정렬
            alignItems = AlignItems.FLEX_START          // 위쪽 정렬
        }
        recyclerView.layoutManager = layoutManager

        val favorite = mainActivityViewModel.currentTrack.value
        // 어댑터를 미리 초기화 (빈 데이터로)
        adapter = CalendarAdapter(emptyList())

        Log.d(TAG, "play time calendar adapter initialized with empty list")

        recyclerView.adapter = adapter
        recyclerView.isSaveEnabled = true

        mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
            val favoriteWithPlayCount = mainActivityViewModel.favoriteSongRepository.getFavoriteSongWithPlayCount(favorite!!.track.trackId)
            mainActivityViewModel.viewModelScope.launch(Dispatchers.Main) {
                //val data = generateDataFromFavorite(favoriteWithPlayCount)
                //adapter.updateData(data)
                updateUi(favoriteWithPlayCount)

                musicPlayingViewModel.currentPage.observe(viewLifecycleOwner) {page ->
                    Log.d(TAG, "page changed: " + page + " day size: " + daySize)
                    if (page == 2) {
                        //recyclerView.postDelayed( {recyclerView.scrollToPosition(daySize-1) }, 50)
                        recyclerView.post {
                            val scroller = object : LinearSmoothScroller(context) {
                                override fun getHorizontalSnapPreference() = SNAP_TO_END
                            }
                            scroller.targetPosition = daySize - 1
                            recyclerView.layoutManager?.startSmoothScroll(scroller)
                            Log.d(TAG, "scroll to today (post)")

                        }
                    }
                }
            }
        }

        selectedDateTextView.alpha = 0f
        playCountForDateTextView.alpha = 0f

        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null

        adapter.setOnItemClickListener(object : CalendarAdapter.OnItemClickListener {
            override fun onItemClick(date: LocalDate, playCount: Int, position: Int) {
                //val layoutManager = recyclerView.layoutManager as GridLayoutManager
                //val firstVisible = layoutManager.findFirstVisibleItemPosition()


                selectedDateTextView.text = date.toString()
                playCountForDateTextView.text = playCount.toString()
                selectedDate = date
                selectedDateTextView.alpha = 1f
                playCountForDateTextView.alpha = 1f

                //recyclerView.post { recyclerView.scrollToPosition(position) }
                //recyclerView.postDelayed(  {layoutManager.scrollToPosition(firstVisible)}, 200 )
                //Handler(Looper.getMainLooper()).postDelayed( {recyclerView.scrollToPosition(position)}, 200 )

                /*
                recyclerView.post {
                    val scroller = object : LinearSmoothScroller(context) {
                        override fun getHorizontalSnapPreference() = SNAP_TO_END
                    }
                    scroller.targetPosition = position
                    recyclerView.layoutManager?.startSmoothScroll(scroller)
                }

                 */


            }

        })
    }

    private fun updateUi(favorite: Favorite){
        val data = generateDataFromFavorite(favorite)
        adapter.updateData(data)

        recyclerView.post {
            val scroller = object : LinearSmoothScroller(context) {
                override fun getHorizontalSnapPreference() = SNAP_TO_END
            }
            scroller.targetPosition = daySize - 1
            recyclerView.layoutManager?.startSmoothScroll(scroller)
            Log.d(TAG, "scroll to today (post)")
        }

        val playedCountLastYear = favorite.playCountByDay
            .filter { ChronoUnit.DAYS.between(it.key, today) < daySize }
            .values
            .sum()
        binding.playedCountLastYear.text = playedCountLastYear.toString()

        if (selectedDateTextView.isVisible && selectedDate != null){
            playCountForDateTextView.text = (favorite.playCountByDay.get(selectedDate) ?: 0).toString()
        }

    }

    // **favorite.playCountByDay 기반으로 변환**
    private fun generateDataFromFavorite(favorite: Favorite): List<ContributionDay> {
        //  Map<LocalDate, Int>
        val contributions = favorite.playCountByDay

        val list = mutableListOf<ContributionDay>()
        var startDate = today.minusYears(1)
        while (startDate.dayOfWeek != DayOfWeek.MONDAY) {
            startDate = startDate.minusDays(1)
        }

        daySize = (ChronoUnit.DAYS.between(startDate, today) + 1).toInt()
        Log.d(TAG, "daysize: $daySize")

        for (i in 0 until daySize) {
            val date = startDate.plusDays(i.toLong())
            val count = contributions[date] ?: 0
            list.add(ContributionDay(date, count))
        }
        return list
    }


    companion object {
        private const val TAG = "PlayTimeCalendar"
    }
}

data class ContributionDay(
    val date: LocalDate,
    val count: Int
)



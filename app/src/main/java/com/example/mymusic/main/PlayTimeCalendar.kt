package com.example.mymusic.main

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentPlayTimeCalendarBinding
import com.example.mymusic.model.Favorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PlayTimeCalendar : Fragment() {

    private lateinit var adapter: CalendarAdapter
    private val today = LocalDate.now()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()
    private var daySize = 0
    private var _binding : FragmentPlayTimeCalendarBinding? = null
    private val binding get() = _binding!!
    private val recyclerView: RecyclerView by lazy { binding.calendarRecyclerView }

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
        val layoutManager = GridLayoutManager(requireContext(), 7, RecyclerView.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val favorite = mainActivityViewModel.currentTrack.value
        // 어댑터를 미리 초기화 (빈 데이터로)
        adapter = CalendarAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.isSaveEnabled = true

        mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
            val favoriteWithPlayCount = mainActivityViewModel.favoriteSongRepository.getFavoriteSongWithPlayCount(favorite!!.track.trackId)
            Handler(Looper.getMainLooper()).post {
                val data = generateDataFromFavorite(favoriteWithPlayCount)
                adapter.updateData(data)
                musicPlayingViewModel.currentPage.observe(viewLifecycleOwner) {page ->
                    if (page == 2) {
                        recyclerView.scrollToPosition(daySize-1)
                    }
                }
            }
        }

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



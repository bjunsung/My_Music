package com.example.mymusic.ui.musicInfo.subFragment

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymusic.databinding.FragmentPlayTimeCalendarBinding
import com.example.mymusic.main.playtime.CalendarAdapter
import com.example.mymusic.main.playtime.ContributionDay
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoViewModel
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PlayTimeCalendar : Fragment() {
    private val musicInfoViewModel: MusicInfoViewModel by activityViewModels()
    private lateinit var adapter: CalendarAdapter
    private val today = LocalDate.now()
    private var daySize = 0
    private var _binding : FragmentPlayTimeCalendarBinding? = null
    private val binding get() = _binding!!
    private val recyclerView: RecyclerView by lazy { binding.calendarRecyclerView }
    private val selectedDateTextView: TextView by lazy { binding.selectedDate }
    private val playCountForDateTextView: TextView by lazy { binding.playedCountForDay }
    private var selectedDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayTimeCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()

        musicInfoViewModel.favorite?.let { favorite ->
            musicInfoViewModel.viewModelScope.launch(Dispatchers.IO) {
                val favoriteWithPlayCount =
                    musicInfoViewModel.favoriteSongRepository.getFavoriteSongWithPlayCount(
                        favorite.track.trackId
                    )
                Handler(Looper.getMainLooper()).post { updateUi(favoriteWithPlayCount) }
            }

            Glide.with(requireContext())
                .load(favorite.track.artworkUrl)
                .centerCrop()
                .into(binding.backgroundImage)
            if (favorite.track.primaryColor != null) {
                setBackgroundColor(favorite.track.primaryColor)
            }
            else {
                ImageColorAnalyzer.analyzePrimaryColor(requireContext(), favorite.track.artworkUrl,
                    object : ImageColorAnalyzer.OnPrimaryColorAnalyzedListener {
                        override fun onSuccess(
                            dominantColor: Int,
                            primaryColor: Int,
                            selectedColor: Int,
                            unselectedColor: Int
                        ) {
                            setBackgroundColor(primaryColor)
                            favorite.track.primaryColor = primaryColor
                        }

                        override fun onFailure() {}

                    })
            }

        }

        recyclerView.post { recyclerView.scrollToPosition(daySize - 1) }
    }

    private fun lockRecyclerViewWidth() {
        val displayMetrics = Resources.getSystem().displayMetrics
        val orientation = resources.configuration.orientation

        val widthPx = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            (displayMetrics.widthPixels * 0.92f).toInt()   // 세로 모드 → 92% 만 사용
        } else {
            (displayMetrics.widthPixels * 0.92f).toInt()  // 가로 모드 → 45.5%만 사용
        }

        binding.calendarRecyclerView.layoutParams = binding.calendarRecyclerView.layoutParams.apply {
            width = widthPx
        }
        binding.calendarRecyclerView.setHasFixedSize(true)
    }


    private fun bind() {
        val layoutManager = GridLayoutManager(requireContext(), 7, RecyclerView.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager

        lockRecyclerViewWidth()

        val favorite = musicInfoViewModel.favorite
        // 어댑터를 미리 초기화 (빈 데이터로)
        adapter = CalendarAdapter(emptyList())

        Log.d(TAG, "play time calendar adapter initialized with empty list")

        recyclerView.adapter = adapter
        //recyclerView.isSaveEnabled = true

        musicInfoViewModel.viewModelScope.launch(Dispatchers.IO) {
            val favoriteWithPlayCount = musicInfoViewModel.favoriteSongRepository.getFavoriteSongWithPlayCount(favorite!!.track.trackId)
            musicInfoViewModel.viewModelScope.launch(Dispatchers.Main) {
                updateUi(favoriteWithPlayCount)
            }
        }

        selectedDateTextView.alpha = 0f
        playCountForDateTextView.alpha = 0f

        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null

        adapter.setOnItemClickListener(object : CalendarAdapter.OnItemClickListener {
            override fun onItemClick(
                day: ContributionDay,
                position: Int
            ) {
                val newList = musicInfoViewModel.lastContributionData.value!!.map { it.copy(isFocused = it.date == day.date) }
                adapter.updateData(newList)

                selectedDateTextView.text = day.date.toString()
                playCountForDateTextView.text = day.count.toString()
                selectedDate = day.date
                selectedDateTextView.alpha = 1f
                playCountForDateTextView.alpha = 1f
            }

        })
    }






    private fun updateUi(favorite: Favorite){
        val data = generateDataFromFavorite(favorite)
        musicInfoViewModel.lastContributionData.value = data
        adapter.updateData(data)

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
            list.add(ContributionDay(date, count, false))
        }
        return list
    }

    private fun setBackgroundColor(primaryColor: Int) {
        var darkenColor: Int
        if (context != null && DarkModeUtils.isDarkMode(context)) {
            darkenColor =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.3f
                )
        } else {
            darkenColor =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.9f
                )
        }
        binding.dimView.setBackgroundColor(MyColorUtils.adjustForWhiteText(darkenColor))
    }

    companion object {
        private const val TAG = "PlayTimeCalendar"
    }
}
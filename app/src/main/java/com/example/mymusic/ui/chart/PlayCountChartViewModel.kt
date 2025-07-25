package com.example.mymusic.ui.chart

import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import com.example.mymusic.model.Favorite
import java.time.LocalDate

class PlayCountChartViewModel : ViewModel() {
    var rawList: List<Favorite>? = null
    var top10LastMonth: List<Favorite>? = null
    var top10Last3Month: List<Favorite>? = null
    var top10LastYear: List<Favorite>? = null
    val today = LocalDate.now()
    val oneMonthAgo = today.minusMonths(1)
    val threeMonthAgo = today.minusMonths(3)
    val oneYearAgo = today.minusYears(1)
    var specificYear = today.year
    var specificMap: MutableMap<Int, List<Favorite>> = mutableMapOf()

    var adapter: PlayTimeChartAdapter? = null
    var transitionName: String = ""

    //popup view
    var onFocused = false
    var focusedPosition: Int = 0
    var focusedTrack: Favorite? = null

    var reenterStateFromMusicInfoFragment = false
    var reenterStateFromAlbumInfoFragment = false
    var reenterStateFromArtistInfoFragment = false

    var chartOption = LAST_MONTH

    fun getStartDate() : LocalDate {
        when (chartOption) {
            LAST_MONTH -> return oneMonthAgo
            LAST_3_MONTH -> return threeMonthAgo
            LAST_YEAR -> return oneYearAgo
            SPECIFIC_YEAR -> return LocalDate.of(specificYear, 1, 1)
            else -> return today
        }
    }

    fun getEndDate(): LocalDate {
        when (chartOption) {
            SPECIFIC_YEAR -> return LocalDate.of(specificYear, 12, 31)
            else -> return today
        }
    }

    fun getOrderedList() : List<Favorite>? {
        when (chartOption) {
            LAST_3_MONTH -> return top10Last3Month
            LAST_YEAR -> return top10LastYear
            SPECIFIC_YEAR -> (specificMap[specificYear]?.let { return it }) ?: return getSpecificList(specificYear)
            else -> return top10LastMonth
        }
    }

    fun getSpecificList(year: Int): List<Favorite> {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)
        var specificList = rawList
            ?.mapNotNull { item ->
                val total = item.playCountByDay
                    .filterKeys { it in startDate..endDate }
                    .values
                    .sum()
                if (total == 0) return@mapNotNull null
                item to total
            }
            ?.sortedByDescending { it.second }
            ?.map{ it.first }
            ?.take(30)
            ?.toList()
        if (specificList == null)
            specificList = emptyList()
        specificMap.put(year, specificList)
        Log.d(TAG, "year: " + year + " list: " + specificList)
        return specificList
    }

    companion object {
        const val LAST_MONTH = 0
        const val LAST_3_MONTH = 1
        const val LAST_YEAR = 2
        const val SPECIFIC_YEAR = 3

        const val TAG = "PlayCountChartViewModel"
    }


}
package com.example.mymusic.ui.chart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.mymusic.model.Favorite
import java.time.DayOfWeek
import java.time.LocalDate

class PlayCountChartViewModel : ViewModel() {
    var navController: NavController? = null
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
    var quarter: String = NO_QUARTER

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
        return when (chartOption) {
            LAST_MONTH -> oneMonthAgo
            LAST_3_MONTH -> threeMonthAgo
            LAST_YEAR -> oneYearAgo
            SPECIFIC_YEAR -> {
                when(quarter) {
                    FIRST_QUARTER -> LocalDate.of(specificYear, 1, 1)
                    SECOND_QUARTER -> LocalDate.of(specificYear, 4, 1)
                    THIRD_QUARTER -> LocalDate.of(specificYear, 7, 1)
                    FOURTH_QUARTER -> LocalDate.of(specificYear, 10, 1)
                    else -> LocalDate.of(specificYear, 1, 1)
                }
            }
            else -> today
        }
    }

    fun getEndDate(): LocalDate {
        return when (chartOption) {
            SPECIFIC_YEAR -> {
                when(quarter) {
                    FIRST_QUARTER -> LocalDate.of(specificYear, 4, 1).minusDays(1)
                    SECOND_QUARTER -> LocalDate.of(specificYear, 7, 1).minusDays(1)
                    THIRD_QUARTER -> LocalDate.of(specificYear, 10, 1).minusDays(1)
                    FOURTH_QUARTER -> LocalDate.of(specificYear, 12, 31)
                    else -> LocalDate.of(specificYear, 12, 31)
                }
            }
            else -> today
        }
    }

    fun getOrderedList() : List<Favorite>? {
        when (chartOption) {
            LAST_3_MONTH -> return top10Last3Month
            LAST_YEAR -> return top10LastYear
            SPECIFIC_YEAR -> {
                return getSpecificList(specificYear)
                /*
                when (quarter) {
                    NO_QUARTER -> return (specificMap[specificYear]?.let { return it }) ?: return getSpecificList(specificYear)
                    else -> return getSpecificList(specificYear)
                }
                 */
            }
            else -> return top10LastMonth
        }
    }

    fun getSpecificList(year: Int): List<Favorite> {
        val startDate = when (quarter)  {
            FIRST_QUARTER -> LocalDate.of(year, 1, 1)
            SECOND_QUARTER -> LocalDate.of(year, 4, 1)
            THIRD_QUARTER -> LocalDate.of(year, 7, 1)
            FOURTH_QUARTER -> LocalDate.of(year, 10, 1)
            else -> LocalDate.of(year, 1, 1)
        }
        val endDate = when (quarter)  {
            FIRST_QUARTER -> LocalDate.of(year, 4, 1).minusDays(1)
            SECOND_QUARTER -> LocalDate.of(year, 7, 1).minusDays(1)
            THIRD_QUARTER -> LocalDate.of(year, 10, 1).minusDays(1)
            FOURTH_QUARTER -> LocalDate.of(year, 12, 31)
            else -> LocalDate.of(year, 12, 31)
        }
        var specificList = rawList
            ?.mapNotNull { item ->
                val total = item.playCountByDay
                    .filterKeys { it in startDate..endDate }
                    .values
                    .sum()
                if (total == 0) return@mapNotNull null
                item to total
            }
            ?.sortedWith (
                compareByDescending<Pair<Favorite, Int>> { it.second }
                    .thenByDescending { it.first.lastPlayedDate }
                )
            ?.map{ it.first }
            ?.take(100)
            ?.toList()
        if (specificList == null)
            specificList = emptyList()
        specificMap.put(year, specificList)
        Log.d(TAG, "year: " + year + " list: " + specificList)
        return specificList
    }

    fun getCountByDay(): Map<DayOfWeek, Int> {
        // 모든 요일 0으로 초기화
        val counts = DayOfWeek.entries.associateWith { 0 }.toMutableMap()
        rawList.orEmpty().forEach { favorite ->
            favorite.playCountByDay.forEach { (date, count) ->
                val dayOfWeek = date.dayOfWeek
                counts[dayOfWeek] = counts.getValue(dayOfWeek) + count
            }
        }
        return counts
    }


    fun changeToNextQuarter() {
        quarter = when(quarter) {
            FIRST_QUARTER -> SECOND_QUARTER
            SECOND_QUARTER -> THIRD_QUARTER
            THIRD_QUARTER -> FOURTH_QUARTER
            FOURTH_QUARTER -> NO_QUARTER
            else -> FIRST_QUARTER
        }
    }

    companion object {
        const val LAST_MONTH = 0
        const val LAST_3_MONTH = 1
        const val LAST_YEAR = 2
        const val SPECIFIC_YEAR = 3

        const val NO_QUARTER = "QUARTER_0"
        const val FIRST_QUARTER = "QUARTER_1"
        const val SECOND_QUARTER = "QUARTER_2"
        const val THIRD_QUARTER = "QUARTER_3"
        const val FOURTH_QUARTER = "QUARTER_4"
        const val TAG = "PlayCountChartViewModel"
    }


}
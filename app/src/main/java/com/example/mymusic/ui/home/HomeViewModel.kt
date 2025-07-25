package com.example.mymusic.ui.home

import android.R
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.home.recommend.recommendFavorites
import com.kizitonwose.calendar.core.yearMonth
import java.time.LocalDate
import java.time.YearMonth

class HomeViewModel : ViewModel() {

    private val _onThisDayReleases :MutableLiveData<List<Favorite>> = MutableLiveData(emptyList())
    val onThisDayReleases get() = _onThisDayReleases

    private val _onThisMonthReleases : MutableLiveData<List<Favorite>> = MutableLiveData(emptyList())
    val onThisMonthReleases get() = _onThisMonthReleases

    var favoriteList : List<Favorite>? = null

    private val _hotNowTop5 : MutableLiveData<List<Favorite>> = MutableLiveData(emptyList())
    val hotNowTop5 get() = _hotNowTop5

    private val _comebackTop5 : MutableLiveData<List<Favorite>> = MutableLiveData(emptyList())
    val comebackTop5 get() = _comebackTop5

    var dropWindowPresetWeek: Int = 12
    var maxCountDiff: Int = 0


    fun getRecommendedFavorites(rawList: List<Favorite>?) {
        if (rawList.isNullOrEmpty()) return
        val recommended = recommendFavorites(
            rawList = rawList,
            today = LocalDate.now(),
            minDropThreshold = 4,
            this
        )
        recommended.hotNowTop5.let { if (it.isNotEmpty()) _hotNowTop5.value = it }
        recommended.comebackTop5.let { if (it.isNotEmpty()) _comebackTop5.value = it }
    }

    fun checkOnThisDayReleases(favoriteList: List<Favorite>?) {
        Log.d(TAG, "check on this day releases " + favoriteList?.size)
        if (favoriteList == null) return
        if (this.favoriteList === favoriteList) {
            // ✅ 새 화면이 구독할 때 즉시 반영되도록 재발행
            _onThisDayReleases.value = _onThisDayReleases.value
            return
        }

        this.favoriteList = favoriteList

        val today = LocalDate.now()
        _onThisMonthReleases.value = favoriteList.filter {
            it.track.releaseDate?.let {
                val parsed = LocalDate.parse(it)
                parsed.month == today.month
            } ?: false
        }.shuffled()

        _onThisDayReleases.value = onThisMonthReleases.value?.sortedBy { favorite ->
            val parsed = LocalDate.parse(favorite.releaseDate)
            parsed.dayOfMonth
        }?.filter {
            it.track.releaseDate?.let {
                val parsed = LocalDate.parse(it)
                parsed.dayOfMonth == today.dayOfMonth
            } ?: false
        }

        Log.d(TAG, "on this day size: " + onThisDayReleases.value?.size)
    }



    companion object {
        const val TAG = "HomeViewModel"
    }

}
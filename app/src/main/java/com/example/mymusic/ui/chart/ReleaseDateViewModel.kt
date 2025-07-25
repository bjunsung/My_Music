package com.example.mymusic.ui.myCalendar

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.model.Favorite
import com.github.mikephil.charting.data.Entry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

class ReleaseDateViewModel (application: Application): AndroidViewModel(application) {
    private var favoriteList: List<Favorite> = emptyList()
    private val favoriteRepository = FavoriteSongRepository(application)
    private var startYear by Delegates.notNull<Int>()
    private var endYear by Delegates.notNull<Int>()
    lateinit var favListForYearMap: MutableMap<Int, List<Favorite>>

    var chartEntries: List<Entry> = emptyList()
    var chartLabels: List<String> = emptyList()

    var seeMoreState: Boolean = false
    var onFocused = false
    var shuffledList: List<Favorite> = listOf()
    var focusedPosition: Int = 0
    lateinit var focusedTrack: Favorite

    fun getFavoriteList() : List<Favorite> {
        return favoriteList
    }

    fun updateFavoriteList(favList: List<Favorite>) {
        this.favoriteList = favList
    }

    interface FavoriteLoadCallback {
        fun onSuccess(favList: List<Favorite>)
        fun onDuplicated()
        fun onFailure(message: String?)
    }

    fun loadFavoriteList(callback: FavoriteLoadCallback?) {
        Thread{
            try{
                val newList: List<Favorite> = favoriteRepository.allFavoriteTracks
                if (newList == favoriteList) {
                    Log.d(TAG, "load favorite list success but already exist in viewModel, so do not update")
                    //callback?.onDuplicated()
                    callback?.onSuccess(newList)
                }
                else{
                    Log.d(TAG, "load favorite list success")
                    callback?.onSuccess(newList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "load favorite list failed")
                callback?.onFailure(e.message)
            }
        }.start()
    }



    fun getListOfYear(fav: List<Favorite>, year: Int): List<Favorite> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 실제 포맷에 맞게 수정
        Log.d(TAG, "favorite received size: " + favoriteList.size)

        return favoriteList.filter { favorite ->
            val releaseDateStr = favorite.track?.releaseDate
            if (!releaseDateStr.isNullOrBlank()) {
                try {
                    val releaseDate = LocalDate.parse(releaseDateStr, formatter)
                    releaseDate.year == year
                } catch (e: Exception) {
                    Log.e("ParseError", "Invalid date: $releaseDateStr", e)
                    false
                }
            } else {
                false
            }
        }
    }

    companion object{
        const val TAG = "ReleaseDateViewModel"
    }


    fun updateStartYear(year: Int){
        this.startYear = year
    }

    fun fetchStartYear(): Int {
        return this.startYear
    }

    fun updateEndYear(year: Int){
        this.endYear = year
    }

    fun fetchEndYear(): Int {
        return this.endYear
    }



}
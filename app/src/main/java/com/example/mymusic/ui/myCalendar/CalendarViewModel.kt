package com.example.mymusic.ui.myCalendar

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.model.Favorite
import java.time.LocalDate // java.time.LocalDate 사용
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap // 스레드 안전한 맵 사용 권장

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    // Repository에서 불러온 전체 즐겨찾기 목록
    var favoriteList: List<Favorite> = emptyList()

    // 캘린더에 표시되지 않는 즐겨찾기 목록 (현재 캘린더 로직에서는 직접 사용되지 않음)
    var excludedFavorites: List<Favorite> = emptyList()

    private val favoriteRepository = FavoriteSongRepository(application)

    // 날짜별 이벤트를 미리 계산하여 저장할 맵
    // Key: LocalDate (연도를 2000으로 고정하여 월/일만으로 그룹화)
    // Value: 해당 날짜의 Favorite 리스트
    private val _eventsByDate: ConcurrentHashMap<LocalDate, MutableList<Favorite>> = ConcurrentHashMap()
    val eventsByDate: Map<LocalDate, List<Favorite>> get() = _eventsByDate // 외부에 읽기 전용으로 노출

    /**
     * 모든 Favorite 리스트를 기반으로 날짜별 이벤트를 미리 계산하는 함수.
     * 이 함수는 데이터를 로드하거나 변경할 때 한 번만 호출되어 맵을 채웁니다.
     */
    private fun precomputeEventsByDate(favList: List<Favorite>) {
        _eventsByDate.clear() // 새로운 데이터가 들어오면 기존 맵을 초기화
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 날짜 파싱을 위한 포맷터

        favList.forEach { favorite ->
            val releaseDateStr = favorite.track?.releaseDate
            if (!releaseDateStr.isNullOrBlank()) {
                try {
                    val releaseDate = LocalDate.parse(releaseDateStr, formatter)
                    // 연도를 2000으로 고정하여, 다른 연도라도 같은 월/일이면 동일한 키로 묶이도록 함
                    val dateWithoutYear = LocalDate.of(2000, releaseDate.month, releaseDate.dayOfMonth)
                    // 해당 날짜(월/일)에 이벤트 리스트가 없으면 새로 생성하고, 있으면 기존 리스트에 Favorite 추가
                    _eventsByDate.computeIfAbsent(dateWithoutYear) { mutableListOf() }.add(favorite)
                } catch (e: Exception) {
                    Log.e("ParseError", "Invalid date: $releaseDateStr in precomputeEventsByDate", e)
                }
            }
        }
        Log.d(TAG, "Precomputed events for ${_eventsByDate.size} unique dates.")
    }

    interface FavoriteUpdateCallback {
        fun onSuccess(favList: List<Favorite>)
        fun onDuplicated()
        fun onFailure(message: String?)
    }

    /**
     * 즐겨찾기 목록을 로드하는 함수.
     * 데이터가 변경되면 미리 계산하는 precomputeEventsByDate()를 호출합니다.
     */
    fun loadFavoriteList(callback: FavoriteUpdateCallback) {
        Thread { // 백그라운드 스레드에서 데이터 로드
            try {
                val newList: List<Favorite> = favoriteRepository.allFavoriteTracks
                if (newList == favoriteList) {
                    // 데이터가 ViewModel에 이미 동일하게 존재하면 onDuplicated 호출 (UI 갱신만 필요할 때)
                    Log.d(TAG, "load favorite list success but already exist in viewModel")
                    callback.onDuplicated()
                } else {
                    // 새로운 데이터가 로드되면 ViewModel의 favoriteList 업데이트
                    Log.d(TAG, "load favorite list success, updating ViewModel and precomputing events")
                    favoriteList = newList
                    precomputeEventsByDate(newList) // 로드된 새 데이터를 기반으로 맵을 미리 계산
                    callback.onSuccess(newList) // 성공 콜백 호출
                }
            } catch (e: Exception) {
                Log.e(TAG, "load favorite list failed", e)
                callback.onFailure(e.message) // 실패 콜백 호출
            }
        }.start()
    }

    /**
     * 주어진 날짜(월/일)에 해당하는 이벤트를 미리 계산된 맵에서 가져오는 함수.
     * 이 함수는 맵에서 조회만 하므로 매우 빠릅니다.
     */
    fun getEventsForDate(date: LocalDate): List<Favorite> {
        // 검색할 때도 연도를 2000으로 고정하여 맵의 키와 일치시킴
        val dateWithoutYear = LocalDate.of(2000, date.month, date.dayOfMonth)
        val events = _eventsByDate[dateWithoutYear] ?: emptyList() // 맵에서 해당 키의 리스트를 가져오거나 없으면 빈 리스트 반환
        return events
    }

    companion object {
        const val TAG = "CalendarViewModel"
    }
}
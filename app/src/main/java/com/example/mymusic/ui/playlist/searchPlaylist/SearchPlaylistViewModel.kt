package com.example.mymusic.ui.playlist.searchPlaylist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SearchPlaylistViewModel(application: Application) : AndroidViewModel(application) {
    // 전체 즐겨찾기 (오디오 URI 있는 것만)
    private val _favoriteList = MutableLiveData<List<Favorite>?>()
    val favoriteList get() = _favoriteList

    // 관련곡 리스트 (선택된 곡의 아티스트 기준)
    private val _relatedList = MutableLiveData<List<Favorite>?>()
    val relatedList get() = _relatedList

    private val favoriteSongRepository by lazy { FavoriteSongRepository(application) }
    private val playlistRepository by lazy { PlaylistRepository(application) }

    var playlist: Playlist? = null
    private val _alreadyExistSet = MutableLiveData<MutableSet<String>>(mutableSetOf())
    val alreadyExistSet get() = _alreadyExistSet


    /** 순서 유지용 (표시/재생용 큐) */
    val selectedList: MutableList<Favorite> = mutableListOf()

    /** 체크 상태의 단일 소스(진실) */
    val selectedIds: MutableSet<String> = mutableSetOf()

    fun loadFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = favoriteSongRepository.allFavoriteTracks
                .filter { !it.audioUri.isNullOrEmpty() }
                .reversed()
            _favoriteList.postValue(list)
        }
    }

    fun initializeAlreadyExistSet(alreadyExist: MutableSet<String>) {
        _alreadyExistSet.value = alreadyExist
    }

    /** ===== 선택 로직 (Set을 기준으로) ===== */

    /** 체크 ON: Set에 추가 + 순서리스트에 (중복 제거 후) 뒤에 추가 */
    fun select(fav: Favorite) {
        val id = fav.track.trackId
        if (selectedIds.add(id)) {
            // 순서 리스트 중복 제거 후 추가
            selectedList.removeAll { it.track.trackId == id }
            selectedList.add(fav)
            // 관련곡 갱신
            filterRelatedTrack(fav)
            //Log.d(TAG, "select -> ids=$selectedIds, list=$selectedList")
        }
    }

    /** 체크 OFF: Set에서 제거 + 순서리스트에서도 제거 */
    fun unselect(fav: Favorite) {
        val id = fav.track.trackId
        if (selectedIds.remove(id)) {
            selectedList.removeAll { it.track.trackId == id }
            // 관련곡은 마지막 선택곡 기준 유지하고 싶으면 건드리지 않음
            _relatedList.value = _relatedList.value?.toList()
            //Log.d(TAG, "unselect -> ids=$selectedIds, list=$selectedList")
        }
    }

    fun unselectAll() {
        selectedList.clear()
        selectedIds.clear()
        _relatedList.value = _relatedList.value?.toList()
    }

    /** 토글: Set 기준으로 */
    fun toggleSelection(fav: Favorite) {
        if (isSelected(fav.track.trackId)) unselect(fav) else select(fav)
    }

    fun isSelected(trackId: String): Boolean = selectedIds.contains(trackId)

    /** 외부에서 여러 개 한꺼번에 세팅할 때 (예: 초기화/복원) */
    fun setSelected(ids: Collection<String>, ordered: List<Favorite> = emptyList()) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        selectedList.clear()
        if (ordered.isNotEmpty()) {
            // 전달받은 순서 유지
            ordered.forEach { if (selectedIds.contains(it.track.trackId)) selectedList.add(it) }
        } else {
            // favoriteList 기준으로 순서 구성
            _favoriteList.value.orEmpty().forEach { if (selectedIds.contains(it.track.trackId)) selectedList.add(it) }
        }
        // 관련곡은 마지막 선택 기준으로 갱신하고 싶다면:
        selectedList.lastOrNull()?.let { filterRelatedTrack(it) } ?: run { _relatedList.value = relatedList.value }
    }

    /** 관련곡: 선택한 곡의 아티스트 기준 */
    fun filterRelatedTrack(fav: Favorite) {
        _relatedList.value = favoriteList.value
            ?.filter { it.track.artistId == fav.track.artistId }
            ?.sortedBy {
                runCatching { LocalDate.parse(it.track.releaseDate) }.getOrNull()
            }
    }

    fun addTracksToPlaylist() {
        viewModelScope.launch(Dispatchers.IO) {
            val id = playlist?.playlistId
            if (id != null && selectedList.isNotEmpty()) {
                val selectedIds = selectedList.map { it.track.trackId }
                try {
                    playlistRepository.addTracksIgnoreDuplicates(
                        playlistId =  id,
                        toAdd = selectedIds
                    )
                    withContext(Dispatchers.Main) {
                        addToAlreadyExist(selectedIds)
                        unselectAll()
                    }
                } catch (e: Exception) {
                    // TODO: 에러 핸들링/UI 알림
                }
            }
        }
    }

    fun addToAlreadyExist(ids: List<String>) {
        val current = alreadyExistSet.value ?: mutableSetOf()
        current.addAll(ids)
        _alreadyExistSet.value = current.toMutableSet() // 새로운 Set으로 교체
    }

    fun removeFromAlreadyExist(ids: List<String>) {
        val current = alreadyExistSet.value ?: mutableSetOf()
        current.removeAll(ids.toSet())
        _alreadyExistSet.value = current.toMutableSet()
    }

    companion object { const val TAG = "SearchPlaylistViewModel" }
}
package com.example.mymusic.ui.setting.hiddenTracks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.model.Favorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HiddenTrackViewModel(application: Application) : AndroidViewModel(application){

    private val _hiddenTracks: MutableLiveData<MutableList<Favorite>> = MutableLiveData(mutableListOf<Favorite>())
    val hiddenTracks: LiveData<MutableList<Favorite>> get() = _hiddenTracks
    val favoriteRepository : FavoriteSongRepository by lazy { FavoriteSongRepository(application) }

    fun loadHiddenTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val hiddenRaw = favoriteRepository.hiddenTracks
            _hiddenTracks.postValue(hiddenRaw)
        }
    }

    fun deleteTrack(favorite: Favorite) {
        viewModelScope.launch(Dispatchers.IO){
            favoriteRepository.deleteFavoritesByIds(listOf<String>(favorite.track.trackId))
            withContext(Dispatchers.Main) {
                val updated = _hiddenTracks.value?.filterNot { it.track.trackId == favorite.track.trackId }?.toMutableList() ?: mutableListOf<Favorite>()
                _hiddenTracks.value = updated
            }
        }

    }

    fun restoreVisibility(favorite: Favorite) {
        viewModelScope.launch(Dispatchers.IO) {
            favorite.isHidden = false

            favoriteRepository.updateFavoriteSongExceptPlayCount(
                favorite,
                object : FavoriteSongRepository.FavoriteDbCallback {
                    override fun onSuccess() {
                        // 메인 스레드에서 새 리스트로 갱신
                        viewModelScope.launch(Dispatchers.Main) {
                            val current = _hiddenTracks.value.orEmpty()
                            val updated = current.filterNot {
                                it.track.trackId == favorite.track.trackId
                            }.toMutableList()
                            _hiddenTracks.value = updated
                        }
                    }
                    override fun onFailure() {
                        // 롤백
                        favorite.isHidden = true
                    }
                }
            )
        }
    }

}



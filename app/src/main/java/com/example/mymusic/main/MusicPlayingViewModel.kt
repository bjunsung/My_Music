package com.example.mymusic.main

import android.app.Application
import android.widget.ImageView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.main.playtime.ContributionDay
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayingViewModel(application: Application) : AndroidViewModel(application) {
    private val _rotationAngle = MutableLiveData(0f)
    val rotationAngle: LiveData<Float> get() = _rotationAngle

    private val _currentPage = MutableLiveData(0)
    val currentPage get(): LiveData<Int> = _currentPage

    val _lastData = MutableLiveData<List<ContributionDay>>()
    val lastData get() = _lastData

    fun saveCurrentPageValue(page: Int){
        _currentPage.value = page
    }

    fun setRotationAngle(angle: Float) {
        _rotationAngle.value = angle
    }

    private val _requestDismiss = MutableLiveData(false)
    val requestDismiss get() = _requestDismiss

    fun requestDismiss(dismiss: Boolean) {
        this._requestDismiss.value = dismiss
    }

    private val favoriteRepository by lazy { FavoriteSongRepository(application) }

    suspend fun loadFavoritesByIds(reorderedIds: List<String>): List<Favorite> {
        return favoriteRepository.getFavoritesByIds(reorderedIds)
    }


    var artworkImage: ImageView? = null


    private var allPlaylists: List<Playlist>? = null
    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(application) }

    suspend fun findContainingPlaylist(trackId: String): List<Playlist> {
        val cached = allPlaylists
        val playlists = if (cached != null) {
            cached
        } else {
            withContext(Dispatchers.IO) {
                playlistRepository.getAllWithFavorites().also { allPlaylists = it }
            }
        }
        return playlists.filter { it.trackIds.contains(trackId) }
    }


}
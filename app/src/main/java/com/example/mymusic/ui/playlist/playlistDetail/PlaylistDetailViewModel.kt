package com.example.mymusic.ui.playlist.playlistDetail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailViewModel(application: Application) : AndroidViewModel(application) {
    val _playlist = MutableLiveData<Playlist>(null)
    val playlist get() = _playlist
    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(application) }

    val isNonEditable: Boolean
        get() = playlist.value?.playlistId == PlaylistRepository.PLAYLIST_ID_RECENTLY_PLAYED


    private val _selectedIdsSet = MutableLiveData<MutableSet<String>>(mutableSetOf())
    val selectedIdsSet get() = _selectedIdsSet
    fun updateSelectedIdSet(newIds: Set<String>) {
        _selectedIdsSet.value = newIds.toMutableSet()
    }

    fun removeTracks(playlistId: String, toRemove: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = kotlin.runCatching { playlistRepository.removeTracks(playlistId = playlistId, toRemove = toRemove) }.isSuccess
            if (ok) {
                withContext(Dispatchers.Main) {
                    val cleaned = (_selectedIdsSet.value ?: emptySet()).toMutableSet().apply {
                        removeAll(toRemove)
                    }
                    _selectedIdsSet.value = cleaned
                    Log.d(TAG, "cleaned: ${cleaned}")
                    Log.d(TAG, "selected live data : ${selectedIdsSet.value}")
                }
                playlistRepository.getByIdWithFavorites(playlistId)?.let { _playlist.postValue(it) }
            }
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        val oldName = playlist.playlistName
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == playlist.playlistName) return

        // favorites 따로 저장해두기 (copy()하면 날아가니까)
        val favoritesBackup = playlist.favorites

        // 2) DB 업데이트 (실패 시 롤백)
        viewModelScope.launch(Dispatchers.IO) {
            val updated = playlist.copy(playlistName = trimmed, favorites = favoritesBackup)
            val ok = runCatching { playlistRepository.updatePlaylist(updated) }.isSuccess

            withContext(Dispatchers.Main) {
                var cur = _playlist.value ?: return@withContext

                if (!ok) {
                    // 롤백
                    cur = cur.copy(playlistName = oldName).also {
                        it.favorites = favoritesBackup
                    }
                } else {
                    // DB 반영 성공 시 favorites 그대로 유지
                    cur = cur.copy(playlistName = trimmed).also {
                        it.favorites = favoritesBackup
                    }
                }
                _playlist.value = cur
            }
        }
    }

    fun saveOrder(playlistId: String, reorderedIds: List<String>) {
        var reordered = playlist.value?.copy(trackIds = reorderedIds.toMutableList()) ?: return
        if (playlistId != reordered.playlistId) return
        reordered = reorderFavoritesByTrackIds(reordered)
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { playlistRepository.updatePlaylist(reordered) }.isSuccess
            if (ok) playlist.postValue(reordered)
        }

    }

    private fun reorderFavoritesByTrackIds(playlist: Playlist): Playlist {
        if (playlist.trackIds.isEmpty() || playlist.favorites.isEmpty()) return playlist

        // favorites를 trackIds 순서대로 정렬
        val idToFav = playlist.favorites.associateBy { it.track.trackId }
        val reorderedFavs = playlist.trackIds.mapNotNull { idToFav[it] }.toMutableList()

        return playlist.deepCopy().apply {
            favorites = reorderedFavs
        }
    }

    fun synchronizePlaylist() {
        playlist.value?.playlistId?.let { id ->
            viewModelScope.launch(Dispatchers.IO) {
                playlistRepository.getByIdWithFavorites(id)?.let { playlistSync ->
                    if (playlist.value != playlistSync)
                        _playlist.postValue(playlistSync)
                }
            }
        }
    }

    private var allPlaylists: List<Playlist>? = null

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


    var focusedTrackId: String? = null



    companion object {
        const val TAG = "PlaylistDetailViewModel"
    }

}
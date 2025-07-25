package com.example.mymusic.ui.playlist

import android.app.Application
import android.util.Log
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>> get() = _playlists

    private val playlistRepository by lazy { PlaylistRepository(application) }
    private val favoriteSongRepository by lazy { FavoriteSongRepository(application) }

    val playlistNameSet = mutableSetOf<String>()

    var sharedElementTargetPlaylistId: String? = null

    fun loadPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = playlistRepository.getAllWithFavorites()
            _playlists.postValue(playlists)
        }
    }

    fun createNewPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistNameSet.add(playlist.playlistName)
            playlistRepository.create(playlist)
        }
    }

    fun deletePlaylist(playlistId: String, playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {

            val result = playlistRepository.delete(playlistId = playlistId)
            if (result) {
              withContext(Dispatchers.Main) {
                  // DB 반영 성공 시 set 에서 이름 제거
                  playlistNameSet.remove(playlistName)
                  _playlists.value = _playlists.value?.filterNot { it.playlistId == playlistId }
              }
            }
        }
    }



    fun renamePlaylist(playlist: Playlist, newName: String) {
        val oldName = playlist.playlistName
        Log.d(TAG, "playlist: $playlist")
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == playlist.playlistName) return

        // favorites 따로 저장해두기 (copy()하면 날아가니까)
        val favoritesBackup = playlist.favorites

        // 1) UI 낙관적 업데이트
        val current = _playlists.value?.toMutableList() ?: mutableListOf()
        val idx = current.indexOfFirst { it.playlistId == playlist.playlistId }
        if (idx != -1) {
            val old = current[idx]
            current[idx] = old.copy(playlistName = trimmed, favorites = favoritesBackup)
            _playlists.value = current
        }

        // 2) DB 업데이트 (실패 시 롤백)
        viewModelScope.launch(Dispatchers.IO) {
            val updated = playlist.copy(playlistName = trimmed, favorites = favoritesBackup)

            val ok = runCatching { playlistRepository.updatePlaylist(updated) }.isSuccess

            withContext(Dispatchers.Main) {
                val cur = _playlists.value?.toMutableList() ?: return@withContext
                val i = cur.indexOfFirst { it.playlistId == playlist.playlistId }

                if (i != -1) {
                    if (!ok) {
                        // 롤백
                        cur[i] = cur[i].copy(playlistName = oldName).also {
                            it.favorites = favoritesBackup
                        }
                    } else {
                        // DB 반영 성공 시 favorites 그대로 유지
                        cur[i] = cur[i].copy(playlistName = trimmed).also {
                            it.favorites = favoritesBackup
                        }

                        // DB 반영 성공 시 name set update
                        playlistNameSet.remove(oldName)
                        playlistNameSet.add(newName)
                    }
                    _playlists.value = cur
                }
            }
        }
    }


    companion object {
        const val TAG = "PlaylistLibraryViewModel"
    }

}

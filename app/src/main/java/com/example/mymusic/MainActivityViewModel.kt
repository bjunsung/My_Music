package com.example.mymusic

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mymusic.model.Favorite
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTrack = MutableLiveData<Favorite?>(null)
    val currentTrack: LiveData<Favorite?> get() = _currentTrack

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    var exoPlayer: ExoPlayer? = ExoPlayer.Builder(application).build()
    var lastPlayedTrack: Favorite? = null

    private var playlist: List<Favorite> = emptyList()
    private var currentIndex = 0
    private val _repeatMode = MutableLiveData(Player.REPEAT_MODE_OFF)
    val repeatMode: LiveData<Int> get() = _repeatMode

    init {
        initPlayer()
    }

    /** 플레이리스트와 시작 인덱스를 설정하고 재생 시작 */
    fun setPlaylist(playlist: List<Favorite>, startPosition: Int) {
        if (exoPlayer == null) return

        this.playlist = playlist
        currentIndex = startPosition.coerceIn(0, playlist.lastIndex)
        _currentTrack.value = playlist.get(startPosition)

        exoPlayer!!.clearMediaItems()

        val mediaItems = playlist.map {MediaItem.fromUri(it.audioUri)}
        exoPlayer!!.setMediaItems(mediaItems, startPosition, 0)
        exoPlayer!!.prepare()

        exoPlayer!!.playWhenReady = true
        _isPlaying.value = true
        Log.d(TAG, "isplaying " + isPlaying.value)

    }

    /** 플레이어 초기화 */
    private fun initPlayer() {
        if (exoPlayer == null) return
        exoPlayer!!.clearMediaItems()
        val mediaItems = playlist.map { MediaItem.fromUri(it.audioUri) } // track.url은 Favorite 모델에서 곡 URL
        exoPlayer!!.setMediaItems(mediaItems)
        exoPlayer!!.prepare()



        // 곡 끝나면 다음 곡 자동 재생
        exoPlayer!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    // 자연스럽게 곡이 끝나서 넘어간 경우
                    playNext()
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    if ( repeatMode.value == Player.REPEAT_MODE_OFF && currentIndex == playlist.lastIndex) {
                        _isPlaying.value = false
                    }
                }
            }
        })



    }

    /** 반복 모드 변경 */
    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer?.repeatMode = nextMode
    }

    /** 특정 인덱스 재생 */
    fun playAt(index: Int) {
        if (playlist.isEmpty()) return
        currentIndex = index.coerceIn(0, playlist.lastIndex)
        _currentTrack.value = playlist[currentIndex]
        exoPlayer?.seekTo(currentIndex, 0)
        exoPlayer?.playWhenReady = true
        _isPlaying.value = true
        lastPlayedTrack = playlist[currentIndex]
    }

    /** 다음 곡 */
    fun playNext() {
        if (playlist.isEmpty()) return
        when (repeatMode.value) {
            Player.REPEAT_MODE_OFF -> {
                // 마지막 곡이면 그냥 멈춤
                if (currentIndex < playlist.lastIndex) {
                    playAt(currentIndex + 1)
                }
            }
            Player.REPEAT_MODE_ALL -> {
                // 마지막 곡이면 첫 곡으로
                playAt((currentIndex + 1) % playlist.size)
            }
            Player.REPEAT_MODE_ONE -> {
                // 같은 곡 반복
                playAt(currentIndex)
            }
        }
    }



    /** 이전 곡 */
    fun playPrevious() {
        if (currentIndex - 1 >= 0) {
            playAt(currentIndex - 1)
        }
    }

    /** 일시정지/재생 토글 */
    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                it.play()
                _isPlaying.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
    
    
    companion object {
        const val TAG = "MainActivityViewModel"
    }
}

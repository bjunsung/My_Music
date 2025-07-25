package com.example.mymusic

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.model.Favorite
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTrack = MutableLiveData<Favorite?>(null)
    val currentTrack: LiveData<Favorite?> get() = _currentTrack

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    var exoPlayer: ExoPlayer? = ExoPlayer.Builder(application).build()
    var lastPlayedTrackIndex: Int = 0


    private var _playlist = MutableLiveData<List<Favorite>>(emptyList())

    val playlist : LiveData<List<Favorite>> get() = _playlist

    var currentIndex = 0
    private val _repeatMode = MutableLiveData(Player.REPEAT_MODE_OFF)
    val repeatMode: LiveData<Int> get() = _repeatMode
    var _trackDuration: MutableLiveData<Int> = MutableLiveData(0)
    val trackDuration: LiveData<Int> get() = _trackDuration

    // <<< [수정] 재생 시간 측정 관련 변수들
    private val handler = Handler(Looper.getMainLooper())
    private var listeningTimeUpdater: Runnable? = null
    private var listenedTimeMs = 0L
    private var currentTrackDurationMs = 0L // 카운트할 트랙의 전체 길이

    private val _totalPlayCountInARow = MutableLiveData<Int>(0)
    val totalPlayCountInARow : LiveData<Int> get() = _totalPlayCountInARow!!

    val favoriteSongRepository: FavoriteSongRepository by lazy { FavoriteSongRepository(application)}


    init {
        initPlayer()
    }

    /** 플레이리스트와 시작 인덱스를 설정하고 재생 시작 */
    fun setPlaylist(playlist: List<Favorite>, startPosition: Int) {
        if (exoPlayer == null) return
        shuffledMode = false

        _totalPlayCountInARow.value = 0

        _playlist.value = playlist
        currentIndex = startPosition.coerceIn(0, playlist.lastIndex)
        _currentTrack.value = playlist.get(startPosition)

        exoPlayer!!.clearMediaItems()

        val mediaItems = playlist.map {MediaItem.fromUri(it.audioUri)}
        Log.d(TAG, "exoplayer: " + exoPlayer)
        exoPlayer!!.setMediaItems(mediaItems)
        exoPlayer!!.prepare()
        Log.d(TAG, "play at from first play of playlist")
        playAt(startPosition)

        Log.d(TAG, "isplaying " + isPlaying.value)

    }

    /** 플레이어 초기화 */
    private fun initPlayer() {
        if (exoPlayer == null) return
        exoPlayer!!.clearMediaItems()
        val mediaItems = playlist.value!!.map { MediaItem.fromUri(it.audioUri) } // track.url은 Favorite 모델에서 곡 URL
        exoPlayer!!.setMediaItems(mediaItems)
        exoPlayer!!.prepare()


        // 곡 끝나면 다음 곡 자동 재생
        exoPlayer!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

                super.onMediaItemTransition(mediaItem, reason)
                val newIndex = exoPlayer?.currentMediaItemIndex ?: 0
                if (currentIndex == newIndex) return //유저 개입으로 playat 이 이미 호출된 경우 무시 (자연스럽게 넘어가는 경우에만 playAt  으로 ui 업데이트 가능하게)
                Log.d(TAG, "play at from onMediaItemTransition Listener")
                playAt(newIndex)  // 트랙이 바뀔 때 항상 playAt() 호출
            }


            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                // <<< [수정] isPlaying 상태에 따라 시간 측정 핸들러를 시작/중지
                if (isPlaying) {
                    startListeningTracker()
                } else {
                    stopListeningTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    if ( repeatMode.value == Player.REPEAT_MODE_OFF && currentIndex == playlist.value!!.lastIndex) {
                        _isPlaying.value = false
                    }
                }
                if (playbackState == Player.STATE_READY) {
                    val newDuration = exoPlayer?.duration?.toInt() ?: 0
                    _trackDuration.postValue(newDuration) // ViewModel에서 새로운 LiveData로 전달
                    // <<< [추가] 새 트랙의 전체 길이를 변수에 저장
                    currentTrackDurationMs = newDuration.toLong()
                }
            }
        })



    }

    // <<< [수정] 시간 측정 시작 로직
    private fun startListeningTracker() {
        // 이미 실행 중이면 중복 실행 방지
        if (listeningTimeUpdater != null) return

        listeningTimeUpdater = Runnable {
            listenedTimeMs += 500 // 0.5초마다 누적
            // 로그를 자주 찍으면 성능에 영향 줄 수 있으니, 디버깅 시에만 활용하세요.
            // Log.d(TAG, "Accumulated time: $listenedTimeMs ms")
            handler.postDelayed(listeningTimeUpdater!!, 500)
        }
        handler.post(listeningTimeUpdater!!)
        Log.d(TAG, "▶️ Listening tracker started, played time: " +listenedTimeMs / 1000 + " s")
    }

    // <<< [수정] 시간 측정 중지 로직
    private fun stopListeningTracker() {
        listeningTimeUpdater?.let { handler.removeCallbacks(it) }
        listeningTimeUpdater = null
        Log.d(TAG, "⏹️ Listening tracker stopped, played time: " + listenedTimeMs / 1000 + " s")
    }

    // <<< [수정] 재생 카운트 체크 및 초기화 로직
    private fun checkPlayCount() {
        // 유효한 트랙 길이를 가지고 있을 때만 체크
        if (currentTrackDurationMs <= 0) {
            Log.d(TAG, "Check skipped: Invalid duration ($currentTrackDurationMs)")
            resetListeningTime() // 시간 초기화
            return
        }
        val lastPlayedTrack = playlist.value?.get(lastPlayedTrackIndex)
        if (listenedTimeMs >= currentTrackDurationMs * 2 / 3) {
            val today:String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            Log.d(TAG, "✅ COUNT! Date: $today, Listened: $listenedTimeMs, Total: $currentTrackDurationMs, track name: ${lastPlayedTrack?.title ?: "name not found"}")
            updatePlayCount()
        } else {
            Log.d(TAG, "❌ NOT COUNTED. Listened: $listenedTimeMs, Total: $currentTrackDurationMs, track name: ${lastPlayedTrack?.title ?: "name not found"}")
        }

        // 다음 곡을 위해 누적 시간 초기화
        resetListeningTime()
    }

    private fun updatePlayCount() {
        val today:String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastPlayed = playlist.value?.get(lastPlayedTrackIndex)
        lastPlayed?.let {
            lastPlayed.addPlayCount(today)
            viewModelScope.launch(Dispatchers.IO) {
                favoriteSongRepository.updateFavoriteSong(
                    lastPlayed,
                    object : FavoriteSongRepository.FavoriteDbCallback {
                        override fun onSuccess() {
                            Log.d(TAG, "count updated, current count: ${lastPlayed.playCount}")
                        }

                        override fun onFailure() {
                            Log.d(TAG, "count update failed for ${lastPlayed.track.trackName}")
                        }

                    })
            }
        }
    }

    // <<< [추가] 누적 시간 초기화 함수
    private fun resetListeningTime() {
        stopListeningTracker()
        listenedTimeMs = 0L
        // currentTrackDurationMs는 onPlaybackStateChanged에서 갱신되므로 여기서 0으로 만들지 않음
    }


    var shuffledMode = false

    fun shufflePlayList() {
        if (playlist.value!!.isEmpty()) return
        val newPlayList = playlist.value!!.shuffled()
        setPlaylist(newPlayList, 0)
        shuffledMode = true
    }


    /** 반복 모드 변경 */
    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        Log.d(TAG, "repeat mode: " + repeatMode.value)
        exoPlayer?.repeatMode = nextMode
    }

    /** 특정 인덱스 재생 */
    fun playAt(index: Int) {
        if (playlist.value!!.isEmpty()) return
        // <<< [추가] 트랙 넘기기 전에 현재 곡 카운트 체크
        checkPlayCount()
        currentIndex = index.coerceIn(0, playlist.value!!.lastIndex)
        _currentTrack.value = playlist.value!![currentIndex]
        Log.d(TAG, "current track: " + currentTrack.value?.title + " play count: " + currentTrack.value?.playCount)
        exoPlayer?.seekTo(currentIndex, 0)
        exoPlayer?.playWhenReady = true
        _isPlaying.value = true
        lastPlayedTrackIndex = currentIndex

        _totalPlayCountInARow.value = _totalPlayCountInARow.value?.plus(1)
    }

    /** 다음 곡 */
    fun playNext() {
        if (playlist.value!!.isEmpty() || exoPlayer == null) return

        when (repeatMode.value) {
            Player.REPEAT_MODE_OFF -> {
                // 마지막 곡이면 그냥 멈춤
                if (currentIndex < playlist.value!!.lastIndex) {
                    Log.d(TAG, "play at from playNext")
                    playAt(currentIndex + 1)
                }
            }
            Player.REPEAT_MODE_ALL -> {
                // 마지막 곡이면 첫 곡으로
                Log.d(TAG, "play at from playNext")
                playAt((currentIndex + 1) % playlist.value!!.size)

            }
            Player.REPEAT_MODE_ONE -> {
                // 같은 곡 반복
                Log.d(TAG, "play at from playNext")
                playAt(currentIndex)
            }
        }
    }



    /** 이전 곡 */
    fun playPrevious() {
        if (playlist.value!!.isEmpty() || exoPlayer == null) return

        when (repeatMode.value) {
            Player.REPEAT_MODE_OFF -> {
                if (currentIndex > 0) {
                    Log.d(TAG, "play at from playPrevious")
                    playAt(currentIndex - 1)
                }
            }
            Player.REPEAT_MODE_ALL -> {
                Log.d(TAG, "play at from playPrevious")
                playAt((currentIndex + playlist.value!!.size - 1) % playlist.value!!.size)
            }
            Player.REPEAT_MODE_ONE -> {
                Log.d(TAG, "play at from playPrevious")
                playAt(currentIndex)
            }
        }
    }

    /** 일시정지/재생 토글 */
    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                val isAtEnd = it.currentPosition >= (it.duration - 500)
                if (repeatMode.value == Player.REPEAT_MODE_OFF && currentIndex == playlist.value!!.lastIndex && isAtEnd) {
                    Log.d(TAG, "play at from toggle play/pause button click (lastIndex)")
                    playAt(currentIndex)
                }
                it.play()
                _isPlaying.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // <<< [추가] ViewModel 소멸 시 마지막 곡 카운트 체크
        checkPlayCount()

        exoPlayer?.release()
        exoPlayer = null
    }
    
    
    companion object {
        const val TAG = "MainActivityViewModel"
    }
}

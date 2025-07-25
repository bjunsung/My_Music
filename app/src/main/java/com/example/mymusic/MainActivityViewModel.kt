package com.example.mymusic

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.main.MyMediaService
import com.example.mymusic.model.Favorite
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@UnstableApi
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    // --- LiveData ---
    private val _currentTrack = MutableLiveData<Favorite?>(null)
    val currentTrack: LiveData<Favorite?> get() = _currentTrack

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _totalPlayCountInARow = MutableLiveData<Int>(0)
    val totalPlayCountInARow: LiveData<Int> get() = _totalPlayCountInARow

    private var _playlist = MutableLiveData<List<Favorite>>(emptyList())
    val playlist: LiveData<List<Favorite>> get() = _playlist

    private val _repeatMode = MutableLiveData(Player.REPEAT_MODE_OFF)
    val repeatMode: LiveData<Int> get() = _repeatMode

    private val _trackDuration = MutableLiveData(0)
    val trackDuration: LiveData<Int> get() = _trackDuration

    private val _showBottomSheet = MutableLiveData(false)
    val showBottomSheet: LiveData<Boolean> get() = _showBottomSheet

    // --- Properties ---
    var mediaController: MediaController? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    var currentIndex = 0
    var lastPlayedTrackIndex: Int = 0
    var shuffledMode = false

    // --- Play Time Tracking ---
    private val handler = Handler(Looper.getMainLooper())
    private var listeningTimeUpdater: Runnable? = null
    private var listenedTimeMs = 0L
    private var currentTrackDurationMs = 0L

    private val _audioSessionId = MutableLiveData<Int>()
    val audioSessionId: LiveData<Int> get() = _audioSessionId

    // ✅ [추가] 재생 위치를 주기적으로 확인할 핸들러와 Runnable
    private val positionTrackerHandler = Handler(Looper.getMainLooper())
    private var positionTrackerRunnable: Runnable? = null
    private var previousPositionMs = 0L

    // --- Repository ---
    val favoriteSongRepository: FavoriteSongRepository by lazy { FavoriteSongRepository(application) }

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), MyMediaService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            setupControllerListeners()
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun setupControllerListeners() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                Log.d(TAG, "is playing changed " + isPlaying)
                if (isPlaying) {
                    startListeningTracker()
                    startPositionTracker()
                } else {
                    stopListeningTracker()
                    stopPositionTracker()
                }
            }


            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    _audioSessionId.postValue(audioSessionId)
                    Log.d(TAG, "audio session id changed: " + audioSessionId)
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                Log.d(TAG, "onMediaItemTransition: " + reason)
                if (mediaItem == null || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    return
                }

                handleTrackChange()


            }



            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (repeatMode.value == Player.REPEAT_MODE_OFF && currentIndex == (_playlist.value?.lastIndex ?: -1)) {
                        _isPlaying.value = false
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun handleTrackChange() {
        checkPlayCount(currentTrack.value)

        ///mediaController!!.pause()
        ///mediaController!!.play()

        Log.d(TAG, "handle track change")
        Handler(Looper.getMainLooper()).postDelayed({
            _totalPlayCountInARow.value = (totalPlayCountInARow.value ?: 0) + 1
            val newDuration = mediaController?.duration?.takeIf { it != C.TIME_UNSET }?.toInt() ?: 0
            _trackDuration.postValue(newDuration)
            currentTrackDurationMs = newDuration.toLong()
            Log.d(TAG, "duration: " + currentTrackDurationMs)
            lastPlayedTrackIndex = currentIndex
            currentIndex = mediaController?.currentMediaItemIndex ?: 0
            _currentTrack.value = _playlist.value?.getOrNull(currentIndex)
        }, 200)
    }

    fun setPlaylist(playlist: List<Favorite>, startPosition: Int) {
        val controller = this.mediaController ?: return
        shuffledMode = false
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), MyMediaService::class.java))
        _totalPlayCountInARow.value = 0
        _playlist.value = playlist
        currentIndex = startPosition.coerceIn(0, playlist.lastIndex)

        val mediaItems = playlist.map { item ->
            val metadata = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.track.artistName)
                .setArtworkUri(item.track.artworkUrl?.toUri())
                .build()

            MediaItem.Builder()
                .setMediaId(item.track.trackId)
                .setUri(item.audioUri)
                .setMediaMetadata(metadata)
                .build()
        }
        controller.setMediaItems(mediaItems, startPosition, C.TIME_UNSET)
        controller.prepare()
        controller.play()
    }

    fun shufflePlayList() {
        if (playlist.value.isNullOrEmpty()) return
        mediaController!!.clearMediaItems()
        val newPlayList = playlist.value!!.shuffled()
        setPlaylist(newPlayList, 0)
        shuffledMode = true
    }

    fun toggleRepeatMode() {
        val nextMode = when (mediaController?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        mediaController?.repeatMode = nextMode
    }

    fun playNext() {
        if (repeatMode.value == Player.REPEAT_MODE_ONE) {
            handleTrackChange()
            mediaController?.seekToDefaultPosition(currentIndex)
            return
        }
        mediaController?.seekToNextMediaItem()
    }

    fun playPrevious() {
        if (repeatMode.value == Player.REPEAT_MODE_ONE) {
            handleTrackChange()
            mediaController?.seekToDefaultPosition(currentIndex)
            return
        }
        mediaController?.seekToPreviousMediaItem()
    }



    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun requestBottomSheet(isVisible: Boolean) {
        _showBottomSheet.value = isVisible
    }

    private fun startListeningTracker() {
        Log.d(TAG, "start Listening Trackers" + listenedTimeMs)
        if (listeningTimeUpdater != null) return
        listeningTimeUpdater = Runnable {
            listenedTimeMs += 500
            handler.postDelayed(listeningTimeUpdater!!, 500)
           // Log.d(TAG, "listened time ms" + listenedTimeMs)
        }
        handler.post(listeningTimeUpdater!!)
    }

    private fun stopListeningTracker() {
        Log.d(TAG, "stop Listening Trackers" + listenedTimeMs)
        listeningTimeUpdater?.let { handler.removeCallbacks(it) }
        listeningTimeUpdater = null
    }

    private fun checkPlayCount(lastPlayedTrack: Favorite?) {
        Log.d(TAG, "Count check for ${currentTrack.value?.title} and current track duration is ${currentTrackDurationMs}}")
        if (currentTrackDurationMs <= 0) {
            resetListeningTime()
            return
        }

        if (listenedTimeMs >= currentTrackDurationMs * 2 / 3) {
            updatePlayCount(lastPlayedTrack)
        }
        else {
            Log.d(TAG, "❌ NOT COUNTED. Listened: $listenedTimeMs, Total: $currentTrackDurationMs, track name: ${lastPlayedTrack?.title ?: "name not found"}")
        }
        resetListeningTime()
    }

    private fun updatePlayCount(lastPlayedTrack: Favorite?) {
        if (lastPlayedTrack == null) return
        val today = LocalDate.now()
        viewModelScope.launch(Dispatchers.IO) {
            val lastPlayedSync = favoriteSongRepository.getFavoriteSongWithPlayCount(lastPlayedTrack.track.trackId)
            lastPlayedSync.addPlayCount(today)
            favoriteSongRepository.updateFavoriteSongWithPlayCount(lastPlayedSync, object : FavoriteSongRepository.FavoriteDbCallback {
                override fun onSuccess() {
                    Log.d(TAG, "✅ Count updated for ${lastPlayedSync.track.trackName}")
                }
                override fun onFailure() {
                    Log.d(TAG, "❌ Count update failed for ${lastPlayedSync.track.trackName}")
                }
            })
        }
    }

    private fun resetListeningTime() {
        //stopListeningTracker()
        listenedTimeMs = 0L
    }

    override fun onCleared() {
        super.onCleared()
        checkPlayCount(currentTrack.value)
        MediaController.releaseFuture(controllerFuture)
    }

    fun requestAudioSessionId() {
        val controller = this.mediaController ?: return

        // 1. 서비스에 보낼 '질문'을 만듭니다.
        val command = SessionCommand(COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY)

        // 2. 비동기로 질문을 보냅니다.
        val resultFuture = controller.sendCustomCommand(command, Bundle.EMPTY)

        // 3. 서비스에서 '답장'이 오면 실행될 코드를 등록합니다.
        resultFuture.addListener({
            try {
                // 답장(SessionResult)을 받고, 그 안의 Bundle을 엽니다.
                val result = resultFuture.get()
                val resultBundle = result.extras

                // Bundle에서 키를 이용해 sessionId를 꺼냅니다.
                val sessionId = resultBundle.getInt(KEY_AUDIO_SESSION_ID, 0)

                if (sessionId != 0 && sessionId != C.AUDIO_SESSION_ID_UNSET) {
                    // 성공적으로 받은 ID를 LiveData에 업데이트합니다.
                    _audioSessionId.postValue(sessionId)
                }
            } catch (e: Exception) {
                // 에러 처리
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }


    // ✅ [추가] 위치 감시를 시작하는 함수
    private fun startPositionTracker() {
        positionTrackerRunnable = object : Runnable {
            override fun run() {
                val controller = mediaController ?: return
                val currentPositionMs = controller.currentPosition

                // ✅ 핵심 로직: 이전 위치보다 현재 위치가 작아지면 루프로 간주
                //    (예: 4분 30초 -> 0초)
                if (currentPositionMs < previousPositionMs) {
                    // 이전 위치가 곡 길이의 끝부분에 가까웠을 때만 루프로 인정 (탐색 오작동 방지)
                    val duration = controller.duration
                    if (duration > 0 && previousPositionMs > duration - 1000) {
                        Log.d("LOOP_DETECTOR", "Loop Detected! Position jumped from $previousPositionMs to $currentPositionMs")
                        // 기존에 사용하던 로직 호출
                        handleTrackChange()
                    }
                }

                previousPositionMs = currentPositionMs

                // 0.25초마다 이 검사를 반복합니다.
                positionTrackerHandler.postDelayed(this, 250)
            }
        }
        positionTrackerHandler.post(positionTrackerRunnable!!)
    }

    // ✅ [추가] 위치 감시를 중단하는 함수
    private fun stopPositionTracker() {
        positionTrackerRunnable?.let { positionTrackerHandler.removeCallbacks(it) }
        previousPositionMs = 0L // 위치 초기화
    }

    companion object {
        const val TAG = "MainActivityViewModel"
        const val COMMAND_GET_AUDIO_SESSION_ID = "com.example.mymusic.GET_AUDIO_SESSION_ID"
        const val KEY_AUDIO_SESSION_ID = "audio_session_id"
    }
}
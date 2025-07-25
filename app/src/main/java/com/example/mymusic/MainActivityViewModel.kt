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
import com.example.mymusic.data.repository.PlaylistRepository
import com.example.mymusic.main.MyMediaService
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import com.example.mymusic.model.SessionKind
import com.example.mymusic.model.SessionSnapshot
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@UnstableApi
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    // --- LiveData ---
    private val _currentTrack = MutableLiveData<Favorite?>(null)
    val currentTrack: LiveData<Favorite?> get() = _currentTrack

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _totalPlayCountInARow = MutableLiveData<Int>(0)
    val totalPlayCountInARow: LiveData<Int> get() = _totalPlayCountInARow

    private var _favoriteList = MutableLiveData<List<Favorite>>(emptyList())
    val favoriteList get() = _favoriteList

    private var _nowPlayingList = MutableLiveData<List<Favorite>>(emptyList())
    val nowPlayingList: LiveData<List<Favorite>> get() = _nowPlayingList

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

    private val _shuffleMode = MutableLiveData<Boolean>(false)
    val shuffleMode: LiveData<Boolean> get() = _shuffleMode

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
    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(application) }


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
                Log.d(TAG, "onMediaItemTransition: reason = $reason")

                if (mediaItem == null || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    return
                }

                updateTrackInfoOnTransition(reason)

                /**
                // ✅ [수정] reason에 따라 로직 분기
                when (reason) {
                    // 세션 토글 등으로 플레이리스트 자체가 바뀐 경우
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                        // 이전 곡 재생시간 계산(checkPlayCount)을 하지 않고, 새 곡 정보 업데이트만 수행
                        Log.d(TAG, "Playlist changed. Only updating new track info.")
                        handleTrackChange()
                    }
                    // 자동 다음곡, 사용자 요청(다음/이전) 등 일반적인 트랙 변경
                    else -> {
                        // 이전 곡 재생시간을 계산한 후, 새 곡 정보 업데이트 수행
                        Log.d(TAG, "Normal track transition. Evaluating previous track and updating new one.")
                        checkPlayCount(currentTrack.value)
                        handleTrackChange()
                    }
                }
                */
            }



            override fun onPlaybackStateChanged(playbackState: Int) {
                // 재생 목록의 마지막 곡이 끝나면 재생 상태를 false로 변경
                if (playbackState == Player.STATE_ENDED) {
                    if (repeatMode.value == Player.REPEAT_MODE_OFF && currentIndex == (_nowPlayingList.value?.lastIndex ?: -1)) {
                        _isPlaying.value = false
                    }
                }

                /**
                if (playbackState == Player.STATE_READY) { /** 사용자 조작에 의해서 트랙이 변경된 모든 경우(이 시점에서 newDuration 값은 정확하다 ) */
                    val newDuration = mediaController?.duration?.takeIf { it != C.TIME_UNSET }?.toInt() ?: 0
                    if (newDuration > 1) {
                        _trackDuration.postValue(newDuration)
                        currentTrackDurationMs = newDuration.toLong()
                    }
                }
                */
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun handleTrackChange() {
        Log.d(TAG, "handle track change")
        Handler(Looper.getMainLooper()).postDelayed({
            _totalPlayCountInARow.value = (totalPlayCountInARow.value ?: 0) + 1
            val newDuration = mediaController?.duration?.takeIf { it != C.TIME_UNSET }?.toInt() ?: 0
            if (newDuration > 1) { /** 값이 0 또는 1일 수 있다. 하지만 playbackState change callback 이 못 잡는 트랙이 자동으로 넘어가는 경우에 정확한 값이다 */
                _trackDuration.postValue(newDuration)
                currentTrackDurationMs = newDuration.toLong()
            }
            Log.d(TAG, "duration: " + currentTrackDurationMs)
            lastPlayedTrackIndex = currentIndex
            currentIndex = mediaController?.currentMediaItemIndex ?: 0
            _currentTrack.value = _nowPlayingList.value?.getOrNull(currentIndex)
        }, 0)
    }

    // ✅ 1. 새로운 최종 트랙 정보 업데이트 함수
    private fun updateTrackInfoOnTransition(reason: Int) {
        // ★ 1-1. 이전 곡에 대한 마무리 작업 (재생 카운트 체크)
        // 셔플/토글이 아닌 경우에만 이전 곡 카운트 체크
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
            checkPlayCount(currentTrack.value)
        }

        // ★ 1-2. 새로운 곡의 기본 정보 즉시 업데이트
        _totalPlayCountInARow.value = (totalPlayCountInARow.value ?: 0) + 1
        lastPlayedTrackIndex = currentIndex
        currentIndex = mediaController?.currentMediaItemIndex ?: 0
        _currentTrack.value = _nowPlayingList.value?.getOrNull(currentIndex)

        // ★ 1-3. "재시도" 로직으로 정확한 duration 가져오기 시작
        fetchDurationWithRetry()
    }

    // ✅ 2. Duration을 가져오기 위한 재시도 함수
    private fun fetchDurationWithRetry(retryCount: Int = 0) {
        val controller = mediaController ?: return

        // 최대 20번(약 2초)까지 재시도 후 포기
        if (retryCount > 20) {
            Log.e(TAG, "Failed to get duration after multiple retries.")
            _trackDuration.postValue(0)
            currentTrackDurationMs = 0L
            return
        }

        val newDuration = controller.duration
        // 유효한 duration 값인지 확인 (보통 1000ms 이상)
        if (newDuration > 1000) {
            // 성공! LiveData 및 변수 업데이트
            Log.d(TAG, "Duration fetched successfully on retry $retryCount: $newDuration")
            _trackDuration.postValue(newDuration.toInt())
            currentTrackDurationMs = newDuration
        } else {
            // 실패. 100ms 후에 자기 자신을 다시 호출하여 재시도
            Handler(Looper.getMainLooper()).postDelayed({
                fetchDurationWithRetry(retryCount + 1)
            }, 100)
        }
    }



    fun shufflePlayList() {
        if (nowPlayingList.value.isNullOrEmpty()) return
        mediaController!!.clearMediaItems()

        if (activeKind.value == SessionKind.AD_HOC) {
            val newPlayList = nowPlayingList.value!!.shuffled()
            setPlaylist(newPlayList, 0)
        }
        else {
            val snap = savedSession ?: return
            val playlistId = snap.playlistId ?: return

            viewModelScope.launch(Dispatchers.IO) {
                val playlist = playlistRepository.getByIdWithFavorites(playlistId) ?: return@launch

                // 1) 통째로 셔플 (현재 곡/위치 무시)
                val shuffledList = playlist.favorites.shuffled()

                // 2) 새 스냅샷: 셔플된 순서 반영 + 처음부터(원하면 randomStart로 변경)
                val newTrackIds = shuffledList.map { it.track.trackId }
                val startIndex = 0                 // 랜덤 시작 원하면 Random.nextInt(shuffledList.size)
                val newSnap = snap.copy(
                    trackIds   = newTrackIds,
                    index      = startIndex,
                    positionMs = 0L,
                    shuffled   = true,
                    accumulatedListenTimeMs = 0L
                )

                withContext(Dispatchers.Main) {
                    _activeKind.value = SessionKind.SAVED
                    currentSavedPlaylistId = playlistId

                    _shuffleMode.value = true

                    // 3) 적용 + 저장(토글/복원 대비)
                    applySessionSnapshot(newSnap, shuffledList, autoPlay = true)
                    savedSession = newSnap
                }
            }
        }

        _shuffleMode.value = true
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
            //Log.d(TAG, "listened time ms" + listenedTimeMs)
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

            val lastPlayedId = listOf(lastPlayedTrack.track.trackId)
            playlistRepository.addTracksMoveDuplicatesToFront(
                PlaylistRepository.PLAYLIST_ID_RECENTLY_PLAYED,
                lastPlayedId
            )

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


    fun loadFavorite() {
        viewModelScope.launch(Dispatchers.IO) {
            val rawList = favoriteSongRepository.allFavoriteTracks
            this@MainActivityViewModel._favoriteList.postValue(rawList)
        }
    }

    fun addPlaylist(incomingList: List<Favorite>) {
        val controller = mediaController ?: return

        // 0) ad-hoc & saved 둘 다 비어있으면: incoming으로 ad-hoc 세팅 + 자동 재생
        if (adHocSession == null && savedSession == null) {
            // ad-hoc 스냅샷 생성
            val newIds = incomingList.map { it.track.trackId }
            adHocSession = SessionSnapshot(
                kind = SessionKind.AD_HOC,
                trackIds = newIds,
                index = 0,
                positionMs = 0L,
                repeatMode = controller.repeatMode,
                shuffled = false,
                playlistId = null,
                accumulatedListenTimeMs = 0L
            )

            // 포그라운드 서비스 보장(다른 곳에서 이미 스타트한다면 생략 가능)
            ContextCompat.startForegroundService(
                getApplication(),
                Intent(getApplication(), MyMediaService::class.java)
            )

            // nowPlayingList 반영 + 자동 재생(현재 곡 없음 → 새 큐로 시작)
            _activeKind.value = SessionKind.AD_HOC
            _nowPlayingList.value = incomingList
            currentIndex = 0
            listenedTimeMs = 0L

            val items = buildMediaItems(incomingList)
            controller.setMediaItems(items, /* startIndex = */ 0, /* positionMs = */ C.TIME_UNSET)
            controller.prepare()
            controller.play()
            return
        }

        // 1) ad-hoc 스냅샷이 아예 없을 때: 스냅샷만 만들어 두고(자동 재생 X),
        //    현재 재생이 AD_HOC면 중복 제거 후 뒤에 붙이기
        if (adHocSession == null) {
            val newIds = incomingList.map { it.track.trackId }
            adHocSession = SessionSnapshot(
                kind = SessionKind.AD_HOC,
                trackIds = newIds,
                index = 0,
                positionMs = 0L,
                repeatMode = controller.repeatMode,
                shuffled = false,
                playlistId = null,
                accumulatedListenTimeMs = 0L
            )

            if (activeKind.value == SessionKind.AD_HOC) {
                val currentList = _nowPlayingList.value?.toMutableList() ?: mutableListOf()
                val existingSet = currentList.map { it.track.trackId }.toHashSet()
                val toAppend = incomingList.filter { it.track.trackId !in existingSet }
                if (toAppend.isNotEmpty()) {
                    controller.addMediaItems(buildMediaItems(toAppend)) // 현재 곡 유지
                    currentList.addAll(toAppend)
                    _nowPlayingList.value = currentList
                    adHocSession = adHocSession!!.copy(
                        trackIds = currentList.map { it.track.trackId }
                    )
                }
            }
            return
        }

        // 2) ad-hoc 스냅샷 존재: 중복 제거 후 ad-hoc 뒤에 append
        val currentIds = adHocSession!!.trackIds
        val seen = currentIds.toMutableSet()
        val toAppend = incomingList.filter { seen.add(it.track.trackId) }
        if (toAppend.isEmpty()) return

        adHocSession = adHocSession!!.copy(
            trackIds = currentIds + toAppend.map { it.track.trackId }
        )

        // AD_HOC 재생 중이면 실제 큐/리스트 동기화(현재 곡 유지)
        if (activeKind.value == SessionKind.AD_HOC) {
            controller.addMediaItems(buildMediaItems(toAppend)) // 현재 곡 계속 재생
            val currentList = _nowPlayingList.value?.toMutableList() ?: mutableListOf()
            currentList.addAll(toAppend)
            _nowPlayingList.value = currentList
        }
    }



    private fun buildMediaItems(queue: List<Favorite>): List<MediaItem> = queue.map { item ->
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

    private fun applySessionSnapshot(
        snapshot: SessionSnapshot,
        queue: List<Favorite>,          // snapshot.trackIds로 재구성한 실제 큐
        autoPlay: Boolean
    ) {
        val controller = mediaController ?: return

        // nowPlayingList 반영
        _nowPlayingList.value = queue
        currentIndex = snapshot.index.coerceIn(0, queue.lastIndex)
        _repeatMode.value = snapshot.repeatMode
        _shuffleMode.value = snapshot.shuffled
        controller.repeatMode = snapshot.repeatMode
        listenedTimeMs = snapshot.accumulatedListenTimeMs


        val items = buildMediaItems(queue)
        controller.setMediaItems(items, currentIndex, snapshot.positionMs)
        controller.prepare()
        if (autoPlay) controller.play()
    }

    // MainActivityViewModel.kt

    // 현재 어떤 세션인지
    private val _activeKind = MutableLiveData<SessionKind?>()
    val activeKind: LiveData<SessionKind?> get() = _activeKind

    // 저장된 플레이리스트에서 재생 중일 때의 세션 상태
    private var savedSession: SessionSnapshot? = null

    private var lastSelectedPlaylist: Playlist? = null
    fun getLastSelectedPlaylist():Playlist? = lastSelectedPlaylist

    // 임시(Ad-hoc) 플레이리스트에서 재생 중일 때의 세션 상태
    private var adHocSession: SessionSnapshot? = null

    val isDualSessionAvailable: Boolean
        get() = savedSession != null && adHocSession != null

    val isAnySessionAvailable: Boolean
        get() = savedSession != null || adHocSession != null

    fun toggleSession() {
        // 둘 다 없거나 하나만 있으면 종료
        if (savedSession == null || adHocSession == null) return

        // 1) 현재 재생 상태 저장
        saveActiveSnapshot()

        // 2) 반대 세션으로 전환
        when (activeKind.value) {
            SessionKind.SAVED -> resumeAdHocIfPossible()   // SAVED → AD_HOC 로 전환
            SessionKind.AD_HOC -> resumeSavedIfPossible()  // AD_HOC → SAVED 로 전환
            else -> {}
        }
    }


    private fun saveActiveSnapshot() {
        val controller = mediaController ?: return
        val kind = _activeKind.value ?: return
        val queue = _nowPlayingList.value ?: return
        if (queue.isEmpty()) return

        val index = controller.currentMediaItemIndex.coerceAtLeast(0)
        val pos = controller.currentPosition
        val ids = queue.map { it.track.trackId }

        val snap = SessionSnapshot(
            kind = kind,
            trackIds = ids,
            index = index,
            positionMs = pos,
            repeatMode = controller.repeatMode,
            shuffled = shuffleMode.value ?: false,
            playlistId = if (kind == SessionKind.SAVED) currentSavedPlaylistId else null,
            accumulatedListenTimeMs = listenedTimeMs
        )

        when (kind) {
            SessionKind.AD_HOC -> adHocSession = snap
            SessionKind.SAVED  -> savedSession = snap
        }
    }


    // 현재 활성 SAVED 세션의 playlistId 기억용
    private var currentSavedPlaylistId: String? = null

    fun resumeAdHocIfPossible() {
        val snap = adHocSession ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // trackId들로 Favorite 리스트를 다시 복구 (repo/캐시에서 가져오기)
            val queue = favoriteSongRepository.getFavoritesByIds(snap.trackIds)
            withContext(Dispatchers.Main) {
                _activeKind.value = SessionKind.AD_HOC
                applySessionSnapshot(snap, queue, autoPlay = isPlaying.value ?: false)
            }
        }
    }

    fun resumeSavedIfPossible() {
        val snap = savedSession ?: return
        val playlistId = snap.playlistId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val playlist = playlistRepository.getByIdWithFavorites(playlistId) ?: return@launch

            // index 재매핑
            val desiredTrackId = snap.trackIds.getOrNull(snap.index)
            val newIndex = desiredTrackId?.let { id ->
                playlist.favorites.indexOfFirst { it.track.trackId == id }
            }?.takeIf { it >= 0 } ?: 0

            val fixedSnap = snap.copy(index = newIndex)

            withContext(Dispatchers.Main) {
                _activeKind.value = SessionKind.SAVED
                currentSavedPlaylistId = playlistId
                applySessionSnapshot(fixedSnap, playlist.favorites, isPlaying.value ?: false)
            }
        }
    }




    fun setPlaylistFromPlaylistInOrder(playlist: Playlist, startPosition: Int) {
        mediaController ?: return
        if (playlist.favorites.isEmpty()) return
        _shuffleMode.value = false
        setPlaylistFromPlaylist(playlist, startPosition)
    }
    fun setPlaylistFromPlaylistWithShuffle(playlist: Playlist) {
        mediaController ?: return
        playlist.favorites = playlist.favorites
        if (playlist.favorites.isEmpty()) return
        _shuffleMode.value = true
        setPlaylistFromPlaylist(playlist, 0)
    }
    fun setPlaylistFromPlaylist(playlist: Playlist, startPosition: Int) {
        val controller = mediaController ?: return
        val queue = playlist.favorites
        if (queue.isEmpty()) return

        lastSelectedPlaylist = playlist

        saveActiveSnapshot() // 떠나는 모드 스냅샷 (ad-hoc 저장됨)

        _activeKind.value = SessionKind.SAVED
        currentSavedPlaylistId = playlist.playlistId

        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), MyMediaService::class.java))

        _totalPlayCountInARow.value = 0
        resetListeningTime() // ✅ [추가] 새 플레이리스트이므로 청취 시간 확실히 초기화


        val items = buildMediaItems(queue)
        _nowPlayingList.value = queue
        currentIndex = startPosition.coerceIn(0, queue.lastIndex)

        controller.setMediaItems(items, currentIndex, C.TIME_UNSET)
        controller.prepare()
        controller.play()

        // ✅ 들어온 모드(savED)도 즉시 스냅샷 생성
        saveCurrentModeSnapshotNow()
    }



    fun setPlaylist(playlist: List<Favorite>, startPosition: Int) {
        val controller = mediaController ?: return
        saveActiveSnapshot() // 떠나는 모드 스냅샷 (saved 저장될 수 있음)

        _activeKind.value = SessionKind.AD_HOC
        currentSavedPlaylistId = null
        _shuffleMode.value = false
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), MyMediaService::class.java))

        _totalPlayCountInARow.value = 0
        resetListeningTime() // ✅ [추가] 새 플레이리스트이므로 청취 시간 확실히 초기화

        val items = buildMediaItems(playlist)
        _nowPlayingList.value = playlist
        currentIndex = startPosition.coerceIn(0, playlist.lastIndex)

        controller.setMediaItems(items, currentIndex, C.TIME_UNSET)
        controller.prepare()
        controller.play()

        // ✅ 들어온 모드(ad-hoc)도 즉시 스냅샷 생성
        saveCurrentModeSnapshotNow()
    }

    /** 현재 활성 모드의 스냅샷을 즉시 찍어서 두 세션을 항상 보유하도록 */
    private fun saveCurrentModeSnapshotNow() {
        val controller = mediaController ?: return
        val kind = _activeKind.value ?: return

        val queue = _nowPlayingList.value ?: return
        if (queue.isEmpty()) return

        val index = controller.currentMediaItemIndex.coerceAtLeast(0)
        val pos = controller.currentPosition
        val ids = queue.map { it.track.trackId }

        val snap = SessionSnapshot(
            kind = kind,
            trackIds = ids,
            index = index,
            positionMs = pos,
            repeatMode = controller.repeatMode,
            shuffled = shuffleMode.value ?: false,
            playlistId = if (kind == SessionKind.SAVED) currentSavedPlaylistId else null,
            accumulatedListenTimeMs = listenedTimeMs
        )

        when (kind) {
            SessionKind.AD_HOC -> adHocSession = snap
            SessionKind.SAVED  -> savedSession = snap
        }
    }




}
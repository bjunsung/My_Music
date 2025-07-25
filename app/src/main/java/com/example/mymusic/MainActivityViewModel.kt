package com.example.mymusic

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.model.Favorite
import com.bumptech.glide.Priority   //
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ExternalLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.main.MyMediaService
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTrack = MutableLiveData<Favorite?>(null)
    val currentTrack: LiveData<Favorite?> get() = _currentTrack

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    //var exoPlayer: ExoPlayer? = ExoPlayer.Builder(application).build()
    var exoPlayer: ExoPlayer? = null
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

    private val _audioSessionId = MutableLiveData<Int>()
    val audioSessionId get() = _audioSessionId!!


    private val _showBottomSheet = MutableLiveData(false)
    val showBottomSheet get() = _showBottomSheet
    fun requestBottomSheet(isVisible: Boolean) {
        _showBottomSheet.value = isVisible
    }



    init {
        initPlayer()
    }



    /** 플레이리스트와 시작 인덱스를 설정하고 재생 시작 */
    @OptIn(UnstableApi::class)
    fun setPlaylist(playlist: List<Favorite>, startPosition: Int) {
        if (exoPlayer == null) return
        shuffledMode = false

        // 재생 버튼 누를 때 등, 플레이 시작 직전에:
        ContextCompat.startForegroundService(
            getApplication(), Intent(getApplication(), MyMediaService::class.java)
        )
// 이후에 controller나 바인딩으로 MyMediaService의 player에 미디어 세팅 & play


        _totalPlayCountInARow.value = 0
        _playlist.value = playlist
        currentIndex = startPosition.coerceIn(0, playlist.lastIndex)

        exoPlayer!!.clearMediaItems()

        val mediaItems = playlist.mapNotNull { item ->
            item.audioUri?.let { uri ->

                // 1. String 형태의 이미지 주소를 Uri 객체로 변환합니다.
                // item.albumArtUrl이 String 타입이라고 가정합니다.
                val imageUri: Uri? = if (item.track.artworkUrl.isNullOrEmpty()) {
                    null // 주소가 없거나 비어있으면 null 처리
                } else {
                    item.track.artworkUrl.toUri() // ✨ 바로 이 부분입니다!
                }

                // 2. MediaMetadata를 생성합니다.
                val metadata = MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artistName)
                    .setArtworkUri(imageUri) // 변환된 Uri 객체를 여기에 설정합니다.
                    .build()

                // 3. MediaItem을 생성합니다.
                MediaItem.Builder()
                    .setMediaId(item.track.trackId)
                    .setUri(uri)
                    .setMediaMetadata(metadata)
                    .build()
            }
        }

        exoPlayer!!.setMediaItems(mediaItems)

        exoPlayer!!.prepare()
        Log.d(TAG, "play at from first play of playlist")
        exoPlayer!!.seekToDefaultPosition(startPosition)
        exoPlayer!!.playWhenReady = true
    }
    @OptIn(UnstableApi::class)
    fun initPlayer() {
        if (exoPlayer == null) {
            // 1) Glide 기반 ExternalLoader
            val glidePreloader = ExternalLoader { req: ExternalLoader.LoadRequest ->
                // ListenableFuture<File> 생성
                val future = com.google.common.util.concurrent.SettableFuture.create<File>()
                val target = object : CustomTarget<File>() {
                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        future.set(resource)
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        future.setException(IllegalStateException("Artwork preload failed: ${req.uri}"))
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                }


                Glide.with(getApplication<Application>())
                    .asFile()
                    .apply(
                        RequestOptions
                            .diskCacheStrategyOf(DiskCacheStrategy.DATA)
                            .priority(Priority.HIGH)              // ✅ 여기!
                            .skipMemoryCache(true)
                    )
                    .load(req.uri)
                    .into(target)

                future


            }

            val msFactory = DefaultMediaSourceFactory(getApplication<Application>())
                .setExternalImageLoader(glidePreloader)

            // 3) ExoPlayer 생성
            exoPlayer = ExoPlayer.Builder(getApplication())
                .setMediaSourceFactory(msFactory)
                .build().apply {
                    setAudioAttributes(
                        androidx.media3.common.AudioAttributes.Builder()
                            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        /* handleAudioFocus = */ true
                    )
                }
        }

        // 이하 네 리스너/로직은 기존 코드 유지 (onMediaItemTransition 등)
        // ...
        exoPlayer!!.prepare()

        // 곡 끝나면 다음 곡 자동 재생
        exoPlayer!!.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                exoPlayer!!.pause()
                exoPlayer!!.play()

                Handler(Looper.getMainLooper()).postDelayed({
                    val newDuration = exoPlayer?.duration?.toInt() ?: 0
                    _trackDuration.postValue(newDuration) // ViewModel에서 새로운 LiveData로 전달
                    // <<< [추가] 새 트랙의 전체 길이를 변수에 저장
                    currentTrackDurationMs = newDuration.toLong()
                }, 200)


                // <<< [추가] 트랙 넘기기 전에 현재 곡 카운트 체크
                Log.d(TAG, "current track!!!!!!!!!" + currentTrack.value?.title)
                checkPlayCount(currentTrack.value)

                lastPlayedTrackIndex = currentIndex
                currentIndex = exoPlayer?.currentMediaItemIndex?.coerceIn(0, playlist.value!!.lastIndex) ?: 0
                _currentTrack.value = playlist.value!![currentIndex]
                Log.d(TAG, "current track: " + currentTrack.value?.title + " play count: " + currentTrack.value?.playCount)

                _totalPlayCountInARow.value = _totalPlayCountInARow.value?.plus(1)

                Log.d(TAG, "play at from onMediaItemTransition Listener before")

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
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
                    _audioSessionId.postValue(audioSessionId)
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
    private fun checkPlayCount(lastPlayedTrack: Favorite?) {
        // 유효한 트랙 길이를 가지고 있을 때만 체크
        if (currentTrackDurationMs <= 0) {
            Log.d(TAG, "Check skipped: Invalid duration ($currentTrackDurationMs)")
            resetListeningTime() // 시간 초기화
            return
        }

        if (listenedTimeMs == 0L) return
        if (listenedTimeMs >= currentTrackDurationMs * 2 / 3) {
            val today:String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            Log.d(TAG, "✅ COUNT! Date: $today, Listened: $listenedTimeMs, Total: $currentTrackDurationMs, track name: ${lastPlayedTrack?.title ?: "name not found"}")
            updatePlayCount(lastPlayedTrack)
        } else {
            Log.d(TAG, "❌ NOT COUNTED. Listened: $listenedTimeMs, Total: $currentTrackDurationMs, track name: ${lastPlayedTrack?.title ?: "name not found"}")
        }

        // 다음 곡을 위해 누적 시간 초기화
        resetListeningTime()
    }

    private fun updatePlayCount(lastPlayedTrack: Favorite?) {
        val today = LocalDate.now()
        viewModelScope.launch(Dispatchers.IO) {
            val lastPlayedSync =  favoriteSongRepository.getFavoriteSongWithPlayCount(lastPlayedTrack!!.track.trackId)
            lastPlayedSync.addPlayCount(today)
            favoriteSongRepository.updateFavoriteSongWithPlayCount(lastPlayedSync, object : FavoriteSongRepository.FavoriteDbCallback {
                override fun onSuccess() {
                    Log.d(TAG, "count updated, current count: ${lastPlayedSync.playCount}")
                }

                override fun onFailure() {
                    Log.d(TAG, "count update failed for ${lastPlayedSync.track.trackName}")
                }

            })
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
        Log.d(TAG, "repeat mode: " + nextMode)
        exoPlayer?.repeatMode = nextMode
    }



    /** 다음 곡 */
    fun playNext() {
        if (playlist.value!!.isEmpty() || exoPlayer == null) return
        if (repeatMode.value == Player.REPEAT_MODE_ONE)
            exoPlayer?.seekToDefaultPosition(currentIndex)
        else
            exoPlayer?.seekToNextMediaItem()

        /*
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

         */
    }



    /** 이전 곡 */
    fun playPrevious() {
        if (playlist.value!!.isEmpty() || exoPlayer == null) return
        if (repeatMode.value == Player.REPEAT_MODE_ONE)
            exoPlayer?.seekToDefaultPosition(currentIndex)
        else
            exoPlayer?.seekToPreviousMediaItem()
        /*
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

         */
    }

    /** 일시정지/재생 토글 */
    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                //_isPlaying.value = false
            } else {
                val isAtEnd = it.currentPosition >= (it.duration - 500)
                if (repeatMode.value == Player.REPEAT_MODE_OFF && currentIndex == playlist.value!!.lastIndex && isAtEnd) {
                    Log.d(TAG, "play at from toggle play/pause button click (lastIndex)")
                    //playAt(currentIndex)
                    _repeatMode.value = Player.REPEAT_MODE_ONE
                    exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
                }
                it.play()
                //_isPlaying.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        checkPlayCount(currentTrack.value)
        exoPlayer?.release()
        exoPlayer = null
    }


    companion object {
        const val TAG = "MainActivityViewModel"
    }
}

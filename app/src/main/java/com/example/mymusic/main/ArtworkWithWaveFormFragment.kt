package com.example.mymusic.main

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.customView.WaveformView
import com.example.mymusic.databinding.FragmentArtworkWithWaveFormBinding
import com.example.mymusic.main.MusicPlayingBottomSheet.Companion.TAG
import com.example.mymusic.model.Favorite
import com.google.android.exoplayer2.Player

class ArtworkWithWaveFormFragment : Fragment() {

    private var _binding : FragmentArtworkWithWaveFormBinding? = null
    private val binding get() = _binding!!

    private val artworkImage: ImageView by lazy { binding.focusedImage }
    private val titleTextView: TextView by lazy { binding.focusedTitle }
    private val albumTitleTextView: TextView by lazy { binding.focusedAlbumTitle }
    private val artistNameTextView: TextView by lazy { binding.focusedArtist }
    private val durationTextView: TextView by lazy { binding.focusedDuration }
    private val releaseDateTextView: TextView by lazy { binding.focusedReleaseDate }
    private val waveformView : WaveformView by lazy {binding.waveformView}
    private val mainActivityViewModel : MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()

    private var favorite: Favorite? = null

    private var animator: ValueAnimator? = null

    private var visualizer: Visualizer? = null
    private var visualizerRetryAttempt = 0
    private val VISUALIZER_MAX_RETRIES = 3
    private val VISUALIZER_RETRY_DELAY_MS = 300L
    private var playerListener: Player.Listener? = null

    private var handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) : View {
        _binding = FragmentArtworkWithWaveFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
    }

    override fun onResume() {
        super.onResume()
        waveformView.startAnimation()
    }

    private fun bind() {
        favorite = mainActivityViewModel.currentTrack.value
        favorite?.let { updateData(it) }
        mainActivityViewModel.currentTrack.observe(viewLifecycleOwner) { favoriteSync ->
            favoriteSync?.let {
                this.favorite = favoriteSync
                updateData(favoriteSync)
            }
        }

        mainActivityViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            if (playing){
                waveformView.startAnimation()
                startArtworkImageRotation()
            }
            else {
                waveformView.stopAnimation()
                stopArtworkImageRotation()
            }
        }
        // 1. 리스너 생성 및 등록
        playerListener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    Log.d(TAG, "Listener: audioSessionId가 $audioSessionId 으로 변경됨")
                    setupVisualizerWithRetry(audioSessionId)
                }
            }
        }
        mainActivityViewModel.exoPlayer?.addListener(playerListener!!)


        checkAndRequestAudioPermission()
    }

    private fun updateData(currentFavorite: Favorite) {
        //compose transition 대비
        Glide.with(requireContext())
            .asBitmap()
            .load(currentFavorite.track.artworkUrl)
            .override(720, 720)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    artworkImage.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        titleTextView.text = currentFavorite.title
        albumTitleTextView.text = currentFavorite.track.albumName
        artistNameTextView.text = currentFavorite.artistName
        durationTextView.text = currentFavorite.durationStr
        releaseDateTextView.text = currentFavorite.releaseDate
    }

    private fun startArtworkImageRotation() {
        val clockwise = if (mainActivityViewModel.currentIndex % 2 == 0) 1.0f else -1.0f
        animator?.cancel()
        val currentRotation = musicPlayingViewModel.rotationAngle.value ?: 0f
        animator = ValueAnimator.ofFloat(currentRotation, currentRotation + clockwise * 360f).apply {
            duration = musicPlayingViewModel.rotationDuration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                artworkImage.rotation = (it.animatedValue as Float) % 360
            }
            start()
        }

    }

    private fun stopArtworkImageRotation() {
        animator?.cancel()
    }
    private val requesetPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 사용자가 권한을 '허용'했을 때의 동작
                Log.d(TAG, "RECORD_AUDIO 권한이 허용되었습니다.")
                // 권한을 받았으니, Visualizer 설정을 다시 시도합니다.
                val sessionId = mainActivityViewModel.exoPlayer?.audioSessionId ?: 0
                if (sessionId != 0) {
                    setupVisualizerWithRetry(sessionId)
                }
            } else {
                // 사용자가 권한을 '거부'했을 때의 동작
                Log.w(TAG, "RECORD_AUDIO 권한이 거부되었습니다.")
                // 필요하다면, 사용자에게 왜 권한이 필요한지 설명하는 UI를 보여줄 수 있습니다.
                waveformView.visibility = View.GONE // 파형 뷰 숨기기
            }
        }

    private fun checkAndRequestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "RECORD_AUDIO 권한이 이미 있습니다.")
                // Visualizer를 바로 설정합니다.
                val sessionId = mainActivityViewModel.exoPlayer?.audioSessionId ?: 0
                if (sessionId != 0) {
                    setupVisualizerWithRetry(sessionId)
                }
            }
            else -> {
                Log.d(TAG, "RECORD_AUDIO 권한 요청을 시작합니다.")
                requesetPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    private fun setupVisualizerWithRetry(sessionId: Int) {
        visualizerRetryAttempt = 0 // 재시도 카운트 초기화
        initVisualizer(sessionId)
    }
    private fun initVisualizer(sessionId: Int) {
        // 최대 재시도 횟수를 초과하면 UI를 숨기고 종료 (Fallback)
        if (visualizerRetryAttempt >= VISUALIZER_MAX_RETRIES) {
            Log.e(TAG, "Visualizer failed to initialize after $VISUALIZER_MAX_RETRIES attempts.")
            waveformView.visibility = View.GONE // WaveformView 숨기기
            return
        }

        try {
            visualizer?.release() // 기존 인스턴스가 있다면 해제

            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                    ) {
                        // <<< [디버깅 로그 추가]
                        if (waveform != null) {
                            waveformView.updateWaveform(waveform)
                        } else {
                            Log.d(TAG, "Waveform data is null.")
                        }
                    }
                    override fun onFftDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }

            // 성공 시 UI를 다시 보이게 처리
            waveformView.visibility = View.VISIBLE
            Log.d(
                TAG,
                "Visualizer initialized successfully on attempt ${visualizerRetryAttempt + 1}."
            )

        } catch (e: Exception) {
            visualizerRetryAttempt++
            Log.w(TAG, "Visualizer init failed on attempt $visualizerRetryAttempt: ${e.message}")

            // 딜레이 후 재시도
            handler.postDelayed({ initVisualizer(sessionId) }, VISUALIZER_RETRY_DELAY_MS)
        }
    }

    override fun onStop() {
        super.onStop()
        // Visualizer 리소스 해제
        visualizer?.release()
        visualizer = null

        // ExoPlayer 리스너 제거
        playerListener?.let {
            mainActivityViewModel.exoPlayer?.removeListener(it)
        }
    }

}
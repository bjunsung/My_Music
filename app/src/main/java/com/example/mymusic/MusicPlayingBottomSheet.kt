    package com.example.mymusic

    import android.animation.ValueAnimator
    import android.content.pm.PackageManager
    import android.graphics.*
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
    import android.widget.FrameLayout
    import android.widget.ImageButton
    import android.widget.ImageView
    import android.widget.SeekBar
    import android.widget.TextView
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.activityViewModels
    import androidx.lifecycle.viewModelScope
    import androidx.recyclerview.widget.RecyclerView
    import com.bumptech.glide.Glide
    import com.bumptech.glide.request.target.CustomTarget
    import com.bumptech.glide.request.transition.Transition
    import com.example.mymusic.data.repository.FavoriteSongRepository
    import com.example.mymusic.databinding.BottomSheetMusicPlayingBinding
    import com.example.mymusic.model.Favorite
    import com.example.mymusic.util.DarkModeUtils
    import com.example.mymusic.util.ImageColorAnalyzer
    import com.example.mymusic.util.MyColorUtils
    import com.google.android.exoplayer2.ExoPlayer
    import com.google.android.exoplayer2.Player
    import com.google.android.material.bottomsheet.BottomSheetBehavior
    import com.google.android.material.bottomsheet.BottomSheetDialog
    import com.google.android.material.bottomsheet.BottomSheetDialogFragment
    import com.google.android.material.card.MaterialCardView
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch

    class MusicPlayingBottomSheet : BottomSheetDialogFragment() {

        private var _binding: BottomSheetMusicPlayingBinding? = null
        private val binding get() = _binding!!
        private val artworkImage: ImageView by lazy { binding.focusedImage }
        private val titleTextView: TextView by lazy { binding.focusedTitle }
        private val albumTitleTextView: TextView by lazy { binding.focusedAlbumTitle }
        private val artistNameTextView: TextView by lazy { binding.focusedArtist }
        private val durationTextView: TextView by lazy { binding.focusedDuration }
        private val releaseDateTextView: TextView by lazy { binding.focusedReleaseDate }
        private val recyclerView: RecyclerView by lazy { binding.playlistRecyclerView }
        private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
        private val lyricsTextView: TextView by lazy { binding.lyrics }

        private val musicPlayingBarFrame: FrameLayout by lazy { binding.musicPlayingBar }
        private val rootFrame: MaterialCardView by lazy { binding.rootFrame }
        private val simpleMusicInfoFrame: MaterialCardView by lazy { binding.simpleMusicInfo }
        private val playlistFrame: MaterialCardView by lazy { binding.playlistFrame }
        private val lyricsFrame: FrameLayout by lazy { binding.lyricsFrame }

        private val seekBar by lazy { binding.seekBar }
        private val currentTime by lazy { binding.currentTime }
        private val totalTimeTextView by lazy { binding.totalTime }
        private val playButton by lazy { binding.audioPlayButton }
        private val pauseButton by lazy { binding.audioPauseButton }
        private val skipPreviousButton by lazy { binding.skipPrevious }
        private val skipNextButton by lazy { binding.skipNext }
        private var updateSeekBarRunnable: Runnable? = null
        private var handler = Handler(Looper.getMainLooper())
        private val exoPlayer: ExoPlayer by lazy { mainActivityViewModel.exoPlayer!! }
        private var isUserSeeking = false

        // 회전용
        private val gradientBackground : FrameLayout by lazy { binding.gradientBackground }
        private var rotationAngle = 0f
        private var animator: ValueAnimator? = null
        private var gradientPaint = Paint()
        private var gradientColors = intArrayOf(Color.DKGRAY, Color.GRAY, Color.DKGRAY)
        private var currentRotation = 0f

        val repeatOffButton : ImageButton by lazy { binding.repeatOff }
        val repeatOnButton : ImageButton by lazy { binding.repeatOn}
        val repeatOneButton : ImageButton by lazy { binding.repeatOne }

        private val shuffleButton by lazy { binding.shuffle }
        private val shuffleOnStateButton by lazy { binding.shuffleOnState }

        override fun getTheme(): Int = R.style.FullScreenBottomSheet

        override fun onStart() {
            super.onStart()
            val dialog = dialog as? BottomSheetDialog ?: return
            val bottomSheet =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return
            (bottomSheet.parent as? ViewGroup)?.setPadding(0, 0, 0, 0)
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        private lateinit var favorite: Favorite

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            favorite = arguments?.getParcelable(ARG_FAVORITE)!!
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = BottomSheetMusicPlayingBinding.inflate(inflater, container, false)
            return binding.root
        }

        fun newInstance(favorite: Favorite): MusicPlayingBottomSheet {
            return MusicPlayingBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FAVORITE, favorite)
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            bind()
        }

        private fun bind() {
            binding.scrollArea.setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            lyricsTextView.setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            setViewByTrack()

            mainActivityViewModel.trackDuration.observe(viewLifecycleOwner) { duration ->
                totalTimeTextView.text = formatDuration(duration)
                seekBar.max = duration
                setViewByTrack()
            }

            skipPreviousButton.setOnClickListener { mainActivityViewModel.playPrevious() }
            skipNextButton.setOnClickListener { mainActivityViewModel.playNext() }

            // 플레이 상태에 따라 회전 애니메이션 시작/정지
            mainActivityViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
                if (playing) {
                    startGradientRotation()
                    binding.waveformView.startAnimation() // <<< 추가
                    pauseButton.visibility = View.VISIBLE
                    playButton.visibility = View.INVISIBLE
                }
                else {
                    stopGradientRotation()
                    binding.waveformView.stopAnimation() // <<< 추가
                    pauseButton.visibility = View.INVISIBLE
                    playButton.visibility = View.VISIBLE
                }
            }

            // lyricsFrame 커스텀 드로잉
            gradientBackground.setWillNotDraw(false)
            gradientBackground.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            gradientBackground.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateGradientShader()
            }
            gradientBackground.setWillNotDraw(false)
            gradientBackground.invalidate()
            gradientBackground.setBackgroundColor(Color.TRANSPARENT)

            gradientBackground.post {
                gradientBackground.foreground = object : Drawable() {
                    override fun draw(canvas: Canvas) {
                        canvas.save()
                        canvas.rotate(rotationAngle, gradientBackground.width / 2f, gradientBackground.height / 2f)
                        canvas.drawRect(
                            0f,
                            0f,
                            gradientBackground.width.toFloat(),
                            gradientBackground.height.toFloat(),
                            gradientPaint
                        )
                        canvas.restore()
                    }

                    override fun setAlpha(alpha: Int) {}
                    override fun setColorFilter(colorFilter: ColorFilter?) {}
                    @Deprecated("Deprecated in Java")
                    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
                }
            }


            repeatOffButton.setOnClickListener { mainActivityViewModel.toggleRepeatMode() }
            repeatOnButton.setOnClickListener { mainActivityViewModel.toggleRepeatMode() }
            repeatOneButton.setOnClickListener { mainActivityViewModel.toggleRepeatMode() }


            mainActivityViewModel.repeatMode.observe(viewLifecycleOwner) { repeatMode ->
                setVisibilityByRepeatMode(repeatMode)
            }


            setVisibilityByShuffledMode()

            shuffleButton.setOnClickListener {
                mainActivityViewModel.shufflePlayList()
                setVisibilityByShuffledMode()
            }
            shuffleOnStateButton.setOnClickListener {
                shuffleButton.visibility = View.VISIBLE
                shuffleOnStateButton.visibility = View.INVISIBLE
            }


            Log.d(TAG, "테스트")
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
            /*
            // 2. 현재 오디오 세션 ID를 즉시 확인
            val currentSessionId = mainActivityViewModel.exoPlayer?.audioSessionId ?: 0
            if (currentSessionId != 0) {
                Log.d(TAG, "초기 확인: audioSessionId가 이미 $currentSessionId 으로 존재함")
                // 이미 유효한 세션 ID가 있으므로 바로 Visualizer 설정 시도
                setupVisualizerWithRetry(currentSessionId)
            }

             */

            //todo
            /**
             * end of bind()
             */
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
                    binding.waveformView.visibility = View.GONE // 파형 뷰 숨기기
                }
            }

        private fun checkAndRequestAudioPermission() {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.RECORD_AUDIO
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
        private var visualizer: Visualizer? = null
        private var visualizerRetryAttempt = 0
        private val VISUALIZER_MAX_RETRIES = 3
        private val VISUALIZER_RETRY_DELAY_MS = 300L
        private var playerListener: Player.Listener? = null
        private fun initVisualizer(sessionId: Int) {
            // 최대 재시도 횟수를 초과하면 UI를 숨기고 종료 (Fallback)
            if (visualizerRetryAttempt >= VISUALIZER_MAX_RETRIES) {
                Log.e(TAG, "Visualizer failed to initialize after $VISUALIZER_MAX_RETRIES attempts.")
                binding.waveformView.visibility = View.GONE // WaveformView 숨기기
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
                                // 배열의 첫 10개 값만 출력해서 데이터가 변하는지 확인
                                val sampleData = waveform.take(10).joinToString { it.toString() }
                                Log.d(TAG, "Waveform captured! Size: ${waveform.size}, Sample: [$sampleData]")
                                binding.waveformView.updateWaveform(waveform)
                            } else {
                                Log.d(TAG, "Waveform data is null.")
                            }
                        }
                        override fun onFftDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {}
                    }, Visualizer.getMaxCaptureRate() / 2, true, false)
                    enabled = true
                }

                // 성공 시 UI를 다시 보이게 처리
                binding.waveformView.visibility = View.VISIBLE
                Log.d(TAG, "Visualizer initialized successfully on attempt ${visualizerRetryAttempt + 1}.")

            } catch (e: Exception) {
                visualizerRetryAttempt++
                Log.w(TAG, "Visualizer init failed on attempt $visualizerRetryAttempt: ${e.message}")

                // 딜레이 후 재시도
                handler.postDelayed({ initVisualizer(sessionId) }, VISUALIZER_RETRY_DELAY_MS)
            }
        }


        private fun setVisibilityByShuffledMode(){
            if (mainActivityViewModel.shuffledMode) {
                shuffleButton.visibility = View.INVISIBLE
                shuffleOnStateButton.visibility = View.VISIBLE
            }
            else{
                shuffleButton.visibility = View.VISIBLE
                shuffleOnStateButton.visibility = View.INVISIBLE
            }
        }

        private fun setVisibilityByRepeatMode(repeatMode: Int) {
            if (repeatMode == Player.REPEAT_MODE_OFF) {
                repeatOffButton.visibility = View.VISIBLE
                repeatOnButton.visibility = View.INVISIBLE
                repeatOneButton.visibility = View.INVISIBLE
            }
            else if (repeatMode == Player.REPEAT_MODE_ALL) {
                repeatOffButton.visibility = View.INVISIBLE
                repeatOnButton.visibility = View.VISIBLE
                repeatOneButton.visibility = View.INVISIBLE
            }
            else {
                repeatOffButton.visibility = View.INVISIBLE
                repeatOnButton.visibility = View.INVISIBLE
                repeatOneButton.visibility = View.VISIBLE
            }
        }


        private fun setViewByTrack() {
            favorite = mainActivityViewModel.currentTrack.value ?: favorite
            val track = favorite.track
            val metadata = favorite.metadata
            val primaryColor: Int? = favorite.track.primaryColor
            if (primaryColor != null) {
                setBackgroundColor(primaryColor)
            } else {
                ImageColorAnalyzer.analyzePrimaryColor(requireContext(), favorite.track.artworkUrl,
                    object : ImageColorAnalyzer.OnPrimaryColorAnalyzedListener {
                        override fun onSuccess(
                            dominantColor: Int,
                            primaryColor: Int,
                            selectedColor: Int,
                            unselectedColor: Int
                        ) {
                            setBackgroundColor(primaryColor)
                            val favoriteSongRepository = FavoriteSongRepository(requireContext())
                            favorite.track.primaryColor = primaryColor
                            mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
                                favoriteSongRepository.updateFavoriteSong(favorite, object : FavoriteSongRepository.FavoriteDbCallback {
                                    override fun onSuccess() {}

                                    override fun onFailure() {}
                                })
                            }
                        }

                        override fun onFailure() {}
                    })
            }

            Glide.with(requireContext())
                .asBitmap()
                .load(track.artworkUrl)
                .override(480, 480)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        artworkImage.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })

            titleTextView.text = favorite.title
            albumTitleTextView.text = track.albumName
            artistNameTextView.text = favorite.artistName
            durationTextView.text = favorite.durationStr
            releaseDateTextView.text = favorite.releaseDate
            lyricsTextView.text = "\n\n\n\n\n\n\n" + metadata.lyrics + "\n\n\n\n\n\n\n"

            pauseButton.setOnClickListener {
                mainActivityViewModel.togglePlayPause()
            }
            playButton.setOnClickListener {
                mainActivityViewModel.togglePlayPause()
            }

            val player = mainActivityViewModel.exoPlayer




            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        exoPlayer.seekTo(progress.toLong())
                        currentTime.text = formatDuration(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    exoPlayer.seekTo(seekBar?.progress?.toLong() ?: 0L)
                }
            })
            startSeekBarUpdate()
        }

        private fun startSeekBarUpdate() {
            updateSeekBarRunnable = object : Runnable {
                override fun run() {
                    if (exoPlayer.isPlaying && !isUserSeeking) {
                        val pos = exoPlayer.currentPosition
                        seekBar.progress = pos.toInt()
                        currentTime.text = formatDuration(pos.toInt())
                    }
                    handler.postDelayed(this, 100)
                }
            }
            handler.post(updateSeekBarRunnable!!)
        }

        override fun onStop() {
            super.onStop()
            updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
            // Visualizer 리소스 해제
            visualizer?.release()
            visualizer = null

            // ExoPlayer 리스너 제거
            playerListener?.let {
                mainActivityViewModel.exoPlayer?.removeListener(it)
            }
        }

        private fun formatDuration(ms: Int): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

        private fun setBackgroundColor(primaryColor: Int) {
            val context = context
            val darkenColor: Int

            if (context != null && DarkModeUtils.isDarkMode(context)) {
                darkenColor =
                    MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.3f)
                val adjustedPrimaryColorForDarkMode =
                    MyColorUtils.darkenHslColor(
                        MyColorUtils.ensureContrastWithWhite(primaryColor),
                        0.7f
                    )
                rootFrame.setCardBackgroundColor(adjustedPrimaryColorForDarkMode)
            } else {
                darkenColor =
                    MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f)
                rootFrame.setCardBackgroundColor(primaryColor)
            }

            val colorPair = MyColorUtils.generateBoundedContrastColors(
                MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.7f),
                0.9f, 0.15f, 0.1f, 0.9f, 0.29f, 0.9f
            )
            gradientColors = intArrayOf(colorPair[1], colorPair[0], colorPair[1])
            updateGradientShader()

            val adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor)
            simpleMusicInfoFrame.setCardBackgroundColor(adjustedForWhiteText)
            musicPlayingBarFrame.setBackgroundColor(adjustedForWhiteText)
            binding.seekFrame.setBackgroundColor(adjustedForWhiteText)

            lyricsTextView.setTextColor(
                MyColorUtils.adjustForWhiteText(
                    MyColorUtils.getSoftWhiteTextColor(adjustedForWhiteText)
                )
            )
        }

        private fun updateGradientShader() {
            val h = lyricsFrame.height.toFloat()
            val w = lyricsFrame.width.toFloat()
            val gradientLength = 0.85f * h
            val startY = (h - gradientLength) / 2f
            val endY = startY + gradientLength

            gradientPaint.shader = LinearGradient(
                0f, startY,
                0f, endY,
                gradientColors, null, Shader.TileMode.CLAMP
            )

            gradientPaint.maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
            gradientBackground.invalidate()
        }


        private fun startGradientRotation() {
            val clockwise = if (mainActivityViewModel.currentIndex % 2 == 0) 1.0f else -1.0f
            animator?.cancel()
            animator = ValueAnimator.ofFloat(currentRotation, currentRotation + clockwise * 360f).apply {
                duration = 20000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val value = it.animatedValue as Float
                    gradientBackground.rotation = value % 360
                    artworkImage.rotation = value % 360
                    currentRotation = value % 360  // 현재 각도 계속 저장
                }
                start()
            }
        }

        private fun stopGradientRotation() {
            animator?.cancel()
            currentRotation = gradientBackground.rotation % 360
        }

        companion object {
            const val TAG = "MusicPlayingBottomSheet"
            const val ARG_FAVORITE = "favorite"
        }
    }

    // 확장 함수: FrameLayout에서 배경만 회전해서 그림
    private fun FrameLayout.setOnDrawListener(block: (Canvas) -> Unit) {
        this.setWillNotDraw(false)
        this.invalidate()
        this.viewTreeObserver.addOnDrawListener {
            val canvas = Canvas()
            block(canvas)
        }
    }

package com.example.mymusic.main

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
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
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
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
    //private val artworkImage: ImageView by lazy { binding.focusedImage }
    //private val titleTextView: TextView by lazy { binding.focusedTitle }
    //private val albumTitleTextView: TextView by lazy { binding.focusedAlbumTitle }
    //private val artistNameTextView: TextView by lazy { binding.focusedArtist }
    //private val durationTextView: TextView by lazy { binding.focusedDuration }
    //private val releaseDateTextView: TextView by lazy { binding.focusedReleaseDate }
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val musicPlayingViewModel: MusicPlayingViewModel by activityViewModels()
    private val lyricsTextView: TextView by lazy { binding.lyrics }

    private val musicPlayingBarFrame: FrameLayout by lazy { binding.musicPlayingBar }
    private val rootFrame: MaterialCardView by lazy { binding.rootFrame }
    private val simpleMusicInfoFrame: MaterialCardView by lazy { binding.simpleMusicInfo }
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


    private var pagerAdapter: MusicPlayingPagerAdapter? = null
    private val musicInfoViewPager: ViewPager2 by lazy { binding.musicInfoViewPager }

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

        pagerAdapter = MusicPlayingPagerAdapter(this)
        musicInfoViewPager.adapter = pagerAdapter

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
                //binding.waveformView.startAnimation() // <<< 추가
                pauseButton.visibility = View.VISIBLE
                playButton.visibility = View.INVISIBLE
            }
            else {
                stopGradientRotation()
                //binding.waveformView.stopAnimation() // <<< 추가
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




        /**
         * end of bind()
         */
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
            ImageColorAnalyzer.analyzePrimaryColor(
                requireContext(), favorite.track.artworkUrl,
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
                            favoriteSongRepository.updateFavoriteSong(
                                favorite,
                                object : FavoriteSongRepository.FavoriteDbCallback {
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
                    //artworkImage.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        //titleTextView.text = favorite.title
        //albumTitleTextView.text = track.albumName
        //artistNameTextView.text = favorite.artistName
        //durationTextView.text = favorite.durationStr
        //releaseDateTextView.text = favorite.releaseDate
        lyricsTextView.text = "\n\n\n\n\n\n\n" + metadata.lyrics + "\n\n\n\n\n\n\n"

        pauseButton.setOnClickListener {
            mainActivityViewModel.togglePlayPause()
        }
        playButton.setOnClickListener {
            mainActivityViewModel.togglePlayPause()
        }


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
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.3f
                )
            val adjustedPrimaryColorForDarkMode =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.7f
                )
            rootFrame.setCardBackgroundColor(adjustedPrimaryColorForDarkMode)
        } else {
            darkenColor =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.9f
                )
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
            duration = musicPlayingViewModel.rotationDuration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                gradientBackground.rotation = value % 360
                //artworkImage.rotation = value % 360
                currentRotation = value % 360  // 현재 각도 계속 저장
                musicPlayingViewModel.setRotationAngle(currentRotation)
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
package com.example.mymusic.main

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.databinding.BottomSheetMusicPlayingBinding
import com.example.mymusic.main.pager.MusicPlayingPagerAdapter
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.Playlist
import com.example.mymusic.model.SessionKind
import com.example.mymusic.model.TrackMetadata
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class MusicPlayingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMusicPlayingBinding? = null
    private val binding get() = _binding!!
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
    private val mediaController: MediaController by lazy { mainActivityViewModel.mediaController!! }
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


    private val lyricsPage : TextView by lazy { binding.lyricsPage }
    private val separatorLineForLyrics : View by lazy { binding.lyricsSeparatorLine }

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


    private fun setLyricsPageVisibilityByScapeMode(){
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (musicPlayingViewModel.currentPage.value == 3) {
                musicPlayingViewModel.saveCurrentPageValue(2)
            }
            lyricsPage.visibility = View.GONE
            separatorLineForLyrics.visibility = View.GONE
            binding.lyricsCard.visibility = View.VISIBLE
        }
        else {
            lyricsPage.visibility = View.VISIBLE
            separatorLineForLyrics.visibility = View.VISIBLE
            binding.lyricsCard.visibility = View.GONE
        }
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
        setLyricsPageVisibilityByScapeMode()
        bind()
    }

    @OptIn(UnstableApi::class)
    private fun bind() {

        val tabs = listOf(
            0 to binding.playlistPage,
            1 to binding.musicInfoPage,
            2 to binding.playtimeCalendarPage,
            3 to binding.lyricsPage
        )

        fun updateAlpha(selectedIndex: Int) {
            tabs.forEachIndexed { index, pair ->
                pair.second.alpha = if (index == selectedIndex) 0.95f else 0.35f
            }
        }

        tabs.forEach { (index, textView) ->
            textView.setOnClickListener {
                musicPlayingViewModel.saveCurrentPageValue(index)
            }
        }

        musicPlayingViewModel.currentPage.observe(viewLifecycleOwner) { page ->
            updateAlpha(page)
            if (mainActivityViewModel.showBottomSheet.value == true){
                musicInfoViewPager.setCurrentItem(page, false)
                mainActivityViewModel.requestBottomSheet(false)
            }
            else {
                musicInfoViewPager.setCurrentItem(page, true)
            }
        }

        if (mainActivityViewModel.showBottomSheet.value == true) {
            musicPlayingViewModel.saveCurrentPageValue(1)
            musicInfoViewPager.offscreenPageLimit = 1
        }
        musicPlayingViewModel.requestDismiss(false)

        musicPlayingViewModel.requestDismiss.observe(viewLifecycleOwner) { dismiss ->
            Log.d(TAG, "dismiss observe called, dismiss " + dismiss)
            if (dismiss) {
                dismiss()
            }
        }


        pagerAdapter = MusicPlayingPagerAdapter(this)
        musicInfoViewPager.adapter = pagerAdapter

        musicInfoViewPager.isUserInputEnabled = false

        binding.scrollArea.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        lyricsTextView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }


        mainActivityViewModel.trackDuration.observe(viewLifecycleOwner) { duration ->
            Log.d(TAG, "Track Duration changed "+ duration )

            totalTimeTextView.text = formatDuration(duration)
            seekBar.max = duration

            setViewByTrack()
        }



        skipPreviousButton.setOnClickListener { mainActivityViewModel.playPrevious() }
        skipNextButton.setOnClickListener { mainActivityViewModel.playNext() }

        // 플레이 상태에 따라 회전 애니메이션 시작/정지
        mainActivityViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            if (playing) {
                //startGradientRotation()
                //binding.waveformView.startAnimation() // <<< 추가
                pauseButton.visibility = View.VISIBLE
                playButton.visibility = View.INVISIBLE
            }
            else {
                //stopGradientRotation()
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


        if (mainActivityViewModel.isDualSessionAvailable) {

        }
        else {
            binding.switchAdHoc.visibility = View.INVISIBLE
            binding.switchSaved.visibility = View.INVISIBLE
        }

        binding.switchSaved.setOnClickListener {
            mainActivityViewModel.toggleSession()
        }
        binding.switchAdHoc.setOnClickListener {
            mainActivityViewModel.toggleSession()
        }

        mainActivityViewModel.activeKind.observe(viewLifecycleOwner) { sessionType ->
            when (sessionType) {
                SessionKind.SAVED -> {
                    val currentPlaylist: Playlist? = mainActivityViewModel.getLastSelectedPlaylist()
                    if (currentPlaylist != null) {
                        binding.playlistName.visibility = View.VISIBLE
                        binding.playlistName.text = currentPlaylist.playlistName
                    }
                    else binding.playlistName.visibility = View.GONE
                }
                else -> { binding.playlistName.visibility = View.GONE }
            }
            if (mainActivityViewModel.isDualSessionAvailable) {
                if (sessionType == SessionKind.AD_HOC) {
                    binding.switchAdHoc.visibility = View.VISIBLE
                    binding.switchSaved.visibility = View.INVISIBLE
                } else {
                    binding.switchAdHoc.visibility = View.INVISIBLE
                    binding.switchSaved.visibility = View.VISIBLE
                }
            }
            else {
                binding.switchAdHoc.visibility = View.GONE
                binding.switchSaved.visibility = View.GONE
            }
        }

        /**
         * end of bind()
         */
    }





    private fun setVisibilityByShuffledMode(){
        if (mainActivityViewModel.shuffleMode.value == true) {
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


    @OptIn(UnstableApi::class)
    private fun setViewByTrack() {
        favorite = mainActivityViewModel.currentTrack.value ?: favorite
        val track = favorite.track
        val metadata: TrackMetadata? = favorite.metadata
        val artworkImage = binding.backgroudArtworkImage
        Glide.with(requireContext())
            .load(track.artworkUrl)
            .error(R.drawable.ic_image_not_found_foreground)
            .centerCrop()
            .into(artworkImage)

        val primaryColor: Int? = track.primaryColor

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
                        track.primaryColor = primaryColor
                        mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
                            favoriteSongRepository.updateFavoriteSongExceptPlayCount(
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



        lyricsTextView.text = "\n\n\n\n\n\n\n" + (metadata?.lyrics ?: "가사정보 없음")  + "\n\n\n\n\n\n\n"

        pauseButton.setOnClickListener {
            mainActivityViewModel.togglePlayPause()
        }
        playButton.setOnClickListener {
            mainActivityViewModel.togglePlayPause()
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaController.seekTo(progress.toLong())
                    currentTime.text = formatDuration(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                mediaController.seekTo(seekBar?.progress?.toLong() ?: 0L)
            }
        })

        startSeekBarUpdate()
    }

    private fun startSeekBarUpdate() {
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
        Log.d(TAG, "startSeekBarUpdate called")

        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                if (mediaController.isPlaying && !isUserSeeking) {
                    val pos = mediaController.currentPosition
                    seekBar.progress = pos.toInt()
                    currentTime.text = formatDuration(pos.toInt())
                }
                // **추가: SeekBar 진행 상태를 회전각으로 변환 (0~360도)**
                val max = seekBar.max.takeIf { it > 0 } ?: 1
                currentRotation = ((seekBar.progress.toFloat() / max) * 360f * 1f) % 360
                //Log.d(TAG, "current rotation: " + currentRotation)
                gradientBackground.rotation = currentRotation
                musicPlayingViewModel.setRotationAngle(currentRotation)
                handler.postDelayed(this, 30)
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }


    override fun onResume() {
        super.onResume()
        startSeekBarUpdate()
    }

    override fun onStop() {
        Log.d(TAG, "onStop called, removing callbacks")


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
            //binding.indicatorTextBar.setCardBackgroundColor(adjustedPrimaryColorForDarkMode)
        } else {
            darkenColor =
                MyColorUtils.darkenHslColor(
                    MyColorUtils.ensureContrastWithWhite(primaryColor),
                    0.9f
                )
            rootFrame.setCardBackgroundColor(primaryColor)
            //binding.indicatorTextBar.setCardBackgroundColor(darkenColor)
        }

        val colorPair = MyColorUtils.generateBoundedContrastColors(
            MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.45f),
            2.0f, 0.15f, 0.1f, 2.0f, 0.29f, 2.0f
        )

        gradientColors = intArrayOf(colorPair[1], colorPair[0], colorPair[1])
        updateGradientShader()

        val adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor)
        simpleMusicInfoFrame.setCardBackgroundColor(adjustedForWhiteText)

        binding.indicatorTextBar.setCardBackgroundColor(MyColorUtils.darkenHslColor(adjustedForWhiteText, 0.7f))

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
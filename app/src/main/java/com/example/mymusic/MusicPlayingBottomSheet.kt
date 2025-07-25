package com.example.mymusic
// 파일 상단에 이 import 문을 추가하거나 확인하세요.


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.databinding.BottomSheetMusicPlayingBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.ImageColorAnalyzer
import com.example.mymusic.util.MyColorUtils
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView


class MusicPlayingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMusicPlayingBinding? = null
    private val binding get() = _binding!!
    private val artworkImage : ImageView by lazy { binding.focusedImage }
    private val titleTextView: TextView by lazy { binding.focusedTitle }
    private val albumTitleTextView: TextView by lazy { binding.focusedAlbumTitle }
    private val artistNameTextView: TextView by lazy { binding.focusedArtist }
    private val durationTextView: TextView by lazy { binding.focusedDuration }
    private val releaseDateTextView: TextView by lazy { binding.focusedReleaseDate }
    private val recyclerView: RecyclerView by lazy { binding.playlistRecyclerView }
    private val mainActivityViewModel : MainActivityViewModel by activityViewModels()
    private val lyricsTextView: TextView by lazy { binding.lyrics }

    private val musicPlayingBarFrame: FrameLayout by lazy { binding.musicPlayingBar }
    private val rootFrame: MaterialCardView by lazy { binding.rootFrame }
    private val simpleMusicInfoFrame: MaterialCardView by lazy { binding.simpleMusicInfo }
    private val playlistFrame: MaterialCardView by lazy { binding.playlistFrame }
    private val lyricsFrame: FrameLayout by lazy { binding.lyricsFrame }

    private val seekBar by lazy { binding.seekBar }
    private val currentTime by lazy { binding.currentTime }
    private val totalTime by lazy { binding.totalTime }
    private val playButton by lazy { binding.audioPlayButton }
    private val pauseButton by lazy { binding.audioPauseButton }
    private val skipPreviousButton by lazy { binding.skipPrevious }
    private val skipNextButton by lazy { binding.skipNext }
    private var updateSeekBarRunnable: Runnable? = null
    private var handler = Handler(Looper.getMainLooper())
    private val exoPlayer: ExoPlayer by lazy { mainActivityViewModel.exoPlayer!! }
    private var isUserSeeking = false


    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return

        // 부모 패딩 제거
        (bottomSheet.parent as? ViewGroup)?.setPadding(0, 0, 0, 0)

        // BottomSheet 크기 강제 MATCH_PARENT
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        // Behavior 확장
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        // **Dialog 윈도우 자체도 강제 풀스크린**
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
        bind(view)
    }



    private fun bind(view: View) {

        binding.scrollArea.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        lyricsTextView.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true) // 부모 인터셉트 방지
            false
        }
        setView()

        mainActivityViewModel.currentTrack.observe(this) { favorite ->
            if (favorite == null) return@observe
            this.favorite = favorite
            setView()
        }

        skipPreviousButton.setOnClickListener { mainActivityViewModel.playPrevious() }
        skipNextButton.setOnClickListener { mainActivityViewModel.playNext() }
    }

    private fun setView() {
        val track = favorite.track
        val metadata = favorite.metadata

        val primaryColor : Int? = favorite.track.primaryColor
        if (primaryColor != null) {
            setBackgroundColor(primaryColor)
        } else{
            ImageColorAnalyzer.analyzePrimaryColor(requireContext(), favorite.track.artworkUrl,
                object : ImageColorAnalyzer.OnPrimaryColorAnalyzedListener {
                    override fun onSuccess(
                        dominantColor: Int,
                        primaryColor: Int,
                        selectedColor: Int,
                        unselectedColor: Int
                    ) {
                        setBackgroundColor(primaryColor)
                    }

                    override fun onFailure() {}

                })
        }

        Glide.with(requireContext())
            .asBitmap()
            .load(track.artworkUrl)
            .override(480, 480)
            .into(object : CustomTarget<Bitmap> (){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    artworkImage.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

            })

        titleTextView.text = favorite.title
        albumTitleTextView.text = track.albumName
        artistNameTextView.text = favorite.artistName
        durationTextView.text = favorite.durationStr
        releaseDateTextView.text = favorite.releaseDate
        lyricsTextView.text =  "\n\n\n\n\n\n\n" + metadata.lyrics + "\n\n\n\n\n\n\n"

        pauseButton.setOnClickListener {
            mainActivityViewModel.togglePlayPause()
            pauseButton.visibility = View.INVISIBLE
            playButton.visibility = View.VISIBLE
        }
        playButton.setOnClickListener {
            mainActivityViewModel.togglePlayPause()
            playButton.visibility = View.INVISIBLE
            pauseButton.visibility = View.VISIBLE
        }

        val player = mainActivityViewModel.exoPlayer
        if (player != null) {
            totalTime.text = formatDuration(player.duration.toInt())
            seekBar.max = player.duration.toInt()
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { // 드래그 중일 때만 seekTo
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

        /**
         * end of set view
         */
    }

    private fun startSeekBarUpdate() {
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying && !isUserSeeking) { // 드래그 중이 아닐 때만 UI 업데이트
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
            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.3f)
            val adjustedPrimaryColorForDarkMode =
                MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.7f)
            rootFrame.setCardBackgroundColor(adjustedPrimaryColorForDarkMode)
        } else {
            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f)
            rootFrame.setCardBackgroundColor(primaryColor)
        }

        /**
         * 배경 그라디언트로 수정
         */
        if (context != null) {
            val colorPair = MyColorUtils.generateBoundedContrastColors(
                MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.7f),
                0.85f, 0.15f, 0.2f, 0.45f, 0.29f, 0.3f
            )
            gradiantToFrame(lyricsFrame, colorPair[0], colorPair[1])
        }

        val adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor)
        simpleMusicInfoFrame.setCardBackgroundColor(adjustedForWhiteText)
        musicPlayingBarFrame.setBackgroundColor(adjustedForWhiteText)
        binding.seekFrame.setBackgroundColor(adjustedForWhiteText)

        lyricsTextView.setTextColor(
            MyColorUtils.adjustForWhiteText(
                MyColorUtils.getSoftWhiteTextColor(adjustedForWhiteText)
            )
        )
        //lyricsTextView.setShadowLayer(0.25f, 0.25f, 0.25f, Color.BLACK)
        lyricsTextView.setTextColor(
            MyColorUtils.adjustForWhiteText(
                MyColorUtils.getSoftWhiteTextColor(adjustedForWhiteText)
            )
        )
    }

    private fun gradiantToFrame(frameLayout: FrameLayout, brightenColor: Int, darkenColor: Int) {
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(darkenColor, brightenColor, darkenColor)
        )
        gradient.cornerRadius = 0f // 필요 시 곡률
        frameLayout.background = gradient
    }


    companion object {
        const val TAG = "musicPlayingBottomSheet"
        const val ARG_FAVORITE = "favorite"
    }
}

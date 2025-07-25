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
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.customView.WaveformView
import com.example.mymusic.dataLoader.FavoriteArtistLoader
import com.example.mymusic.databinding.FragmentArtworkWithWaveFormBinding
import com.example.mymusic.model.Artist
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.FavoriteArtist
import com.example.mymusic.network.ArtistApiHelper
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment
import com.example.mymusic.ui.musicInfo.MusicInfoFragment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtworkWithWaveFormFragment : Fragment() {

    private var _binding : FragmentArtworkWithWaveFormBinding? = null
    private val binding get() = _binding!!

    private val artworkImage: ImageView by lazy { binding.focusedImage }
    private val titleTextView: TextView by lazy { binding.focusedTitle }
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

    private val exoPlayer: ExoPlayer by lazy { mainActivityViewModel.exoPlayer!! }
    private var isUserSeeking = false
    private var seekBar: SeekBar? = null

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

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
        // 화면 다시 켜질 때 Visualizer 재연결
        val sessionId = mainActivityViewModel.exoPlayer?.audioSessionId ?: 0
        if (sessionId != 0) {
            setupVisualizerWithRetry(sessionId)
        }
    }

    override fun onPause() {
        super.onPause()
        // 화면 나갈 때 Visualizer 중단
        releaseVisualizer()
    }

    @OptIn(UnstableApi::class)
    private fun bind() {
        musicPlayingViewModel.artworkImage = this.artworkImage

        favorite = mainActivityViewModel.currentTrack.value
        favorite?.let { updateData(it) }
        mainActivityViewModel.currentTrack.observe(viewLifecycleOwner) { favoriteSync ->
            favoriteSync?.let {
                this.favorite = favoriteSync
                updateData(favoriteSync)
            }
        }

        /*
        mainActivityViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            if (playing){
                startArtworkImageRotation()
            }
            else {
                stopArtworkImageRotation()
            }
        }

         */


        artworkImage.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        musicPlayingViewModel.rotationAngle.observe(viewLifecycleOwner) {
            val currentRotation = musicPlayingViewModel.rotationAngle.value ?: 0f
            binding.focusedImageFrame.rotation = currentRotation
        }




        // 오디오 세션 변경 리스너
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


        titleTextView.setOnClickListener {
            val navController = findNavController()
            val args = Bundle().apply {
                putParcelable(MusicInfoFragment.ARGUMENTS_KEY, favorite)
            }
            musicPlayingViewModel.requestDismiss(true)
            navController.navigate(R.id.musicInfoFragment, args)
        }

        artistNameTextView.setOnClickListener {
            favorite?.let {
                val navController = findNavController()
                val args = Bundle()
                FavoriteArtistLoader.loadFavoriteArtistById(
                    requireContext(),
                    it.track.artistId,
                    object : FavoriteArtistLoader.Companion.OnLoadListener {
                        override fun onLoadSuccess(fav: FavoriteArtist) {
                            args.putParcelable(ArtistInfoFragment.ARGUMENTS_KEY, fav)
                            musicPlayingViewModel.requestDismiss(true)
                            navController.navigate(R.id.artist_info, args)
                        }
                        override fun onLoadFailed() {
                            mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
                                val artistApiHelper = ArtistApiHelper(requireContext())
                                artistApiHelper.getArtist(null, it.track.artistId, object : Consumer<Artist> {
                                    override fun accept(value: Artist) {
                                        mainActivityViewModel.viewModelScope.launch(Dispatchers.Main) {
                                            val searched = FavoriteArtist(value)
                                            args.putParcelable(ArtistInfoFragment.ARGUMENTS_KEY, searched)
                                            musicPlayingViewModel.requestDismiss(true)
                                            navController.navigate(R.id.artist_info, args)
                                        }
                                    }

                                })
                            }
                        }

                    })
            }
        }
    }

    private fun updateData(currentFavorite: Favorite) {
        Glide.with(requireContext())
            .asBitmap()
            .load(currentFavorite.track.artworkUrl)
            .override(720, 720)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    artworkImage.setImageBitmap(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        val titleAndAlbum = currentFavorite.title + " - " + currentFavorite.track.albumName
        titleTextView.text = titleAndAlbum
        titleTextView.isSelected = true
        artistNameTextView.text = currentFavorite.artistName
        durationTextView.text = currentFavorite.durationStr
        releaseDateTextView.text = currentFavorite.releaseDate
    }

    /*
    private fun startArtworkImageRotation() {
        val clockwise = if (mainActivityViewModel.currentIndex % 2 == 0) 1.0f else -1.0f
        animator?.cancel()
        var currentRotation = musicPlayingViewModel.rotationAngle.value ?: 180f
        animator = ValueAnimator.ofFloat(currentRotation, currentRotation + clockwise * 360f).apply {
            duration = musicPlayingViewModel.rotationDuration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = (it.animatedValue as Float) % 360
                currentRotation = value
                musicPlayingViewModel.setRotationAngle(currentRotation)
                artworkImage.rotation = currentRotation
            }
            start()
        }
    }
     */

    private fun stopArtworkImageRotation() {
        animator?.cancel()
    }

    // === 권한 ===
    private val requesetPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "RECORD_AUDIO 권한이 허용되었습니다.")
                val sessionId = mainActivityViewModel.exoPlayer?.audioSessionId ?: 0
                if (sessionId != 0) {
                    setupVisualizerWithRetry(sessionId)
                }
            } else {
                Log.w(TAG, "RECORD_AUDIO 권한이 거부되었습니다.")
                waveformView.visibility = View.GONE
            }
        }

    @OptIn(UnstableApi::class)
    private fun checkAndRequestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                val sessionId = mainActivityViewModel.exoPlayer?.audioSessionId ?: 0
                if (sessionId != 0) {
                    setupVisualizerWithRetry(sessionId)
                }
            }
            else -> {
                requesetPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // === Visualizer ===
    private fun setupVisualizerWithRetry(sessionId: Int) {
        visualizerRetryAttempt = 0
        initVisualizer(sessionId)
    }

    private fun initVisualizer(sessionId: Int) {
        if (visualizerRetryAttempt >= VISUALIZER_MAX_RETRIES) {
            Log.e(TAG, "Visualizer failed after $VISUALIZER_MAX_RETRIES attempts.")
            waveformView.visibility = View.GONE
            return
        }

        try {
            releaseVisualizer()

            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                    ) {
                        waveform?.let { binding.waveformView.updateWaveform(it) }
                    }
                    override fun onFftDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
            waveformView.visibility = View.VISIBLE
            Log.d(TAG, "Visualizer initialized.")
        } catch (e: Exception) {
            visualizerRetryAttempt++
            Log.w(TAG, "Visualizer init failed: ${e.message}")
            handler.postDelayed({ initVisualizer(sessionId) }, VISUALIZER_RETRY_DELAY_MS)
        }
    }

    private fun releaseVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
    }

    override fun onStop() {
        super.onStop()
        releaseVisualizer()
        playerListener?.let { mainActivityViewModel.exoPlayer?.removeListener(it) }
    }

    companion object {
        const val TAG = "ArtworkWithWaveForm"
    }
}

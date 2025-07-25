package com.example.mymusic

import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mymusic.model.Favorite
import com.google.android.exoplayer2.ExoPlayer

class MainActivityViewModel : ViewModel() {

    val currentTrack = MutableLiveData<Favorite?>(null)
    var exoPlayer: ExoPlayer? = null
    var isPlaying = MutableLiveData<Boolean>(false)
    var lastPlayedTrack: Favorite? = null

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

}
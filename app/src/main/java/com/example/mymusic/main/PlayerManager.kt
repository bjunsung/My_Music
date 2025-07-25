package com.example.mymusic.main

import androidx.media3.exoplayer.ExoPlayer

// 앱 전체에서 ExoPlayer를 유일하게 하나만 유지하기 위한 객체
object PlayerManager {
    var exoPlayer: ExoPlayer? = null
}
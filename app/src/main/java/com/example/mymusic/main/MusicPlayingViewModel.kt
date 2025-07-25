package com.example.mymusic.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicPlayingViewModel : ViewModel() {
    private val _rotationAngle = MutableLiveData<Float>(0f)
    val rotationAngle: LiveData<Float> get() = _rotationAngle

    val rotationDuration = 20000L
    fun setRotationAngle(angle: Float) {
        _rotationAngle.value = angle
    }

}
package com.example.mymusic.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicPlayingViewModel : ViewModel() {
    private val _rotationAngle = MutableLiveData<Float>(0f)
    val rotationAngle: LiveData<Float> get() = _rotationAngle

    private val _currentPage = MutableLiveData<Int>(0)
    val currentPage get(): LiveData<Int> = _currentPage!!

    fun saveCurrentPageValue(page: Int){
        _currentPage.value = page
    }

    val rotationDuration = 20000L
    fun setRotationAngle(angle: Float) {
        _rotationAngle.value = angle
    }

}
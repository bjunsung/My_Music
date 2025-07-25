package com.example.mymusic.main

import android.widget.ImageView
import android.widget.SeekBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class MusicPlayingViewModel : ViewModel() {
    private val _rotationAngle = MutableLiveData(0f)
    val rotationAngle: LiveData<Float> get() = _rotationAngle

    private val _currentPage = MutableLiveData(0)
    val currentPage get(): LiveData<Int> = _currentPage

    val _lastData = MutableLiveData<List<ContributionDay>>()
    val lastData get() = _lastData

    fun saveCurrentPageValue(page: Int){
        _currentPage.value = page
    }

    fun setRotationAngle(angle: Float) {
        _rotationAngle.value = angle
    }

    private val _requestDismiss = MutableLiveData(false)
    val requestDismiss get() = _requestDismiss

    fun requestDismiss(dismiss: Boolean) {
        this._requestDismiss.value = dismiss
    }



    var artworkImage: ImageView? = null


}
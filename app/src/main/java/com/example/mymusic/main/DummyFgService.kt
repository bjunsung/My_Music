package com.example.mymusic.main

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DummyFgService : Service() {
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {

        stopSelf()                       // 즉시 종료 (알림은 유지)
        return START_NOT_STICKY
    }
    override fun onBind(i: Intent?) : IBinder? = null
}

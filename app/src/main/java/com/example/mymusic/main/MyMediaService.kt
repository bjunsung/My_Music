package com.example.mymusic.main

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.mymusic.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MyMediaService : MediaSessionService() {

    private var player: Player? = null
    private var mediaSession: MediaSession? = null

    // ✅ ViewModel/Fragment와 주고받을 요청 '암호'와 '키'를 정의합니다.
    companion object {
        const val COMMAND_GET_AUDIO_SESSION_ID = "com.example.mymusic.GET_AUDIO_SESSION_ID"
        const val KEY_AUDIO_SESSION_ID = "audio_session_id"
    }

    /**앱 종료시 플레이 종료*/
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        ensurePlaybackChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val newPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()


        val newMediaSession = MediaSession.Builder(this, newPlayer)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setCallback(SessionCb())
            .build()

        this.player = newPlayer
        this.mediaSession = newMediaSession
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // ✅ MediaSession.Callback 구현 부분
    private class SessionCb : MediaSession.Callback {
        @SuppressLint("WrongConstant")
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {

            // ✅ "ID를 물어볼 수 있는 명령어"를 허용 목록에 추가합니다.
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY))
                .build()

            val availablePlayerCommands = Player.Commands.Builder()
                .addAll(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(availablePlayerCommands) // ✅ 수정된 명령어 목록을 전달
                .build()
        }

        // ✅ "ID 알려줘" 라는 커스텀 요청을 받았을 때 실행될 코드를 추가합니다.
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {

            // 우리가 정의한 요청 '암호'와 일치하는지 확인합니다.
            if (customCommand.customAction == COMMAND_GET_AUDIO_SESSION_ID) {
                // 현재 플레이어의 audioSessionId를 가져옵니다.
                val sessionId = (session.player as ExoPlayer).audioSessionId

                Log.d("MyMediaService", "session id: " + sessionId)
                // 결과를 담을 '답장'용 Bundle을 만듭니다.
                val resultBundle = Bundle().apply {
                    putInt(KEY_AUDIO_SESSION_ID, sessionId)
                }

                // "성공했고, 답장은 이것이다" 라고 즉시 회신합니다.
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
            }
            // 모르는 요청이면 기본 처리를 따릅니다.
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun ensurePlaybackChannel() {
        val channelId = "playback_channel"
        if (getSystemService(NotificationManager::class.java).getNotificationChannel(channelId) == null) {
            val notificationChannel = NotificationChannel(
                channelId,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(notificationChannel)
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.release() // Nullable player
            release()
            mediaSession = null
        }
        super.onDestroy()
    }


}
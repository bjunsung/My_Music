package com.example.mymusic.main

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.mymusic.MainActivity
import com.example.mymusic.R

@UnstableApi
class MyMediaService : MediaSessionService() {

    private var player: Player? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        ensurePlaybackChannel() // 알림 채널 생성

        val newPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
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

    private class SessionCb : MediaSession.Callback {
        @SuppressLint("WrongConstant")
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()

            val customLayout = listOf(
                CommandButton.Builder().setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build(),
                CommandButton.Builder().setPlayerCommand(Player.COMMAND_PLAY_PAUSE).build(),
                CommandButton.Builder().setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).build()
            )

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setCustomLayout(customLayout)
                .build()
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
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
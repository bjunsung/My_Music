package com.example.mymusic.main

import android.app.NotificationManager
import androidx.media3.common.util.UnstableApi

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mymusic.R

@UnstableApi
object MediaSessionHelper {

    private var mediaSession: MediaSession? = null
    private var pnManager: PlayerNotificationManager? = null

    private const val NOTI_ID = 0x112
    private const val ACTION_SHUFFLE = "ACTION_SHUFFLE"
    private const val ACTION_LIKE = "ACTION_LIKE"
    // 캐시 비트맵 변수 (Helper 파일 최상단에 추가)
    private var cachedLarge: Bitmap? = null

    @JvmStatic
    fun attach(player: Player, context: Context) {
        /* ── 1) MediaSession ───────────────────── */
        if (mediaSession == null) {
            mediaSession = MediaSession.Builder(context, player).build()
        }

        /* ── 2) PlayerNotificationManager ──────── */
        if (pnManager == null) {
            // ─────────── MediaSessionHelper.kt 내부 ───────────
            pnManager = PlayerNotificationManager.Builder(
                context,
                NOTI_ID,
                NotificationUtils.CHANNEL_ID
            )
                .setSmallIconResourceId(R.drawable.ic_play_arrow)
                // ↓↓↓ 이 블록만 통째로 복사해 넣으세요
                .setMediaDescriptionAdapter(object :
                    PlayerNotificationManager.MediaDescriptionAdapter {

                    override fun getCurrentContentTitle(player: Player): CharSequence =
                        player.mediaMetadata.title ?: "(제목 없음)"

                    override fun getCurrentContentText(player: Player): CharSequence? =
                        player.mediaMetadata.artist

                    override fun createCurrentContentIntent(player: Player): PendingIntent? = null

                    /** Galaxy 퀵패널·잠금화면이 쓰는 LargeIcon */
                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ): Bitmap? {

                        val artUri = player.mediaMetadata.artworkUri ?: return null

                        // 캐시가 있으면 즉시 반환 → 큰 카드 조건 통과
                        cachedLarge?.let { return it }

                        // 비동기 다운로드 (Main 스레드 안전)
                        Glide.with(context)
                            .asBitmap()
                            .load(artUri)
                            .centerCrop()
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    // 400px 미만이면 업스케일
                                    val large = if (resource.width < 400 || resource.height < 400) {
                                        Bitmap.createScaledBitmap(resource, 600, 600, true)
                                    } else resource

                                    cachedLarge = large          // 다음 호출엔 즉시 반환
                                    callback.onBitmap(large)     // 알림 LargeIcon 갱신
                                }



                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })

                        // placeholder (400px↑ 검정 비트맵) 반환 → 첫 호출도 큰 카드 사용
                        return Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
                    }
                })
                .setCustomActionReceiver(CustomReceiver(context))
                .setChannelImportance(NotificationManager.IMPORTANCE_DEFAULT) // 중요도 DEFAULT
                .build()
                .apply {
                    setPriority(NotificationCompat.PRIORITY_DEFAULT)          // priority 0
                    setUsePreviousAction(true)
                    setUseNextAction(true)
                    setPlayer(player)
                }
// 이미 setPlayer(player) 까지 있는 블록의 맨 아래에 ↓↓↓ 붙여넣기
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 15+
                // 1) 알림 객체를 먼저 만들어 둠

                // 2) 시스템 NotificationManager 에 등록
                val nMgr = context.getSystemService(NotificationManager::class.java)

                // 3) 더미 포그라운드 서비스 시작 → 플래그 붙이기
                val intent = Intent(context, DummyFgService::class.java)

            }



        }
    }

    /* 커스텀 버튼(셔플·좋아요) 정의 */
    private class CustomReceiver(private val ctx: Context) :
        PlayerNotificationManager.CustomActionReceiver {

        override fun getCustomActions(player: Player) =
            listOf(ACTION_SHUFFLE, ACTION_LIKE)

        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): Map<String, NotificationCompat.Action> = mapOf(
            ACTION_SHUFFLE to makeAction(R.drawable.ic_round_shuffle, "셔플", ACTION_SHUFFLE, instanceId),
            ACTION_LIKE    to makeAction(R.drawable.ic_play_arrow,    "좋아요", ACTION_LIKE,    instanceId)
        )

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            when (action) {
                ACTION_SHUFFLE -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                ACTION_LIKE    -> { /* TODO: 좋아요 처리 */ }
            }
        }

        private fun makeAction(
            icon: Int,
            title: String,
            action: String,
            id: Int
        ) = NotificationCompat.Action(
            icon, title,
            PendingIntent.getBroadcast(
                ctx, id,
                Intent(action).setPackage(ctx.packageName)
                    .putExtra(PlayerNotificationManager.EXTRA_INSTANCE_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

}


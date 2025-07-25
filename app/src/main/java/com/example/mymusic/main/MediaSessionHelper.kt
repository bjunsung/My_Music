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
    // ✅ 캐시와 타깃을 항목 키(mediaId 또는 artworkUrl) 기준으로 관리
    private var cachedArtworkKey: String? = null
    private var cachedLarge: Bitmap? = null
    private var currentArtTarget: CustomTarget<Bitmap>? = null

    @JvmStatic
    fun attach(player: Player, context: Context) {
        if (mediaSession == null) {
            mediaSession = MediaSession.Builder(context, player).build()
        }

        if (pnManager == null) {
            pnManager = PlayerNotificationManager.Builder(
                context, NOTI_ID, NotificationUtils.CHANNEL_ID
            )
                .setSmallIconResourceId(R.drawable.ic_play_arrow)
                .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {

                    override fun getCurrentContentTitle(player: Player): CharSequence =
                        player.currentMediaItem?.mediaMetadata?.title ?: "(제목 없음)"

                    override fun getCurrentContentText(player: Player): CharSequence? =
                        player.currentMediaItem?.mediaMetadata?.artist

                    override fun createCurrentContentIntent(player: Player): PendingIntent? = null

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ): Bitmap? {
                        val item = player.currentMediaItem ?: return null
                        val mm = item.mediaMetadata

                        // ✅ 1) artworkUri는 currentMediaItem에서 가져오기
                        val artUri = mm.artworkUri
                        // (보조) 내장 바이너리 아트가 있으면 이것도 시도 가능
                        val artworkData = mm.artworkData

                        // 키: mediaId가 있으면 그걸, 없으면 URL
                        val key = item.mediaId.takeIf { it.isNotEmpty() }
                            ?: artUri?.toString()
                            ?: artworkData?.hashCode()?.toString()
                            ?: return null

                        // 캐시 히트면 즉시 반환
                        if (cachedArtworkKey == key && cachedLarge != null) return cachedLarge

                        // 이전 타깃 클리어(중복 로드 방지)
                        currentArtTarget?.let { Glide.with(context).clear(it) }
                        currentArtTarget = null

                        // ✅ 2) 비동기 로드 (타깃을 필드에 저장해 수명 보장)
                        if (artUri != null) {
                            currentArtTarget = object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    val bmp = ensureMinSize(resource, 600)
                                    cachedArtworkKey = key
                                    cachedLarge = bmp
                                    callback.onBitmap(bmp)
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {}
                            }

                            Glide.with(context)
                                .asBitmap()
                                .load(artUri)
                                .centerCrop()
                                .signature(com.bumptech.glide.signature.ObjectKey(key))
                                .into(currentArtTarget!!)
                        } else if (artworkData != null) {
                            // artworkData 사용 경로 (URI가 없을 때)
                            val bmp = decodeBitmap(artworkData)
                            if (bmp != null) {
                                val ready = ensureMinSize(bmp, 600)
                                cachedArtworkKey = key
                                cachedLarge = ready
                                callback.onBitmap(ready)
                            }
                        }

                        // 첫 호출에 굳이 플레이스홀더를 넣고 싶지 않다면 null 반환
                        return null
                    }
                })
                //.setCustomActionReceiver(CustomReceiver(context))
                .setChannelImportance(NotificationManager.IMPORTANCE_DEFAULT)
                .build()
                .apply {
                    setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    setUsePreviousAction(true)
                    setUseNextAction(true)
                    setPlayer(player)
                }

            // ✅ 3) 전환/메타데이터 변경 시 강제 갱신 + 캐시 초기화
            player.addListener(object : Player.Listener {
                override fun onMediaItemTransition(
                    item: androidx.media3.common.MediaItem?, reason: Int
                ) {
                    invalidateArtwork()
                }
                override fun onMediaMetadataChanged(
                    mediaMetadata: androidx.media3.common.MediaMetadata
                ) {
                    invalidateArtwork()
                }
            })
        }
    }

    private fun invalidateArtwork() {
        cachedArtworkKey = null
        cachedLarge = null
        currentArtTarget?.let { /* 그대로 두면 됩니다. 다음 로드에서 교체/clear */ }
        pnManager?.invalidate()
    }
    // 유틸: 너무 작은 이미지면 키워서 흐릿함 완화(선택)
    private fun ensureMinSize(src: Bitmap, min: Int): Bitmap {
        if (src.width >= min && src.height >= min) return src
        val w = maxOf(min, src.width)
        val h = maxOf(min, src.height)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    // 유틸: artworkData → Bitmap (필요시)
    private fun decodeBitmap(bytes: ByteArray): Bitmap? =
        try { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } catch (_: Throwable) { null }

/*
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

 */

}


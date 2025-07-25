package com.example.mymusic.model

enum class SessionKind { AD_HOC, SAVED }

data class SessionSnapshot(
    val kind: SessionKind,
    val trackIds: List<String>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffled: Boolean,
    val playlistId: String? = null,
    val accumulatedListenTimeMs: Long = 0L // ✅ [추가] 누적 청취 시간
)

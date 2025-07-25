package com.example.mymusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.mymusic.data.local.converter.ConvertersKts
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "playlist_table")
@TypeConverters(ConvertersKts::class)
data class PlaylistEntity @JvmOverloads constructor(
    @PrimaryKey
    val playlistId: String = UUID.randomUUID().toString(), // ✅ 자동 ID 생성
    val playlistName: String,
    val trackIds: List<String>? = null,              // ✅ 변수명 명확화
    val totalDurationSec: Int = 0,                          // ✅ 명확한 단위
    val createdDate: LocalDate = LocalDate.now(),
    val lastPlayedTimeMs: Long? = null,
    val playCount: Int = 0
)

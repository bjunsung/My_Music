package com.example.mymusic.model

import android.os.Parcel
import android.os.Parcelable
import java.time.LocalDate
import java.util.UUID

data class Playlist(
    val playlistId: String = UUID.randomUUID().toString(),
    var playlistName: String,
    val trackIds: MutableList<String> = mutableListOf(),
    var totalDurationSec: Int = 0,
    val createdDate: LocalDate = LocalDate.now(),
    var lastPlayedTimeMs: Long? = null,
    var playCount: Int = 0,
    var favorites: List<Favorite>  = emptyList()
) : Parcelable{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Playlist) return false
        return playlistId == other.playlistId &&
                playlistName == other.playlistName &&
                trackIds == other.trackIds &&
                totalDurationSec == other.totalDurationSec &&
                createdDate == other.createdDate &&
                lastPlayedTimeMs == other.lastPlayedTimeMs &&
                playCount == other.playCount &&
                favorites == other.favorites
    }

    override fun hashCode(): Int {
        var result = playlistId.hashCode()
        result = 31 * result + playlistName.hashCode()
        result = 31 * result + trackIds.hashCode()
        result = 31 * result + totalDurationSec
        result = 31 * result + createdDate.hashCode()
        result = 31 * result + (lastPlayedTimeMs?.hashCode() ?: 0)
        result = 31 * result + playCount
        result = 31 * result + favorites.hashCode()
        return result
    }
    fun deepCopy() = Playlist(
        playlistId,
        playlistName,
        trackIds,
        totalDurationSec,
        createdDate,
        lastPlayedTimeMs,
        playCount,
        favorites
    )
    // 원본 정의가 MutableList라면 새 인스턴스로 복제해서 넘겨야 함
    fun copyWithTrackIds(newIds: List<String>): Playlist =
        this.copy(
            trackIds = newIds.toMutableList()
        )

    fun shuffle() {
        this.favorites = favorites.shuffled()
    }

    /** 중복은 무시하고 추가 */
    fun addTracksIgnoreDuplicates(newIds: List<String>) {
        val seen = trackIds.toHashSet()
        for (id in newIds) {
            if (seen.add(id)) trackIds.add(id)
        }
    }

    /** 중복이 있으면 그 항목을 앞으로 이동 (맨 앞에 쌓임) */
    fun addTracksMoveDuplicatesToBack(newIds: List<String>, maxQueueSize: Int? = null) {
        for (id in newIds) {
            trackIds.remove(id)
            trackIds.add(trackIds.size, id)
        }
        enforceLimit(maxQueueSize)
    }

    /** 여러 개 일괄 삭제 */
    fun removeTracks(removeIds: Set<String>) {
        if (removeIds.isEmpty()) return
        val removeSet = removeIds.toHashSet()
        trackIds.removeAll(removeSet)
    }

    private fun enforceLimit(maxQueueSize: Int?) {
        if (maxQueueSize == null) return
        // 큐 방식: 초과 시 앞에서부터 제거
        if (trackIds.size > maxQueueSize) {
            val dropCount = trackIds.size - maxQueueSize
            repeat(dropCount) { if (trackIds.isNotEmpty()) trackIds.removeAt(0) }
        }
    }

    fun getUrls() : List<String> {
        return favorites.map { it.track.artworkUrl }
    }

    private fun calculateDurationSec(favorites: Set<Favorite>) {
        val totalDurationMs = favorites.map { it.duration }.sum()
        totalDurationSec = totalDurationMs / 1000
    }

    fun getDurationStr(): String {
        if (totalDurationSec == 0) return "0분"

        val hours = totalDurationSec / 3600
        val minutes = (totalDurationSec % 3600) / 60
        val seconds = totalDurationSec % 60

        // 24시간 초과 처리
        if (hours >= 24) return "24시간 +"

        return buildString {
            if (hours > 0) append("${hours}시간")
            if (minutes > 0) {
                if (isNotEmpty()) append(" ")
                append("${minutes}분")
            }
            if (seconds > 0) {
                if (isNotEmpty()) append(" ")
                append("${seconds}초")
            }
        }.trim()
    }

    fun numberOfTracks(): Int = trackIds.size
    fun isEmpty(): Boolean = (trackIds.size == 0)
    fun isNotEmpty(): Boolean = !isEmpty()



    // Parcelable 구현
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(playlistId)
        dest.writeString(playlistName)
        dest.writeStringList(trackIds)
        dest.writeInt(totalDurationSec)
        dest.writeString(createdDate.toString()) // LocalDate → String
        dest.writeValue(lastPlayedTimeMs)        // Long? → Nullable 가능
        dest.writeInt(playCount)
        // favorites는 무거울 수 있으니 Parcel에 안 넣는 선택 가능 (필요하면 Parcelable 구현 추가)
    }

    override fun toString(): String {
        return "${playlistName} track count: ${favorites.size}"
    }


    companion object CREATOR : Parcelable.Creator<Playlist> {
        override fun createFromParcel(parcel: Parcel): Playlist {
            val playlistId = parcel.readString() ?: UUID.randomUUID().toString()
            val playlistName = parcel.readString() ?: ""
            val trackIds = parcel.createStringArrayList()?.toMutableList() ?: mutableListOf()
            val totalDurationSec = parcel.readInt()
            val createdDate = LocalDate.parse(parcel.readString() ?: LocalDate.now().toString())
            val lastPlayedTimeMs = parcel.readValue(Long::class.java.classLoader) as? Long
            val playCount = parcel.readInt()

            return Playlist(
                playlistId,
                playlistName,
                trackIds,
                totalDurationSec,
                createdDate,
                lastPlayedTimeMs,
                playCount
            )
        }

        override fun newArray(size: Int): Array<Playlist?> = arrayOfNulls(size)
    }

}

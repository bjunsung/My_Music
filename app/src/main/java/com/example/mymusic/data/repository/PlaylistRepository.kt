package com.example.mymusic.data.repository

import android.content.Context
import com.example.mymusic.data.local.AppDatabase
import com.example.mymusic.data.local.PlaylistEntity
import com.example.mymusic.data.local.PlaylistDao
import com.example.mymusic.model.Playlist

class PlaylistRepository(
    context: Context
) {
    private val favoriteSongRepository: FavoriteSongRepository = FavoriteSongRepository(context)
    private val maxQueueSize: Int = 50
    private val dao: PlaylistDao = AppDatabase.getInstance(context).playlistDao()

    /** 모델을 받아 DB에 저장 (신규/갱신 모두 REPLACE 전략으로 처리된다고 가정) */
    suspend fun create(playlist: Playlist): Playlist {
        dao.insertPlaylist(playlist.toEntity())
        return playlist
    }

    /** ID로 조회해서 모델 반환 */
    suspend fun getById(playlistId: String): Playlist? =
        dao.getById(playlistId)?.toModel()

    // PlaylistRepository.kt
    suspend fun getByIdWithFavorites(playlistId: String): Playlist? {
        val entity = dao.getById(playlistId) ?: return null
        val model = entity.toModel()
        val favorites = favoriteSongRepository.getFavoritesByIdsIncludeHidden(model.trackIds)
        model.favorites = favorites

        if (model.trackIds.size != favorites.size) {
            model.totalDurationSec = favorites.sumOf { it.duration } / 1000
            val loadSuccessIds = favorites.map { it.track.trackId }.toSet()
            val toRemove = model.trackIds.filter { it !in loadSuccessIds }.toSet()
            model.removeTracks(toRemove)
            dao.updatePlaylist(model.toEntity())
            return model
        }
        return model
    }

    /** 전체 조회(모델 리스트) */
    suspend fun getAll(): List<Playlist> = dao.getAll().map { it.toModel() }

    suspend fun getAllWithFavorites(): List<Playlist> {
        val withoutFavorites = getAll()
        return withoutFavorites.map { getByIdWithFavorites(it.playlistId)!! }
    }

    /** 삭제 */
    suspend fun delete(playlistId: String): Boolean {
        val entity = dao.getById(playlistId)
        if (entity != null) {
            dao.deletePlaylist(entity)
            // 삭제 후 다시 조회
            val exists = dao.getById(playlistId) != null
            return !exists // 존재하지 않으면 true
        }
        return false // 애초에 없었던 경우
    }

    // ---------- 조작(모델 기반) ----------

    /** 중복은 무시하고 추가 */
    suspend fun addTracksIgnoreDuplicates(playlistId: String, toAdd: List<String>): Playlist? {
        val model = dao.getById(playlistId)?.toModel() ?: return null
        model.addTracksIgnoreDuplicates(toAdd)
        model.totalDurationSec = calculateUpdatedDuration(playlistId, toAdd.toSet(), TYPE_ADD_TRACKS)
        dao.updatePlaylist(model.toEntity())
        return model
    }

    suspend fun calculateUpdatedDuration(
        playlistId: String,
        changeSet: Set<String>,
        type: Int
    ): Int {
        val alreadyExistIds = dao.getById(playlistId)?.toModel()?.trackIds.orEmpty()
        val combinedIds = alreadyExistIds.toMutableSet()

        when (type) {
            TYPE_ADD_TRACKS -> combinedIds.addAll(changeSet)
            TYPE_DELETE_TRACKS -> combinedIds.removeAll(changeSet)
        }

        if (combinedIds.isEmpty()) return 0
        val combined = favoriteSongRepository.getFavoritesByIds(combinedIds.toList())
        return combined.sumOf { it.duration } / 1000
    }

    /** 최근 재생한 음악 */
    suspend fun addTracksMoveDuplicatesToBack(playlistId: String, toAdd: List<String>): Playlist? {
        val model = dao.getById(playlistId)?.toModelAlwaysInOrder() ?: return null
        model.addTracksMoveDuplicatesToBack(toAdd, maxQueueSize)
        model.totalDurationSec = calculateUpdatedDuration(playlistId, toAdd.toSet(), TYPE_ADD_TRACKS)
        dao.updatePlaylist(model.toEntity())
        return model.toEntity().toModel()
    }

    /** 여러 개 삭제 */
    suspend fun removeTracks(playlistId: String, toRemove: Set<String>): Playlist? {
        val model = dao.getById(playlistId)?.toModel() ?: return null
        model.removeTracks(toRemove)
        model.totalDurationSec = calculateUpdatedDuration(playlistId, toRemove, TYPE_DELETE_TRACKS)
        dao.updatePlaylist(model.toEntity())
        return model
    }

    suspend fun updatePlaylist(playlist: Playlist): Boolean {
        if (playlist.playlistId == PLAYLIST_ID_RECENTLY_PLAYED) {
            playlist.lastPlayedTimeMs = 0
            playlist.playCount = 0
        }
        val entity = playlist.toEntity()
        val rowsUpdated = dao.updatePlaylist(entity)
        return rowsUpdated > 0
    }



    suspend fun getAllPlaylistNameSet(): Set<String> {
        val models = getAll()
        return models.map { it.playlistName }.toSet()
    }

    // ---------- 매핑 ----------
    private fun Playlist.toEntity(): PlaylistEntity =
        PlaylistEntity(
            playlistId = playlistId,
            playlistName = playlistName,
            trackIds = trackIds.toList(),          // 가변 → 불변
            totalDurationSec = totalDurationSec,   // 초 단위로 일치
            createdDate = createdDate,
            lastPlayedTimeMs = lastPlayedTimeMs,
            playCount = playCount
        )

    private fun PlaylistEntity.toModel(): Playlist {
        val playlist = Playlist(
            playlistId = playlistId,
            playlistName = playlistName,
            trackIds = trackIds?.toMutableList() ?: mutableListOf(),   // 불변 → 가변
            totalDurationSec = totalDurationSec,
            createdDate = createdDate,
            lastPlayedTimeMs = lastPlayedTimeMs,
            playCount = playCount
        )
        if (playlist.playlistId == PLAYLIST_ID_RECENTLY_PLAYED){
            return playlist.deepCopy().copyWithTrackIds(playlist.trackIds.reversed())
        }
        return playlist
    }

    private fun PlaylistEntity.toModelAlwaysInOrder(): Playlist {
        val playlist = Playlist(
            playlistId = playlistId,
            playlistName = playlistName,
            trackIds = trackIds?.toMutableList() ?: mutableListOf(),   // 불변 → 가변
            totalDurationSec = totalDurationSec,
            createdDate = createdDate,
            lastPlayedTimeMs = lastPlayedTimeMs,
            playCount = playCount
        )
        return playlist
    }


    companion object {
        const val PLAYLIST_ID_RECENTLY_PLAYED = "sys_recently_played"
        const val TYPE_ADD_TRACKS = 0
        const val TYPE_DELETE_TRACKS = 1
    }
}

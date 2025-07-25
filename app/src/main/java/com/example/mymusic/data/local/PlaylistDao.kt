package com.example.mymusic.data.local

import androidx.room.*
import com.example.mymusic.data.local.PlaylistEntity

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(entity: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(entity: PlaylistEntity): Int

    @Delete
    suspend fun deletePlaylist(entity: PlaylistEntity)

    @Query("SELECT * FROM playlist_table WHERE playlistId = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlist_table")
    suspend fun getAll(): List<PlaylistEntity>
}

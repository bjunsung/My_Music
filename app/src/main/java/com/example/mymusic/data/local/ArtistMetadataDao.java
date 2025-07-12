package com.example.mymusic.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ArtistMetadataDao {

    @Query("SELECT * FROM artist_metadata_table WHERE vibeArtistId = :vibeArtistId LIMIT 1")
    ArtistMetadata getArtistMetadata(String vibeArtistId);

    @Query("DELETE FROM artist_metadata_table WHERE vibeArtistId = :vibeArtistId")
    int removeArtistMetadata(String vibeArtistId);

    @Query("SELECT * FROM artist_metadata_table WHERE spotifyArtistId = :spotifyId LIMIT 1")
    ArtistMetadata getArtistMetadataBySpotifyId(String spotifyId);

    @Query("DELETE FROM artist_metadata_table WHERE spotifyArtistId = :spotifyId")
    int removeArtistMetadataBySpotifyId(String spotifyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long saveArtistMetadata(ArtistMetadata metadata);

    @Update
    int updateArtistMetadata(ArtistMetadata metadata);
}

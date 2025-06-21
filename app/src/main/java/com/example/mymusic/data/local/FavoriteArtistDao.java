package com.example.mymusic.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteArtistDao {
    //search song by trackId
    @Query("SELECT * FROM favorite_artist_table WHERE artistId = :artistId")
    FavoriteArtist getFavoriteArtist(String artistId);

    //delete song by trackId
    @Query("DELETE FROM favorite_artist_table WHERE artistId = :artistId")
    void deleteFavoriteArtist(String artistId);

    //save favorite song
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void saveFavoriteArtist(FavoriteArtist favoriteArtist);

    //get favorites artist count
    @Query("SELECT COUNT(*) FROM favorite_artist_table")
    int getFavoriteArtistCount();

    //get all favorite Artist
    @Query("SELECT * FROM favorite_artist_table")
    List<FavoriteArtist> getAllFavoriteArtists();

}

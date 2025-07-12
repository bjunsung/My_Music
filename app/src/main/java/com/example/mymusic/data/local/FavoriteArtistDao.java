package com.example.mymusic.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FavoriteArtistDao {
    //search song by trackId
    @Query("SELECT * FROM favorite_artist_table WHERE artistId = :artistId")
    FavoriteArtist getFavoriteArtist(String artistId);

    //delete song by trackId
    @Query("DELETE FROM favorite_artist_table WHERE artistId = :artistId")
    int deleteFavoriteArtist(String artistId);

    @Query("DELETE FROM favorite_artist_table WHERE artistId IN (:artistId)")
    int deleteFavoriteArtistsByIds(List<String> artistId);

    //save favorite song
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long saveFavoriteArtist(FavoriteArtist favoriteArtist);

    //get favorites artist count
    @Query("SELECT COUNT(*) FROM favorite_artist_table")
    int getFavoriteArtistCount();

    //get all favorite Artist
    @Query("SELECT * FROM favorite_artist_table")
    List<FavoriteArtist> getAllFavoriteArtists();

    @Update
    int updateFavoriteArtist(FavoriteArtist favoriteArtist);

}

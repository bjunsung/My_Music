package com.example.mymusic.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FavoritesDao {
    //search song by trackId
    @Query("SELECT * FROM favorites_table WHERE trackId = :trackId")
    Favorites getFavoritesSong(String trackId);

    //delete song by trackId
    @Query("DELETE FROM favorites_table WHERE trackId = :trackId")
    void deleteFavoritesSong(String trackId);

    @Query("DELETE FROM favorites_table WHERE trackId IN (:trackIds)")
    int deleteFavoritesByIds(List<String> trackIds);

    //save favorite song
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveFavoritesSong(Favorites song);

    //get favorites songs count
    @Query("SELECT COUNT(*) FROM favorites_table")
    int getFavoritesCount();

    //get favorites songs
    @Query("SELECT * FROM favorites_table")
    List<Favorites> getAllFavorites();

    //get favorites songs by selected artists
    @Query("SELECT * FROM favorites_table WHERE artistId IN (:artistIds)")
    List<Favorites> getFavoritesByArtistIds(List<String> artistIds);

    @Update
    int updateFavoriteSong(Favorites song);  // update

}

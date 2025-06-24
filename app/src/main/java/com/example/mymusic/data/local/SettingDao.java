package com.example.mymusic.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SettingDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Settings settings);

    @Query("SELECT maxSearchedTracks FROM settings_table WHERE id = 0 LIMIT 1")
    Integer getMaxSearchedTracks();

    @Query("UPDATE settings_table SET maxSearchedTracks = :value WHERE id = 0")
    int updateMaxSearchedTracks(int value);

    @Query("SELECT maxSearchedArtists FROM settings_table WHERE id = 0 LIMIT 1")
    Integer getMaxSearchedArtists();

    @Query("UPDATE settings_table SET maxSearchedArtists = :value WHERE id = 0")
    int updateMaxSearchedArtists(int value);

    @Query("SELECT maxSearchedAlbumsByArtist FROM settings_table WHERE id = 0 LIMIT 1")
    Integer getMaxSearchedAlbumsByArtist();

    @Query("UPDATE settings_table SET maxSearchedAlbumsByArtist = :value WHERE id = 0")
    int updateMaxSearchedAlbumsByArtist(int value);

    @Query("UPDATE settings_table SET trackIdInputPrefersNumeric = :value WHERE id = 0")
    int updateNumericPreference(boolean value);

    @Query("SELECT trackIdInputPrefersNumeric FROM settings_table WHERE id = 0 LIMIT 1")
    boolean getNumeriPadPreference();


}

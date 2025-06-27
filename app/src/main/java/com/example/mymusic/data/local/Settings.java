package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings_table")
public class Settings {
    @NonNull
    @PrimaryKey
    @ColumnInfo(defaultValue = "0")
    public int id;

    public Integer maxSearchedTracks;
    public Integer maxSearchedArtists;
    public Integer maxSearchedAlbumsByArtist;
    public boolean personalized;

    @ColumnInfo(defaultValue = "0")
    public boolean trackIdInputPrefersNumeric;

    public Settings(){}
    public Settings(int id, int maxSearchedTracks, int maxSearchedArtists, int maxSearchedAlbumsByArtist){
        this.id = id;
        this.maxSearchedTracks = maxSearchedTracks;
        this.maxSearchedArtists = maxSearchedArtists;
        this.maxSearchedAlbumsByArtist = maxSearchedAlbumsByArtist;
        this.trackIdInputPrefersNumeric = true;
    }

    public int getId(){
        return this.id;
    }

}

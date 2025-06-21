package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.mymusic.model.Artist;

import java.util.List;

@Entity(tableName = "favorite_artist_table")
public class FavoriteArtist {
    @NonNull
    @PrimaryKey
    public String artistId;
    public String artistName;
    public String artworkUrl;
    public List<String> genres;
    public int followers;
    public int popularity;
    public String addedDate;
    public FavoriteArtist(){};
    public FavoriteArtist(Artist artist, String addedDate){
        this.artistId = artist.artistId;
        this.artistName = artist.artistName;
        this.artworkUrl = artist.artworkUrl;
        this.genres = artist.genres;
        this.followers = artist.followers;
        this.popularity = artist.popularity;
        this.addedDate = addedDate;
    }
}

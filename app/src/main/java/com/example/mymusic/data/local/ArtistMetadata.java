package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "artist_metadata_table")
public class ArtistMetadata {
    @NonNull
    @PrimaryKey
    public String vibeArtistId;
    public String artistNameKr;
    public String spotifyArtistId;
    public String debutDate;
    public List<String> yearsOfActivity;
    public List<String> agency;
    public String biography;
    public List<String> images;
    public List<List<String>> members;
    public List<List<String>> activity;

    public ArtistMetadata(String vibeArtistId,
                          String artistNameKr,
                          String spotifyArtistId,
                          String debutDate,
                          List<String> yearsOfActivity,
                          List<String> agency,
                          String biography,
                          List<String> images,
                          List<List<String>> members,
                          List<List<String>> activity){
        this.vibeArtistId = vibeArtistId;
        this.artistNameKr = artistNameKr;
        this.spotifyArtistId = spotifyArtistId;
        this.debutDate = debutDate;
        this.yearsOfActivity = yearsOfActivity;
        this.agency = agency;
        this.biography = biography;
        this.images = images;
        this.members = members;
        this.activity = activity;


    }



}

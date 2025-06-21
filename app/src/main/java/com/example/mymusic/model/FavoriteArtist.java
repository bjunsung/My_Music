package com.example.mymusic.model;

public class FavoriteArtist {
    public Artist artist;
    public String addedDate;
    public FavoriteArtist(Artist artist, String addedDate){
        this.artist = artist;
        this.addedDate = addedDate;
    }
}

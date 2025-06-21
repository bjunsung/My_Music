package com.example.mymusic.model;

import java.util.ArrayList;
import java.util.List;

public class Artist {
    public String artistId;
    public String artistName;
    public String artworkUrl;
    public List<String> genres;
    public int followers;
    public int popularity;

    public Artist(String artistId, String artistName, String artworkUrl, List<String> genres, int followers, int popularity) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.artworkUrl = artworkUrl;
        this.genres = genres;
        this.followers = followers;
        this.popularity = popularity;
    }
}

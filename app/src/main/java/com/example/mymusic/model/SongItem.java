package com.example.mymusic.model;

import java.io.Serializable;

public class SongItem implements Serializable {
    public String trackName;
    public String artistName;
    public String artworkUrl;
    public String albumName;
    public String releaseDate;
    public String previewUrl;

    public SongItem(String trackName, String artistName, String artworkUrl,
                    String albumName, String releaseDate, String previewUrl) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.artworkUrl = artworkUrl;
        this.albumName = albumName;
        this.releaseDate = releaseDate;
        this.previewUrl = previewUrl;
    }
}

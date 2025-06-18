package com.example.mymusic.model;

import java.util.ArrayList;
import java.util.List;

public class Album {
    public String albumId;
    public String albumName;
    public String releaseDate;
    public String artworkUrl;
    public int totalTracks;
    public List<Track> tracks;
    public Album(String albumId, String albumName, String releaseDate, String artworkUrl, int totalTracks){
        this.albumId = albumId;
        this.albumName = albumName;
        this.releaseDate = releaseDate;
        this.artworkUrl = artworkUrl;
        this.totalTracks = totalTracks;
        tracks = new ArrayList<>();
    }
    public void addTrack(Track track){
        tracks.add(track);
    }
}

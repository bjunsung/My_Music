package com.example.mymusic.model;

public class Favorite {
    public Track track;
    public String addedDate;
    public Favorite(Track track, String addedDate){
        this.track = track;
        this.addedDate = addedDate;
    }
}

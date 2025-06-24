package com.example.mymusic.model;

import java.util.List;

public class Favorite {
    public Track track;
    public String addedDate;
    public TrackMetadata metadata;
    public Favorite(Track track, String addedDate){
        this.track = track;
        this.addedDate = addedDate;
    }
    public Favorite(Track track, String addedDate, TrackMetadata metadata){
        this.track = track;
        this.addedDate = addedDate;
        this.metadata = metadata;
    }
}

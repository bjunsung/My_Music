package com.example.mymusic.ui.search;

import androidx.lifecycle.ViewModel;

import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    boolean initialKeyboardUpDone = false;
    public final List<Track> searchTrackResults = new ArrayList<>();
    public final List<Artist> searchArtistResults = new ArrayList<>();
    public int selectedOption = 0; // 기본값: search Track
    private int trackPosition = -1;
    private int artistPosition = -1;
    public void setTrackPosition(int trackPosition){
        this.trackPosition = trackPosition;
    }

    public int getTrackPosition(){
        return this.trackPosition;
    }

    public void setArtistPosition(int artistPosition){
        this.artistPosition = artistPosition;
    }

    public int getArtistPosition(){
        return this.artistPosition;
    }

}

package com.example.mymusic.ui.artistInfo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class ArtistInfoViewModel extends AndroidViewModel {
    private String initialTransitionName = null;
    private String currentTransitionName = null;
    private int trackPosition = -1;
    private int albumPosition = -1;
    private String trackTransitionName = null;
    public ArtistInfoViewModel(@NonNull Application application) {
        super(application);
    }
    public String getInitialTransitionName() {
        return initialTransitionName;
    }

    public String getCurrentTransitionName(){
        return currentTransitionName;
    }

    public void setInitialTransitionName(String initialTransitionName){
        this.initialTransitionName = initialTransitionName;
    }

    public void setCurrentTransitionName(String currentTransitionName){
        this.currentTransitionName = currentTransitionName;
    }

    public int getTrackPosition(){
        return trackPosition;
    }

    public void setTrackPosition(int position){
        this.trackPosition = position;
    }

    public int getAlbumPosition(){
        return albumPosition;
    }

    public void setAlbumPosition(int position) {
        this.albumPosition = position;
    }

    public void setTrackTransitionName(String transitionName){
        this.trackTransitionName = transitionName;
    }

    public String getTrackTransitionName(){
        return this.trackTransitionName;
    }

}

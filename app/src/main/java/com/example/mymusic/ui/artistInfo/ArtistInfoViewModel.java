package com.example.mymusic.ui.artistInfo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class ArtistInfoViewModel extends AndroidViewModel {
    private String initialTransitionName = null;
    private String initialTransitionNameForm = null;
    private int initialPosition = -1;
    private String currentTransitionName = null;
    private int trackPosition = -1;
    private int albumPosition = -1;
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


    public String getInitialTransitionNameForm() {
        return initialTransitionNameForm;
    }

    public void setInitialTransitionNameForm(String initialTransitionNameForm) {
        this.initialTransitionNameForm = initialTransitionNameForm;
    }

    public int getInitialPosition() {
        return initialPosition;
    }

    public void setInitialPosition(int initialPosition) {
        this.initialPosition = initialPosition;
    }
}

package com.example.mymusic.ui.artistInfo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.recyclerview.widget.RecyclerView;

public class ArtistInfoViewModel extends AndroidViewModel {
    private String initialTransitionName = null;
    private String initialTransitionNameForm = null;
    private int initialPosition = -1;
    private String currentTransitionName = null;
    private int trackPosition = -1;
    private int albumPosition = -1;
    private boolean firstFragmentCreation = true;
    private boolean secondPostponeFlag = false; //return from detail fragment
    private boolean metadataExist = false;
    private int lastPositionAtImageDetailFragment = -1;



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

    public boolean isFirstFragmentCreation() {
        return firstFragmentCreation;
    }

    public void setFirstFragmentCreation(boolean firstFragmentCreation) {
        this.firstFragmentCreation = firstFragmentCreation;
    }

    public boolean isSecondPostponeFlag() {
        return secondPostponeFlag;
    }

    public void setSecondPostponeFlag(boolean secondPostponeFlag) {
        this.secondPostponeFlag = secondPostponeFlag;
    }

    public boolean isMetadataExist() {
        return metadataExist;
    }

    public void setMetadataExist(boolean metadataExist) {
        this.metadataExist = metadataExist;
    }

    public int getLastPositionAtImageDetailFragment() {
        return lastPositionAtImageDetailFragment;
    }

    public void setLastPositionAtImageDetailFragment(int lastPositionAtImageDetailFragment) {
        this.lastPositionAtImageDetailFragment = lastPositionAtImageDetailFragment;
    }

}

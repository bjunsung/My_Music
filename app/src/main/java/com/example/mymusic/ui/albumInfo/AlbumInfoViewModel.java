package com.example.mymusic.ui.albumInfo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class AlbumInfoViewModel extends AndroidViewModel {
    private String initialTransitionName = null;
    private String currentTransitionName = null;
    public AlbumInfoViewModel(@NonNull Application application) {
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

}

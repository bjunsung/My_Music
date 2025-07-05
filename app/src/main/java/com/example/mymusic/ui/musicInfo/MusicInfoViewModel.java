package com.example.mymusic.ui.musicInfo;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class MusicInfoViewModel extends AndroidViewModel {

    private String initialTransitionName = null;
    private String currentTransitionName = null;

    public MusicInfoViewModel(@NonNull Application application) {
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

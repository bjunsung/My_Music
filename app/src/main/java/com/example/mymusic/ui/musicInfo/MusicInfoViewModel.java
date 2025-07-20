package com.example.mymusic.ui.musicInfo;

import android.app.Application;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MusicInfoViewModel extends AndroidViewModel {
    private static final String TAG = "MusicInfoViewModel";

    private String initialTransitionName = null;
    private String currentTransitionName = null;
    private boolean onSimpleDialog = false;
    private int simpleArtistDialogPosition = 0;
    private int detailVisibleStateOnDialog = 0;
    private GradientDrawable lastGradient = null;
    private List<FavoriteArtist> favoriteArtistList = new ArrayList<>();
    private LinkedHashMap<String, ArtistMetadata> metadataMap = new LinkedHashMap<>();

    public MusicInfoViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "MusicInfoViewModel instance created");
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

    public boolean isOnSimpleDialog() {
        return onSimpleDialog;
    }

    public void setOnSimpleDialog(boolean onSimpleDialog) {
        this.onSimpleDialog = onSimpleDialog;
    }

    public int getSimpleArtistDialogPosition() {
        return simpleArtistDialogPosition;
    }

    public void setSimpleArtistDialogPosition(int simpleArtistDialogPosition) {
        this.simpleArtistDialogPosition = simpleArtistDialogPosition;
    }

    public int getDetailVisibleStateOnDialog() {
        return detailVisibleStateOnDialog;
    }

    public void setDetailVisibleStateOnDialog(int detailVisibleStateOnDialog) {
        Log.d(TAG, "detail visible state setter, set to " + detailVisibleStateOnDialog);
        this.detailVisibleStateOnDialog = detailVisibleStateOnDialog;
    }

    public GradientDrawable getLastGradient() {
        return lastGradient;
    }

    public void setLastGradient(GradientDrawable lastGradient) {
        this.lastGradient = lastGradient;
    }

    public List<FavoriteArtist> getFavoriteArtistList() {
        return favoriteArtistList;
    }

    public void setFavoriteArtistList(List<FavoriteArtist> favoriteArtistList) {
        this.favoriteArtistList = favoriteArtistList;
    }

    public LinkedHashMap<String, ArtistMetadata> getMetadataMap() {
        return metadataMap;
    }

    public void setMetadataMap(LinkedHashMap<String, ArtistMetadata> metadataMap) {
        this.metadataMap = metadataMap;
    }
}

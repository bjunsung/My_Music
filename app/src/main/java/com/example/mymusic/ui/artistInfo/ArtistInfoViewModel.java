package com.example.mymusic.ui.artistInfo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.drawable.GradientDrawable;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ArtistInfoViewModel extends AndroidViewModel {
    private String initialTransitionName = null;
    private String initialTransitionNameForm = null;
    private int initialPosition = -1;
    private String currentTransitionName = null;

    private int trackPosition = -1;
    private int albumPosition = -1;

    private int reenterAlbumScrollPosition = 0;
    private int reenterAlbumScrollOffset = 0;

    private boolean firstFragmentCreation = true;
    private boolean metadataExist = false;


    private int scrollY = 0;


    private boolean secondPostponeFlag = false; //return from detail fragment
    private int startPositionAtImageDetailFragment = -1;
    private int lastPositionAtImageDetailFragment = -1;


    private boolean onSimpleDialog = false;
    private int simpleArtistDialogPosition = 0;
    private int detailVisibleStateOnDialog = 0;
    private GradientDrawable lastGradient = null;
    private List<FavoriteArtist> favoriteArtistList = new ArrayList<>();
    private LinkedHashMap<String, ArtistMetadata> metadataMap = new LinkedHashMap<>();

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

    public int getStartPositionAtImageDetailFragment() {
        return startPositionAtImageDetailFragment;
    }

    public void setStartPositionAtImageDetailFragment(int startPositionAtImageDetailFragment) {
        this.startPositionAtImageDetailFragment = startPositionAtImageDetailFragment;
    }

    public int getReenterAlbumScrollPosition() {
        return reenterAlbumScrollPosition;
    }

    public void setReenterAlbumScrollPosition(int reenterAlbumScrollPosition) {
        this.reenterAlbumScrollPosition = reenterAlbumScrollPosition;
    }

    public int getReenterAlbumScrollOffset() {
        return reenterAlbumScrollOffset;
    }

    public void setReenterAlbumScrollOffset(int reenterAlbumScrollOffset) {
        this.reenterAlbumScrollOffset = reenterAlbumScrollOffset;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
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

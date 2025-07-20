package com.example.mymusic.model;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

public class FavoriteArtistDiffCallback extends DiffUtil.Callback {
    private final String TAG = "FavoriteArtistDiffCallback";
    private final List<FavoriteArtist> oldList;
    private final List<FavoriteArtist> newList;
    private final int oldSortOpt;
    private final int newSortOpt;


    public FavoriteArtistDiffCallback(List<FavoriteArtist> oldList, List<FavoriteArtist> newList, int oldSortOpt, int newSortOpt){
        this.oldList = oldList;
        this.newList = newList;
        this.oldSortOpt = oldSortOpt;
        this.newSortOpt = newSortOpt;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition){
        return oldList.get(oldItemPosition).artist.artistId.equals(
                newList.get(newItemPosition).artist.artistId);

    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition){
        FavoriteArtist oldItem =  oldList.get(oldItemPosition);
        FavoriteArtist newItem = newList.get(newItemPosition);

        return Objects.equals(oldItem.artist, newItem.artist)
                && Objects.equals(oldItem.addedDate, newItem.addedDate)
                && Objects.equals(oldItem.metadata, newItem.metadata)
                && oldSortOpt == newSortOpt
                && Objects.equals(oldItem.isSelected, newItem.isSelected);
    }



}

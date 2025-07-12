package com.example.mymusic.model;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

public class FavoriteArtistDiffCallback extends DiffUtil.Callback {
    private final List<FavoriteArtist> oldList;
    private final List<FavoriteArtist> newList;

    public FavoriteArtistDiffCallback(List<FavoriteArtist> oldList, List<FavoriteArtist> newList){
        this.oldList = oldList;
        this.newList = newList;
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
                && Objects.equals(oldItem.metadata, newItem.metadata);
    }

}

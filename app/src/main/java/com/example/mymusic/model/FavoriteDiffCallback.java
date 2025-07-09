package com.example.mymusic.model;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

public class FavoriteDiffCallback extends DiffUtil.Callback {
    private final List<Favorite> oldList;
    private final List<Favorite> newList;

    public FavoriteDiffCallback(List<Favorite> oldList, List<Favorite> newList) {
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
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // 예: 같은 트랙 ID면 같은 항목으로 간주
        return oldList.get(oldItemPosition).track.trackId.equals(
                newList.get(newItemPosition).track.trackId);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Favorite oldItem = oldList.get(oldItemPosition);
        Favorite newItem = newList.get(newItemPosition);

        // 깊은 비교: metadata, 가사, 날짜 등이 바뀌었는지 확인
        return Objects.equals(oldItem.metadata, newItem.metadata)
                && Objects.equals(oldItem.track, newItem.track)
                && oldItem.isSelected == newItem.isSelected
                && oldItem.addedDate.equals(newItem.addedDate);
    }
}

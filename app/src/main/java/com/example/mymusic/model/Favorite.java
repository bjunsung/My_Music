package com.example.mymusic.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Favorite implements Parcelable {
    public Track track;
    public String addedDate;
    public TrackMetadata metadata;


    //for adapter
    public Boolean isSelected = false;
    public String keyword = null;
    public String audioUri;
    public int playCount = 0;
    public List<List<String>> playCountByDay = new ArrayList<>();

    public int recyclerViewPosition = -1;
    public Favorite(Track track){
        this.track = track;
    }
    public Favorite(Track track, String addedDate, TrackMetadata metadata){
        this.track = track;
        this.addedDate = addedDate;
        this.metadata = metadata;
    }

    protected Favorite(Parcel in) {
        track = in.readParcelable(Track.class.getClassLoader());
        addedDate = in.readString();
    }

    public void addPlayCount(String date) {
        playCount++;

        int index = -1;
        int countBeforeAdd = 0;

        for (int i = 0; i < playCountByDay.size(); ++i) {
            List<String> pair = playCountByDay.get(i);
            if (pair.get(0).equals(date)) {
                countBeforeAdd = Integer.parseInt(pair.get(1));
                index = i;
                break;
            }
        }

        int newCount = countBeforeAdd + 1;
        List<String> pair = new ArrayList<>();
        pair.add(date);
        pair.add(String.valueOf(newCount));

        if (index == -1) { // 새로운 날짜면 추가
            playCountByDay.add(pair);
        } else {           // 기존 날짜면 업데이트
            playCountByDay.set(index, pair);
        }
    }


    public static final Creator<Favorite> CREATOR = new Creator<Favorite>() {
        @Override
        public Favorite createFromParcel(Parcel in) {
            return new Favorite(in);
        }

        @Override
        public Favorite[] newArray(int size) {
            return new Favorite[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(track, flags);
        dest.writeString(addedDate);
    }

    public String getTitle() {
        if (metadata != null && metadata.title != null && !metadata.title.isEmpty()){
            return metadata.title;
        }else {
            return track != null ? track.trackName : "";
        }
    }

    public String getAddedDate(){
        return addedDate;
    }

    public String getReleaseDate() {
        return track != null ? track.releaseDate : "9999-99-99";
    }
    public Integer getDuration(){
        return track != null ? Integer.parseInt(track.durationMs) : 0;
    }
    public String getArtistName() {
        return track != null ? track.artistName : "";
    }
    public String getDurationStr() {
        if (track == null || track.durationMs == null) return "0초";
        try {
            int durationMs = Integer.parseInt(track.durationMs);
            int totalSeconds = durationMs / 1000;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            return minutes + "분 " + seconds + "초";
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "0초";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Favorite)) return false;

        Favorite other = (Favorite) obj;

        return Objects.equals(track, other.track)
                && Objects.equals(addedDate, other.addedDate)
                && Objects.equals(metadata, other.metadata)
                && Objects.equals(isSelected, other.isSelected);
    }



}

package com.example.mymusic.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Map<LocalDate, Integer> playCountByDay = new HashMap<>();

    public int recyclerViewPosition = -1;

    public Integer backgroundColor = null;
    public Integer playingPosition = 0;
    public LocalDate firstCountedDate = null;

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

    public void addPlayCount(LocalDate date) {
        playCount++;

        // Map이 비어있으면 초기화
        if (playCountByDay == null) {
            playCountByDay = new HashMap<>();
        }

        // 처음 추가되는 경우에만 firstCountedDate 설정 (put 전에)
        if (playCountByDay.isEmpty()) {
            firstCountedDate = date;
        }

        // 현재 날짜 카운트 (null-safe)
        Integer currentCountObj = playCountByDay.get(date);
        int currentCount = (currentCountObj != null) ? currentCountObj : 0;

        // +1 후 저장
        playCountByDay.put(date, currentCount + 1);
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

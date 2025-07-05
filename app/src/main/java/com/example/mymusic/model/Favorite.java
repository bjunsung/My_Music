package com.example.mymusic.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;


public class Favorite implements Parcelable {
    public Track track;
    public String addedDate;
    public TrackMetadata metadata;


    public Boolean isSelected = false;

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
}

package com.example.mymusic.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class FavoriteArtist implements Parcelable {
    public Artist artist;
    public String addedDate;
    public ArtistMetadata metadata;
    public FavoriteArtist(Artist artist){
        this.artist = artist;
    }
    public FavoriteArtist(Artist artist, String addedDate){
        this.artist = artist;
        this.addedDate = addedDate;
    }

    public FavoriteArtist(Artist artist, String addedDate, ArtistMetadata metadata){
        this.artist = artist;
        this.addedDate = addedDate;
        this.metadata = metadata;
    }

    protected FavoriteArtist(Parcel in) {
        artist = in.readParcelable(Artist.class.getClassLoader());
        addedDate = in.readString();
    }

    public static final Creator<FavoriteArtist> CREATOR = new Creator<FavoriteArtist>() {
        @Override
        public FavoriteArtist createFromParcel(Parcel in) {
            return new FavoriteArtist(in);
        }

        @Override
        public FavoriteArtist[] newArray(int size) {
            return new FavoriteArtist[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(artist, flags);
        dest.writeString(addedDate);
    }
}

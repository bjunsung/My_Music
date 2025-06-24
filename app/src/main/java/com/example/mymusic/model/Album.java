package com.example.mymusic.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Album implements Parcelable {
    public String artistId;
    public String albumId;
    public String artistName;
    public String albumName;
    public String releaseDate;
    public String artworkUrl;
    public int totalTracks;
    public List<Track> tracks;
    public Album(String artistId, String albumId, String artistName, String albumName, String releaseDate, String artworkUrl, int totalTracks){
        this.artistId = artistId;
        this.albumId = albumId;
        this.artistName = artistName;
        this.albumName = albumName;
        this.releaseDate = releaseDate;
        this.artworkUrl = artworkUrl;
        this.totalTracks = totalTracks;
        tracks = new ArrayList<>();
    }

    protected Album(Parcel in) {
        artistId = in.readString();
        albumId = in.readString();
        artistName = in.readString();
        albumName = in.readString();
        releaseDate = in.readString();
        artworkUrl = in.readString();
        totalTracks = in.readInt();
        tracks = in.createTypedArrayList(Track.CREATOR);
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    public void addTrack(Track track){
        tracks.add(track);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(artistId);
        dest.writeString(albumId);
        dest.writeString(artistName);
        dest.writeString(albumName);
        dest.writeString(releaseDate);
        dest.writeString(artworkUrl);
        dest.writeInt(totalTracks);
        dest.writeTypedList(tracks);
    }
}

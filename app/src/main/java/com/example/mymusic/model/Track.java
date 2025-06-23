package com.example.mymusic.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable{
    public String trackId;
    public String albumId;
    public String artistId;
    public String trackName;
    public String albumName;
    public String artistName;
    public String artworkUrl;
    public String releaseDate;
    public String durationMs;

    public Track(String trackId, String albumId, String artistId,
                 String trackName, String albumName, String artistName,
                 String artworkUrl, String releaseDate, String durationMs) {
        this.artistId = artistId;
        this.albumId = albumId;
        this.trackId = trackId;
        this.trackName = trackName;
        this.artistName = artistName;
        this.artworkUrl = artworkUrl;
        this.albumName = albumName;
        this.releaseDate = releaseDate;
        this.durationMs = durationMs;
    }

    protected Track(Parcel in) {
        trackId = in.readString();
        albumId = in.readString();
        artistId = in.readString();
        trackName = in.readString();
        albumName = in.readString();
        artistName = in.readString();
        artworkUrl = in.readString();
        releaseDate = in.readString();
        durationMs = in.readString();
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trackId);
        dest.writeString(albumId);
        dest.writeString(artistId);
        dest.writeString(trackName);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeString(artworkUrl);
        dest.writeString(releaseDate);
        dest.writeString(durationMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}

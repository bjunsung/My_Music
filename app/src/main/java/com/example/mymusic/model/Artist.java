package com.example.mymusic.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Artist implements Parcelable {
    public String artistId;
    public String artistName;
    public String artworkUrl;
    public List<String> genres;
    public int followers;
    public int popularity;

    public String debutDate;

    public Artist(String artistId, String artistName, String artworkUrl, List<String> genres, int followers, int popularity) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.artworkUrl = artworkUrl;
        this.genres = genres;
        this.followers = followers;
        this.popularity = popularity;
    }

    protected Artist(Parcel in) {
        artistId = in.readString();
        artistName = in.readString();
        artworkUrl = in.readString();
        genres = in.createStringArrayList();
        followers = in.readInt();
        popularity = in.readInt();
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(artistId);
        dest.writeString(artistName);
        dest.writeString(artworkUrl);
        dest.writeStringList(genres);
        dest.writeInt(followers);
        dest.writeInt(popularity);
    }

    public String getJoinedGenres(){
        StringBuilder joinedGenres = new StringBuilder();
        joinedGenres.append("[");
        if (this.genres.isEmpty())
            joinedGenres.append("]");
        else
            for (int i=0; i<this.genres.size(); i++) {
                joinedGenres.append(this.genres.get(i));
                if (i < this.genres.size() - 1)
                    joinedGenres.append(", ");
                else
                    joinedGenres.append("]");
            }
        return joinedGenres.toString();
    }

}

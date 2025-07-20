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

    public String getArtistName(){
        if (artist != null && artist.artistName != null && !artist.artistName.isEmpty()){
            return artist.artistName;
        }
        else return null;
    }

    public int getFollowers(){
        if (artist != null &&  artist.followers > 0){
            return artist.followers;
        }
        else return -1;
    }

    public String getAddedDate(){
        if (addedDate != null && !addedDate.isEmpty())
            return addedDate;
        else
            return null;
    }

    public String getDebutDate() {
        if (metadata != null && metadata.debutDate != null && !metadata.debutDate.isEmpty()) {
            try {
                // 기존 형식: 2024.12.31
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy.MM.dd");
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");

                java.util.Date date = inputFormat.parse(metadata.debutDate);
                return outputFormat.format(date);

            } catch (java.text.ParseException e) {
                e.printStackTrace(); // 또는 로그 처리
                return metadata.debutDate; // 파싱 실패 시 원본 그대로 반환
            }
        } else {
            return null;
        }
    }


    public int getMemberCount(){
        if (metadata != null && metadata.members != null && !metadata.members.isEmpty())
            return metadata.members.size();
        else
            return 0;
    }

    public int getImageCount(){
        if (metadata != null && metadata.images != null && !metadata.images.isEmpty())
            if (metadata.images.contains(artist.artworkUrl)) {
                return metadata.images.size();
            } else{
                return metadata.images.size() + 1;
            }
        else
            return 1;
    }

    public String getRepresentativeImageUrl(){
        return artist.artworkUrl;
    }

    public String getSecondaryImageUrl(){
        String url = artist.artworkUrl;
        if (metadata != null && metadata.images != null && !metadata.images.isEmpty()){
            if (metadata.images.size() == 1){
                url = metadata.images.get(0);
                }
            else{
                url = (url.equals(metadata.images.get(0))) ? metadata.images.get(1): metadata.images.get(0);
            }
        }
        return url;
    }

}

package com.example.mymusic.model;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArtistMetadata implements Parcelable {
    public String vibeArtistId;
    public String spotifyArtistId;
    public String debutDate;
    public List<String> yearsOfActivity;
    public List<String> agency;
    public String biography;
    public List<String> images;
    public List<List<String>> members;
    public List<List<String>> activity;
    public String artistNameKr;

    public ArtistMetadata(){}
    public ArtistMetadata(String vibeArtistId, String artistNameKr, String spotifyArtistId, String debutDate, List<String> yearsOfActivity, List<String> agency, String biography, List<String> images, List<List<String>> members, List<List<String>> activity){
        this.spotifyArtistId = spotifyArtistId;
        this.vibeArtistId = vibeArtistId;
        this.debutDate = debutDate;
        this.yearsOfActivity = yearsOfActivity;
        this.agency = agency;
        this.biography = biography;
        this.images = images;
        this.members = members;
        this.activity = activity;
        this.artistNameKr = artistNameKr;
    }

    public ArtistMetadata(String vibeArtistId, String spotifyArtistId){
        this.spotifyArtistId = spotifyArtistId;
        this.vibeArtistId = vibeArtistId;
    }

    public ArtistMetadata(String vibeArtistId){
        this.vibeArtistId = vibeArtistId;
    }

    protected ArtistMetadata(Parcel in) {
        vibeArtistId = in.readString();
        spotifyArtistId = in.readString();
        debutDate = in.readString();
        yearsOfActivity = in.createStringArrayList();
        agency = in.createStringArrayList();
        biography = in.readString();
        images = in.createStringArrayList();
        artistNameKr = in.readString();
    }

    public static final Creator<ArtistMetadata> CREATOR = new Creator<ArtistMetadata>() {
        @Override
        public ArtistMetadata createFromParcel(Parcel in) {
            return new ArtistMetadata(in);
        }

        @Override
        public ArtistMetadata[] newArray(int size) {
            return new ArtistMetadata[size];
        }
    };

    public String membersToString(){
        if (members.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for(List<String> pair : members){
            result.append(pair.get(0));
            result.append(", ");
        }
        return result.substring(0, result.length()-2);
    }

    public String memberIdsToString(){
        if (members.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for(List<String> pair : members){
            result.append(pair.get(1));
            result.append(", ");
        }
        return result.substring(0, result.length()-2);
    }

    public String memberTumbnailToString(){
        if (members.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for(List<String> pair : members){
            result.append(pair.get(2));
            result.append(", ");
        }
        return result.substring(0, result.length()-2);
    }

    public String activityToString(){
        if (activity.isEmpty()) return null;
        List<String> name = new ArrayList<>();
        for(List<String> pair : activity){
            name.add(pair.get(0));
        }
        return String.join(", ", name);
    }

    public String activityIdsToString(){
        if (activity.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for(List<String> pair : activity){
            result.append(pair.get(1));
            result.append(", ");
        }
        return result.substring(0, result.length()-2);
    }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        if (artistNameKr != null && !artistNameKr.isEmpty())
            result.append("이름: " + artistNameKr);
        if (debutDate != null && !debutDate.isEmpty())
            result.append("데뷔일: " + debutDate);

        if (yearsOfActivity != null && !yearsOfActivity.isEmpty())
            result.append("\n연대: " + String.join(", ", yearsOfActivity));

        if (agency != null && !agency.isEmpty())
            result.append("\n소속사: " + String.join(", ", agency));

        if (members != null && !members.isEmpty())
            result.append("\n\nmembers: " + membersToString());

        if (activity != null && !activity.isEmpty())
            result.append("\n활동: " + activityToString());

        if (biography != null && !biography.isEmpty())
            result.append("\n\nbiography: " + biography);
        return result.toString();
    }


    public boolean isFetched(){
        return (debutDate != null && !debutDate.isEmpty()) ||
                (yearsOfActivity != null && !yearsOfActivity.isEmpty()) && yearsOfActivity.get(0) != null && !yearsOfActivity.get(0).isEmpty() ||
                (agency != null && !agency.isEmpty() && agency.get(0) != null && !agency.get(0).isEmpty()) ||
                (biography != null && !biography.isEmpty()) ||
                (images != null && !images.isEmpty() && images.get(0) != null && !images.get(0).isEmpty()) ||
                (members != null && !members.isEmpty() && members.get(0) != null && members.get(0).isEmpty() && members.get(0).get(0) != null &&  !members.get(0).get(0).isEmpty()) ||
                (activity != null && !activity.isEmpty() && activity.get(0) != null && !activity.get(0).isEmpty());
    }


    @Override
    public boolean equals(Object obj){
        if (obj == null) return false;
        if (this == obj) return true;
        if (this.getClass() != obj.getClass()) return false;
        ArtistMetadata other = (ArtistMetadata) obj;

        return Objects.equals(this.vibeArtistId, other.vibeArtistId)
                && Objects.equals(this.artistNameKr, other.artistNameKr)
                && Objects.equals(this.spotifyArtistId, other.spotifyArtistId)
                && Objects.equals(this.debutDate, other.debutDate)
                && Objects.equals(this.yearsOfActivity, other.yearsOfActivity)
                && Objects.equals(this.agency, other.agency)
                && Objects.equals(this.biography, other.biography)
                && Objects.equals(this.images, other.images)
                && Objects.equals(this.members, other.members)
                && Objects.equals(this.activity, other.activity);
    }

    @Override
    public int hashCode(){
        return Objects.hash(artistNameKr, vibeArtistId, spotifyArtistId, debutDate, yearsOfActivity, agency, biography, images, members, activity);
    }
    public void addMembers(List<String> list) {


    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(vibeArtistId);
        dest.writeString(spotifyArtistId);
        dest.writeString(debutDate);
        dest.writeStringList(yearsOfActivity);
        dest.writeStringList(agency);
        dest.writeString(biography);
        dest.writeStringList(images);
        dest.writeString(artistNameKr);
    }
}

package com.example.mymusic.model;

import java.util.List;

public class ArtistMetadata {
    public String spotifyArtistId;
    public String vibeArtistId;
    public String debutDate;
    public List<String> yearsOfActivity;
    public List<String> agency;
    public String biography;
    public List<String> images;
    public List<List<String>> members;
    public List<List<String>> activity;

    public ArtistMetadata(){}
    public ArtistMetadata(String spotifyArtistId, String vibeArtistId){
        this.spotifyArtistId = spotifyArtistId;
        this.vibeArtistId = vibeArtistId;
    }

    public ArtistMetadata(String vibeArtistId){
        this.vibeArtistId = vibeArtistId;
    }



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
        StringBuilder result = new StringBuilder();
        for(List<String> pair : activity){
            result.append(pair.get(0));
            result.append(", ");
        }
        return result.substring(0, result.length()-2);
    }

    public String activityToString2(){
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
        return "spotify id: " + spotifyArtistId +
                "\nvibe id:" + vibeArtistId +
                "\ndebut date:" + debutDate +
                "\n연대: " + String.join(", ", yearsOfActivity) +
                "\n소속사: " + String.join(", ", agency) +
                "\nbiography: " + biography +
                "\nimages: " + String.join(", ", images) +
                "\nmembers: " + membersToString() +
                "\nids: " + memberIdsToString() +
                "\n썸네일: " + memberTumbnailToString() +
                "\n활동: " + activityToString() +
                "\n활동2: " + activityToString2() ;
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

}

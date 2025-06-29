package com.example.mymusic.model;

import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TrackMetadata {
    public String vibeTrackId;
    public String title;
    public String artistLink;
    public String lyrics;
    public List<List<String>> vocalists;
    public List<String> lyricists;
    public List<String> composers;
    public TrackMetadata(){
        vocalists = new ArrayList<>();
    }


    public TrackMetadata(String vibeTrackId, String title, String lyrics, List<List<String>> vocalists, List<String> lyricists, List<String> composers) {
        this.vibeTrackId = vibeTrackId;
        this.title = title;
        this.lyrics = lyrics;
        this.lyricists = lyricists;
        this.composers = composers;
        this.vocalists = vocalists;
    }

    public TrackMetadata(String vibeTrackId, String title, String lyrics, List<String> lyricists, List<String> composers) {
        this.vibeTrackId = vibeTrackId;
        this.title = title;
        this.lyrics = lyrics;
        this.lyricists = lyricists;
        this.composers = composers;
    }


    public void addVocalists(List<String> vocalistsName){
        if (vocalists == null)
            vocalists = new ArrayList<>();
        if (vocalistsName != null) {
            for (String name : vocalistsName) {
                List<String> entry = new ArrayList<>();
                entry.add(name);
                entry.add(null); // 아직 링크 없음
                vocalists.add(entry);
            }
        }
    }

    public void setArtistLink(String artistLink){
        this.artistLink =  "https://vibe.naver.com" + artistLink;
    }


    // Getter
    public String getTitle() { return title; }
    public String getLyrics() { return lyrics; }
    public List<String> getLyricists() { return lyricists; }
    public List<String> getComposers() { return composers; }



    public void setVocalists(List<List<String>> vocalists) {
        this.vocalists = vocalists;
    }

    public String vocalistsToString(){
        if (vocalists.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for(List<String> vocalistSet : vocalists){
            result.append(vocalistSet.get(0));
            result.append(", ");
        }
        return result.substring(0, result.length()-2);
    }

    public StringBuilder vocalistIdsToString(){
        StringBuilder result = new StringBuilder();
        for(List<String> vocalistSet : vocalists){
            result.append(vocalistSet.get(1));
            result.append(" ");
        }
        return result;
    }


    // (Optional) toString()
    @Override
    public String toString() {
        return "artist link: " + artistLink + "" +
                "\n제목: " + title +
                "\n\n보컬: " + vocalistsToString()   +
               // "\n\nid : " +  vocalistIdsToString() +
                "\n\n작사: " + String.join(", ", lyricists) +
                "\n작곡: " + String.join(", ", composers) +
                "\n\n가사:\n" + lyrics;
    }


    public void update(List<List<String>> vocalists) {
        this.vocalists = vocalists;
    }

}
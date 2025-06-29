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
    public List<String> vocalists;  // [이름, 링크]
    public List<String> lyricists;
    public List<String> composers;
    public TrackMetadata(){}

    public TrackMetadata(String vibeTrackId, String title ,String lyrics, List<String> vocalists, List<String> lyricists, List<String> composers) {
        this.vibeTrackId = vibeTrackId;
        this.title = title;
        this.lyrics = lyrics;
        this.vocalists = vocalists;
        this.lyricists = lyricists;
        this.composers = composers;
    }

    public TrackMetadata(String vibeTrackId, String title, String artistLink, String lyrics, List<String> vocalists, List<String> lyricists, List<String> composers) {
        this.vibeTrackId = vibeTrackId;
        this.artistLink =  "https://vibe.naver.com" + artistLink;
        this.title = title;
        this.lyrics = lyrics;
        this.vocalists = vocalists;
        this.lyricists = lyricists;
        this.composers = composers;
    }


    // Getter
    public String getTitle() { return title; }
    public String getLyrics() { return lyrics; }
    public List<String> getLyricists() { return lyricists; }
    public List<String> getComposers() { return composers; }


    /*
    public void setVocalists(List<List<String>> vocalists) {
        this.vocalists = vocalists;
    }

    public StringBuilder vocalistsToString(){
        StringBuilder result = new StringBuilder();
        for(List<String> vocalistSet : vocalists){
            result.append(vocalistSet.get(0));
            result.append(" ");
        }
        return result;
    }

    public StringBuilder vocalistIdsToString(){
        StringBuilder result = new StringBuilder();
        for(List<String> vocalistSet : vocalists){
            result.append(vocalistSet.get(1));
            result.append(" ");
        }
        return result;
    }
*/

    // (Optional) toString()
    @Override
    public String toString() {
        return "artist link: " + artistLink + "" +
                "\n제목: " + title +
                "\n\n보컬: " +  String.join(", ", vocalists)   +
               // "\n\nid : " +  vocalistIdsToString() +
                "\n\n작사: " + String.join(", ", lyricists) +
                "\n작곡: " + String.join(", ", composers) +
                "\n\n가사:\n" + lyrics;
    }
}
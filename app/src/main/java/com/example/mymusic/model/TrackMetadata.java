package com.example.mymusic.model;

import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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



    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("\n제목: ").append(title);
        if (vocalists!=null && !vocalists.isEmpty())
            result.append("\n\n보컬: ").append(vocalistsToString());
        if (lyricists != null && !lyricists.isEmpty())
            result.append("\n\n작사: ").append(String.join(", ", lyricists));
        if (composers != null && !composers.isEmpty())
            result.append("\n작곡: ").append(String.join(", ", composers));
        result.append("\n\n가사:\n").append(lyrics);

        return String.valueOf(result);
    }


    public void update(List<List<String>> vocalists) {
        this.vocalists = vocalists;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TrackMetadata that = (TrackMetadata) obj;

        return Objects.equals(lyrics, that.lyrics)
                && Objects.equals(vibeTrackId, that.vibeTrackId)
                && Objects.equals(title, that.title)
                && Objects.equals(artistLink, that.artistLink)
                && Objects.equals(vocalists, that.vocalists)
                && Objects.equals(lyricists, that.lyricists)
                && Objects.equals(composers, that.composers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lyrics, vibeTrackId, title, artistLink,
                vocalists, lyricists, composers);
    }


}
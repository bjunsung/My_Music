package com.example.mymusic.model;

import java.util.List;

public class TrackMetadata {
    public String title;
    public String lyrics;
    public List<String> lyricists;
    public List<String> composers;
    public TrackMetadata(){}

    public TrackMetadata(String title, String lyrics, List<String> lyricists, List<String> composers) {
        this.title = title;
        this.lyrics = lyrics;
        this.lyricists = lyricists;
        this.composers = composers;
    }

    // Getter
    public String getTitle() { return title; }
    public String getLyrics() { return lyrics; }
    public List<String> getLyricists() { return lyricists; }
    public List<String> getComposers() { return composers; }

    // (Optional) toString()
    @Override
    public String toString() {
        return "곡명: " + title +
                "\n\n작사: " + String.join(", ", lyricists) +
                "\n작곡: " + String.join(", ", composers) +
                "\n\n가사:\n" + lyrics;
    }
}
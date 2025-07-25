package com.example.mymusic.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

import com.example.mymusic.model.Track;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@Entity(tableName = "favorites_table")
public class Favorites
{
    @NonNull
    @PrimaryKey
    public String trackId;
    public String albumId;
    public String artistId;
    public String trackName;
    public String albumName;
    public String artistName;
    public String artworkUrl;
    public String releaseDate;
    public String durationMs;
    public String addedDate;

    public String vibeTrackId;

    public String trackNameKr;
    public String lyrics;
    public List<List<String>> vocalists;
    public List<String> lyricists;
    public List<String> composers;
    public Integer primaryColor;
    public String audioUri;
    public Integer playCount;

    // 변경된 부분

    // **변경: Map<LocalDate, Integer> 로 교체**
    public Map<LocalDate, Integer> playCountByDay;
    public LocalDate firstCountedDate; // 최솟값 날짜 캐싱
    public LocalDate lastPlayedDate;  //마지막 플레이 날짜

    public Favorites(){}
    public Favorites(Track track,
                     String addedDate,
                     String vibeTrackId,
                     String trackNameKr,
                     String lyrics,
                     List<List<String>> vocalists,
                     List<String> lyricists,
                     List<String> composers,
                     String audioUri,
                     int playCount,
                     Map<LocalDate, Integer> playCountByDay,
                     LocalDate firstCountedDate,
                     LocalDate lastPlayedDate){
        this.trackId = track.trackId;
        this.albumId = track.albumId;
        this.artistId = track.artistId;
        this.trackName = track.trackName;
        this.albumName = track.albumName;
        this.artistName = track.artistName;
        this.artworkUrl = track.artworkUrl;
        this.releaseDate = track.releaseDate;
        this.durationMs = track.durationMs;
        this.addedDate = addedDate;
        this.vibeTrackId = vibeTrackId;
        this.trackNameKr = trackNameKr;
        this.lyrics = lyrics;
        this.vocalists = vocalists;
        this.lyricists = lyricists;
        this.composers = composers;
        this.primaryColor = track.primaryColor;
        this.audioUri = audioUri;
        this.playCount = playCount;
        this.playCountByDay = playCountByDay;
        this.firstCountedDate = firstCountedDate;
        this.lastPlayedDate = lastPlayedDate;
    }

    public Favorites(Track track, String addedDate){
        this.trackId = track.trackId;
        this.albumId = track.albumId;
        this.artistId = track.artistId;
        this.trackName = track.trackName;
        this.albumName = track.albumName;
        this.artistName = track.artistName;
        this.artworkUrl = track.artworkUrl;
        this.releaseDate = track.releaseDate;
        this.durationMs = track.durationMs;
        this.addedDate = addedDate;
        this.primaryColor = track.primaryColor;
    }


}

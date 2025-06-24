package com.example.mymusic.data.repository;

import android.content.Context;
import android.health.connect.datatypes.Metadata;
import android.util.Log;


import androidx.core.util.Consumer;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.Favorites;
import com.example.mymusic.data.local.FavoritesDao;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;

import java.util.ArrayList;
import java.util.List;


public class FavoriteSongRepository {
    private final FavoritesDao favoritesDao;
    public FavoriteSongRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        favoritesDao = db.favoritesDao();
    }
    public void saveFavoritesSong(Track track, String addedDate){
        Favorites song = new Favorites(track, addedDate, null, null, null, null, null);
        favoritesDao.saveFavoritesSong(song);
    }
    public String deleteFavoritesSong(String trackId){
        Favorites song = favoritesDao.getFavoritesSong(trackId);
        if (song == null) return null;
        String songName = song.trackName;
        favoritesDao.deleteFavoritesSong(trackId);
        return songName;
    }
    public Favorite getFavoritesSong(String trackId){
        Favorites song = favoritesDao.getFavoritesSong(trackId);
        if(song == null) {
            return null;
        }
        Track track = new Track(
                song.trackId,
                song.albumId,
                song.artistId,
                song.trackName,
                song.albumName,
                song.artistName,
                song.artworkUrl,
                song.releaseDate,
                song.durationMs);
        TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.lyricists, song.composers);
        return new Favorite(track, song.addedDate, metadata);
    }
    public int getFavoritesCount(){
        return favoritesDao.getFavoritesCount();
    }
    public List<Favorite> getAllFavoriteTracks() {
        List<Favorites> songs = favoritesDao.getAllFavorites();
        List<Favorite> favorites = new ArrayList<>();
        for (Favorites song : songs) {
            Track track = new Track(
                    song.trackId,
                    song.albumId,
                    song.artistId,
                    song.trackName,
                    song.albumName,
                    song.artistName,
                    song.artworkUrl,
                    song.releaseDate,
                    song.durationMs
            );
            TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.lyricists, song.composers);
            favorites.add(new Favorite(track, song.addedDate, metadata));
        }
        return favorites;
    }

    public List<Favorites> getFavoriteTracksByArtist(List<String> artistIds) {
        return favoritesDao.getFavoritesByArtistIds(artistIds);
    }

    public void updateFavoriteSong(String trackId, TrackMetadata metadata, Consumer<Integer> callback){
        Favorites favorites = favoritesDao.getFavoritesSong(trackId);
        favorites.vibeTrackId = metadata.vibeTrackId;
        favorites.trackNameKr = metadata.title;
        favorites.lyrics = metadata.lyrics;
        favorites.lyricists = metadata.lyricists;
        favorites.composers = metadata.composers;
        Integer result = favoritesDao.updateFavoriteSong(favorites);
        callback.accept(result);
    }

}

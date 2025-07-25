package com.example.mymusic.data.repository;

import android.content.Context;


import androidx.core.util.Consumer;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.Favorites;
import com.example.mymusic.data.local.FavoritesDao;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class FavoriteSongRepository {
    public interface FavoriteDbCallback{
        void onSuccess();
        void onFailure();
    }
    private final FavoritesDao favoritesDao;
    public FavoriteSongRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        favoritesDao = db.favoritesDao();
    }
    public void saveFavoritesSong(Track track, String addedDate){
        Favorites song = new Favorites(track, addedDate);
        song.audioUri = null;
        song.firstCountedDate = null;
        song.playCountByDay = new HashMap<>();
        song.playCount = 0;
        favoritesDao.saveFavoritesSong(song);
        song.lastPlayedDate = null;
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
        track.primaryColor = song.primaryColor;
        TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.vocalists, song.lyricists, song.composers);
        Favorite fav = new Favorite(track, song.addedDate, metadata);
        fav.audioUri = song.audioUri;
        fav.playCount = song.playCount;
        //fav.playCountByDay = song.playCountByDay;
        fav.firstCountedDate = song.firstCountedDate;
        fav.lastPlayedDate = song.lastPlayedDate;
        return fav;
    }

    public Favorite getFavoriteSongWithPlayCount(String trackId) {
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
        track.primaryColor = song.primaryColor;
        TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.vocalists, song.lyricists, song.composers);
        Favorite fav = new Favorite(track, song.addedDate, metadata);
        fav.audioUri = song.audioUri;
        fav.playCount = song.playCount;
        fav.playCountByDay = song.playCountByDay;
        fav.firstCountedDate = song.firstCountedDate;
        fav.lastPlayedDate = song.lastPlayedDate;
        return fav;
    }

    public List<Favorite> getAllFavoriteTracksWithPlayCount() {
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
            TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.vocalists, song.lyricists, song.composers);
            Favorite fav = new Favorite(track, song.addedDate, metadata);
            fav.audioUri = song.audioUri;
            fav.playCount = song.playCount;
            fav.playCountByDay = song.playCountByDay;
            fav.firstCountedDate = song.firstCountedDate;
            fav.lastPlayedDate = song.lastPlayedDate;
            favorites.add(fav);
        }
        return favorites;
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
            TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.vocalists, song.lyricists, song.composers);
            Favorite fav = new Favorite(track, song.addedDate, metadata);
            fav.audioUri = song.audioUri;
            fav.playCount = song.playCount;
            //fav.playCountByDay = song.playCountByDay;
            fav.firstCountedDate = song.firstCountedDate;
            favorites.add(fav);
        }
        return favorites;
    }

    public List<Favorites> getFavoriteTracksByArtist(List<String> artistIds) {
        return favoritesDao.getFavoritesByArtistIds(artistIds);
    }

    public void updateFavoriteSongMetadata(String trackId, TrackMetadata metadata, Consumer<Integer> callback){
        Favorites favorites = favoritesDao.getFavoritesSong(trackId);
        favorites.vibeTrackId = metadata.vibeTrackId;
        favorites.trackNameKr = metadata.title;
        favorites.lyrics = metadata.lyrics;
        favorites.vocalists = metadata.vocalists;
        favorites.lyricists = metadata.lyricists;
        favorites.composers = metadata.composers;
        Integer result = favoritesDao.updateFavoriteSong(favorites);
        callback.accept(result);
    }

    public void updateFavoriteSongExceptPlayCount(Favorite favorite, FavoriteDbCallback callback) {
        Favorites existing = favoritesDao.getFavoritesSong(favorite.track.trackId);
        TrackMetadata metadata = favorite.metadata;
        existing.addedDate = favorite.addedDate;
        existing.vibeTrackId = metadata.vibeTrackId;
        existing.trackNameKr = metadata.title;
        existing.lyrics = metadata.lyrics;
        existing.vocalists = metadata.vocalists;
        existing.lyricists = metadata.lyricists;
        existing.composers = metadata.composers;
        existing.audioUri = favorite.audioUri;


        int result = favoritesDao.updateFavoriteSong(existing);
        if (result > 0) callback.onSuccess();
        else callback.onFailure();
    }

    public void updateFavoriteSongWithPlayCount(Favorite favorite, FavoriteDbCallback callback) {
        TrackMetadata metadata = favorite.metadata;
        String audioUriStr = favorite.audioUri;
        Favorites converted = new Favorites(favorite.track,
                favorite.addedDate,
                metadata.vibeTrackId,
                metadata.title,
                metadata.lyrics,
                metadata.vocalists,
                metadata.lyricists,
                metadata.composers,
                audioUriStr,
                favorite.playCount,
                favorite.playCountByDay,
                favorite.firstCountedDate,
                favorite.lastPlayedDate);
        int result = favoritesDao.updateFavoriteSong(converted);
        if (result > 0) callback.onSuccess();
        else callback.onFailure();
    }


    public int deleteFavoritesByIds(List<String> trackIds){
        return favoritesDao.deleteFavoritesByIds(trackIds);
    }
}




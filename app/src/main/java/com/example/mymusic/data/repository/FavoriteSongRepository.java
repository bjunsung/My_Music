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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


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

    public List<Favorite> getFavoritesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        // 1) 쿼리 비용을 줄이기 위해 DB 조회는 '고유 ID'로만 (순서는 나중에 메모리에서 복원)
        //    중복을 보존하려면, 여기서 unique로 만드는 건 '조회용'에만 쓰고
        //    최종 반환은 원래 ids 순서/중복을 그대로 따릅니다.
        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>(ids);
        List<Favorites> rawDataList = favoritesDao.getFavoritesByIds(new ArrayList<>(uniqueIds));
        if (rawDataList == null || rawDataList.isEmpty()) return new ArrayList<>();

        // 2) id -> Favorite 매핑
        Map<String, Favorite> byId = new HashMap<>(rawDataList.size());
        for (Favorites item : rawDataList) {
            Favorite model = entityToModel(item);
            byId.put(item.trackId, model);
        }

        // 3) 원래 ids 순서대로 재배열 (중복 보존, 누락 ID는 자동 스킵)
        List<Favorite> ordered = new ArrayList<>(ids.size());
        for (String id : ids) {
            Favorite f = byId.get(id);
            if (f != null) ordered.add(f);
        }

        return ordered;
    }


    private Favorite entityToModel(Favorites rawData) {
        if(rawData == null) {
            return null;
        }
        Track track = new Track(
                rawData.trackId,
                rawData.albumId,
                rawData.artistId,
                rawData.trackName,
                rawData.albumName,
                rawData.artistName,
                rawData.artworkUrl,
                rawData.releaseDate,
                rawData.durationMs);
        track.primaryColor = rawData.primaryColor;
        TrackMetadata metadata = new TrackMetadata(rawData.vibeTrackId, rawData.trackNameKr, rawData.lyrics, rawData.vocalists, rawData.lyricists, rawData.composers);
        Favorite fav = new Favorite(track, rawData.addedDate, metadata);
        fav.audioUri = rawData.audioUri;
        fav.playCount = rawData.playCount;
        //fav.playCountByDay = song.playCountByDay;
        fav.firstCountedDate = rawData.firstCountedDate;
        fav.lastPlayedDate = rawData.lastPlayedDate;
        return fav;
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
        List<Favorite> favoriteList = new ArrayList<>();
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
            track.primaryColor = song.primaryColor;
            TrackMetadata metadata = new TrackMetadata(song.vibeTrackId, song.trackNameKr, song.lyrics, song.vocalists, song.lyricists, song.composers);
            Favorite fav = new Favorite(track, song.addedDate, metadata);
            fav.audioUri = song.audioUri;
            fav.playCount = song.playCount;
            fav.playCountByDay = song.playCountByDay;
            fav.firstCountedDate = song.firstCountedDate;
            fav.lastPlayedDate = song.lastPlayedDate;
            favoriteList.add(fav);
        }
        return favoriteList;
    }


    public int getFavoritesCount(){
        return favoritesDao.getFavoritesCount();
    }


    public List<Favorite> getAllFavoriteTracks() {
        List<Favorites> songs = favoritesDao.getAllFavorites();
        List<Favorite> favorites = new ArrayList<>();
        for (Favorites song : songs) {
            favorites.add(entityToModel(song));
        }
        return favorites;
    }


    public List<Favorite> getFavoritesByArtistId(String artistId) {
        List<Favorites> rawList = favoritesDao.getFavoritesByArtistId(artistId);
        List<Favorite> converted = new ArrayList<>();
        for (Favorites item: rawList) {
            converted.add(entityToModel(item));
        }
        return converted;
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
        existing.primaryColor = favorite.track.primaryColor;

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
        converted.primaryColor = favorite.track.primaryColor;
        int result = favoritesDao.updateFavoriteSong(converted);
        if (result > 0) callback.onSuccess();
        else callback.onFailure();
    }


    public int deleteFavoritesByIds(List<String> trackIds){
        return favoritesDao.deleteFavoritesByIds(trackIds);
    }
}




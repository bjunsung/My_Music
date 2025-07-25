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
            Favorite model = entityToModelWithoutPlayCountByDay(item);
            byId.put(item.trackId, model);
        }

        // 3) 원래 ids 순서대로 재배열 (중복 보존, 누락 ID는 자동 스킵)
        List<Favorite> ordered = new ArrayList<>(ids.size());
        for (String id : ids) {
            Favorite f = byId.get(id);
            if (f != null && !f.isHidden) ordered.add(f);
        }

        return ordered;
    }

    public List<Favorite> getFavoritesByIdsIncludeHidden(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>(ids);
        List<Favorites> rawDataList = favoritesDao.getFavoritesByIds(new ArrayList<>(uniqueIds));
        if (rawDataList == null || rawDataList.isEmpty()) return new ArrayList<>();
        Map<String, Favorite> byId = new HashMap<>(rawDataList.size());
        for (Favorites item : rawDataList) {
            Favorite model = entityToModelWithoutPlayCountByDay(item);
            byId.put(item.trackId, model);
        }
        List<Favorite> ordered = new ArrayList<>(ids.size());
        for (String id : ids) {
            Favorite f = byId.get(id);
            if (f != null) ordered.add(f);
        }

        return ordered;
    }


    private Favorite entityToModelWithoutPlayCountByDay(Favorites entity) {
        if(entity == null) {
            return null;
        }
        Track track = new Track(
                entity.trackId,
                entity.albumId,
                entity.artistId,
                entity.trackName,
                entity.albumName,
                entity.artistName,
                entity.artworkUrl,
                entity.releaseDate,
                entity.durationMs);
        track.primaryColor = entity.primaryColor;
        TrackMetadata metadata = new TrackMetadata(entity.vibeTrackId, entity.trackNameKr, entity.lyrics, entity.vocalists, entity.lyricists, entity.composers);
        Favorite model = new Favorite(track, entity.addedDate, metadata);
        model.audioUri = entity.audioUri;
        model.playCount = entity.playCount;
        //fav.playCountByDay = song.playCountByDay;
        model.firstCountedDate = entity.firstCountedDate;
        model.lastPlayedDate = entity.lastPlayedDate;
        model.isHidden =  entity.isHidden;
        return model;
    }


    public Favorite getFavoritesSong(String trackId){
        Favorites song = favoritesDao.getFavoritesSong(trackId);
        if(song == null || song.isHidden) {
            return null;
        }
        return entityToModelWithoutPlayCountByDay(song);
    }

    public Favorite getFavoriteIncludeHidden(String trackId) {
        Favorites song = favoritesDao.getFavoritesSong(trackId);
        return entityToModelWithoutPlayCountByDay(song);
    }


    public Favorite getFavoriteSongWithPlayCount(String trackId) {
        Favorites song = favoritesDao.getFavoritesSong(trackId);
        if(song == null || song.isHidden) {
            return null;
        }
        Favorite model = entityToModelWithoutPlayCountByDay(song);
        model.playCountByDay = song.playCountByDay;
        return model;
    }

    public List<Favorite> getAllFavoriteTracksWithPlayCount() {
        List<Favorites> songs = favoritesDao.getAllFavorites();
        List<Favorite> favoriteList = new ArrayList<>();
        for (Favorites song : songs) {
            if (!song.isHidden) {
                Favorite item = entityToModelWithoutPlayCountByDay(song);
                item.playCountByDay = song.playCountByDay;
                favoriteList.add(item);
            }
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
            if (!song.isHidden)
                favorites.add(entityToModelWithoutPlayCountByDay(song));
        }
        return favorites;
    }


    public List<Favorite> getFavoritesByArtistId(String artistId) {
        List<Favorites> rawList = favoritesDao.getFavoritesByArtistId(artistId);
        List<Favorite> converted = new ArrayList<>();
        for (Favorites item: rawList) {
            if (!item.isHidden)
                converted.add(entityToModelWithoutPlayCountByDay(item));
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

    private Favorites modelToEntityWithoutPlayCountByDay(Favorite model) {
        if (model == null) return null;
        Favorites entity = favoritesDao.getFavoritesSong(model.track.trackId);
        TrackMetadata metadata = model.metadata;
        entity.addedDate = model.addedDate;
        entity.vibeTrackId = metadata.vibeTrackId;
        entity.trackNameKr = metadata.title;
        entity.lyrics = metadata.lyrics;
        entity.vocalists = metadata.vocalists;
        entity.lyricists = metadata.lyricists;
        entity.composers = metadata.composers;
        entity.audioUri = model.audioUri;
        entity.primaryColor = model.track.primaryColor;
        entity.isHidden = model.isHidden;
        entity.playCount = model.playCount;
        return entity;
    }

    public void updateFavoriteSongExceptPlayCount(Favorite favorite, FavoriteDbCallback callback) {
        Favorites entity = modelToEntityWithoutPlayCountByDay(favorite);
        int result = favoritesDao.updateFavoriteSong(entity);
        if (result > 0) callback.onSuccess();
        else callback.onFailure();
    }

    public void updateFavoriteSongWithPlayCount(Favorite favorite, FavoriteDbCallback callback) {
        Favorites entity = modelToEntityWithoutPlayCountByDay(favorite);
        entity.playCountByDay = favorite.playCountByDay;
        entity.lastPlayedDate = favorite.lastPlayedDate;
        entity.firstCountedDate = favorite.firstCountedDate;
        int result = favoritesDao.updateFavoriteSong(entity);
        if (result > 0) callback.onSuccess();
        else callback.onFailure();
    }

    public int deleteFavoritesByIds(List<String> trackIds){
        return favoritesDao.deleteFavoritesByIds(trackIds);
    }

    public List<Favorite> getHiddenTracks() {
        List<Favorites> songs = favoritesDao.getAllFavorites();
        List<Favorite> result = new ArrayList<>();
        for (Favorites song : songs) {
            if (song.isHidden)
                result.add(entityToModelWithoutPlayCountByDay(song));
        }
        return result;
    }
}




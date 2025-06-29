package com.example.mymusic.ui.favorites;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;

import com.example.mymusic.data.repository.FavoriteSongRepository;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;

import java.util.List;

public class FavoritesViewModel extends AndroidViewModel {
    private final FavoriteSongRepository repository;

    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteSongRepository(application);
    }

    /**
     * DB에서 데이터를 가져오고 결과를 콜백(Consumer)으로 넘김
     */
    public void loadAllFavorites(Consumer<List<Favorite>> callback) {
        new Thread(() -> {
            List<Favorite> result = repository.getAllFavoriteTracks(); // 직접 반환하는 DAO 사용
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result)); // UI 스레드로 전달
        }).start();
    }

    public void deleteFavoriteSong(Track track){
        new Thread(() -> {
            repository.deleteFavoritesSong(track.trackId);
        }).start();
    }
    public void insert(Track track, String addedDate) {
        repository.saveFavoritesSong(track, addedDate); // 내부에서 Thread 처리
    }

    /**
     * DB에서 데이터를 가져오고 결과를 콜백(Consumer)으로 넘김
     */
    public void loadFavoriteItem(String trackId, Consumer<Favorite> callback) {
        new Thread(() -> {
          Favorite result = repository.getFavoritesSong(trackId);
          new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        }).start();
    }

    public void updateMetadata(String trackId, TrackMetadata metadata, Consumer<Integer> callback){
        new Thread(() -> {
            repository.updateFavoriteSong(trackId, metadata, callback);
        }).start();
    }



}
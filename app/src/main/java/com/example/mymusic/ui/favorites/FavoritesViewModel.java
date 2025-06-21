package com.example.mymusic.ui.favorites;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;

import com.example.mymusic.data.repository.FavoriteSongsRepository;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;

import java.util.List;

public class FavoritesViewModel extends AndroidViewModel {
    private final FavoriteSongsRepository repository;

    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteSongsRepository(application);
    }

    /**
     * DB에서 데이터를 가져오고 결과를 콜백(Consumer)으로 넘김
     */
    public void loadFavorites(Consumer<List<Favorite>> callback) {
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
}
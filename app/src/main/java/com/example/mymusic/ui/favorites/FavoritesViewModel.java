package com.example.mymusic.ui.favorites;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mymusic.data.repository.FavoriteSongRepository;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;

import java.util.List;
import java.util.function.Consumer;

public class FavoritesViewModel extends AndroidViewModel {
    private final FavoriteSongRepository repository;
    private final MutableLiveData<Favorite> focusedTrack = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLyricsMode = new MutableLiveData<>(false);
    private int focusedPosition = -1;


    public void setLyricsMode(boolean value) {
        isLyricsMode.setValue(value);
    }

    public LiveData<Boolean> getLyricsMode() {
        return isLyricsMode;
    }


    public void setFocusedTrack(Favorite favorite) {
        focusedTrack.setValue(favorite);
    }

    public LiveData<Favorite> getFocusedTrack() {
        return focusedTrack;
    }

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
            repository.updateFavoriteSong(trackId, metadata, callback::accept);
        }).start();
    }

    public void deleteFavoritesByIds(List<String> trackIds, Consumer<Integer> callback){
        new Thread(() -> {
            int result = repository.deleteFavoritesByIds(trackIds);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        }).start();
    }

    public void getFavoritesCount(Consumer<Integer> callback){
        new Thread(() -> {
            int result = repository.getFavoritesCount();
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        }).start();
    }


    public int getFocusedPosition() {
        return focusedPosition;
    }

    public void setFocusedPosition(int focusedPosition) {
        this.focusedPosition = focusedPosition;
    }
}
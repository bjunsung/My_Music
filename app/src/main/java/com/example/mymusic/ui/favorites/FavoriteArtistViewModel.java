package com.example.mymusic.ui.favorites;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import com.example.mymusic.data.local.FavoriteArtist;
import com.example.mymusic.data.repository.FavoriteArtistRepository;
import com.example.mymusic.data.repository.FavoriteSongRepository;
import com.example.mymusic.model.Artist;

import java.util.List;

public class FavoriteArtistViewModel extends AndroidViewModel {


    private final FavoriteArtistRepository repository;

    public FavoriteArtistViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteArtistRepository(application);
    }

    /**
     * DB에서 데이터를 가져오고 결과를 콜백(Consumer)으로 넘김
     */
    public void loadFavorites(Consumer<List<Artist>> callback) {
        new Thread(() -> {
            List<Artist> result = repository.getAllFavoriteArtist(); // 직접 반환하는 DAO 사용
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result)); // UI 스레드로 전달
        }).start();
    }

    public void deleteFavoriteArtist(Artist artist){
        new Thread(() -> {
            repository.deleteFavoriteArtist(artist.artistId);
        }).start();
    }
    public void insert(Artist artist, String addedDate) {
        repository.saveFavoriteArtist(artist, addedDate); // 내부에서 Thread 처리
    }

    //백그라운드 Thread 에서 db접근 후 UI Thread 로 넘기기
    public void getAddedDateAsync(String artistId, Consumer<String> callback){
        new Thread(() -> {
            FavoriteArtist artist = repository.getFavoriteArtist(artistId);
            if (artist == null) new Handler(Looper.getMainLooper()).post(() -> callback.accept((null)));
            else new Handler(Looper.getMainLooper()).post(() -> {
                callback.accept((artist.addedDate));
            });
        }).start();
    }


}
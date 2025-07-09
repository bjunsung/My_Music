package com.example.mymusic.ui.favorites;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import androidx.lifecycle.AndroidViewModel;

import com.example.mymusic.data.local.FavoriteArtist;
import com.example.mymusic.data.repository.ArtistMetadataRepository;
import com.example.mymusic.data.repository.FavoriteArtistRepository;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FavoriteArtistViewModel extends AndroidViewModel {


    private final FavoriteArtistRepository repository;
    private final ArtistMetadataRepository artistMetadataRepository;
    public Bundle reenterState = null;
    private int scrollPosition = 0;
    private int scrollOffset = 0;
    private int reenterScrollPosition = 0;
    private int reenterScrollOffset = 0;
    public List<Artist> selectedList = new ArrayList<>();

    public FavoriteArtistViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteArtistRepository(application);
        artistMetadataRepository = new ArtistMetadataRepository(application);
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

    public void loadFavoriteArtistByArtistId(String artistID, Consumer<com.example.mymusic.model.FavoriteArtist> callback){
        new Thread(() ->  {
            com.example.mymusic.data.local.FavoriteArtist loaded = repository.getFavoriteArtist(artistID);
            if (loaded != null){
                Artist artist = new Artist(loaded.artistId, loaded.artistName, loaded.artworkUrl, loaded.genres, loaded.followers, loaded.popularity);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(new com.example.mymusic.model.FavoriteArtist(artist, loaded.addedDate)));
            }
            else
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
        }).start();
    }

    public void deleteFavoriteArtist(String artistId){
        new Thread(() -> {
            repository.deleteFavoriteArtist(artistId);
        }).start();
    }

    public void deleteFavoritesArtistByIds(List<String> artistIds, Consumer<Integer> callback){
        new Thread(() -> {
            int result = repository.deleteFavoriteArtistsByIds(artistIds);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
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

    public void getArtistNameById(String artistId, Consumer<String> callback){
        new Thread(() -> {
            FavoriteArtist artist = repository.getFavoriteArtist(artistId);
            if (artist == null) new Handler(Looper.getMainLooper()).post(() -> callback.accept((null)));
            else new Handler(Looper.getMainLooper()).post(() -> {
                callback.accept((artist.artistName));
            });
        }).start();
    }

    public void getFavoriteArtistsCount(Consumer<Integer> callback){
        new Thread(() -> {
            int result = repository.getFavoriteArtistCount();
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        }).start();
    }


    /// ArtistMetadata 관련
    public void updateArtistMetadata(ArtistMetadata metadata, java.util.function.Consumer<Boolean> callback){
        new Thread(() -> {
            artistMetadataRepository.updateArtistMetadata(metadata, callback);
        }).start();
    }


    //같은 spotify id 에 다른 vibe id 가 들어가면 spotify id 여러개가 db에 저장됨
    //다른 spotify id 에 다른 vibe id 가 들어가면 기존 spotify id mapping 은 삭제됨
    public void addArtistMetadata(ArtistMetadata metadata, FavoriteArtistViewModel.addMetadataCallback callback){
        new Thread(() -> {
            ArtistMetadata duplicatedSpotifyId =  artistMetadataRepository.getArtistMetadataBySpotifyId(metadata.spotifyArtistId);
            ArtistMetadata duplicatedVibeId = artistMetadataRepository.getArtistMetadata(metadata.vibeArtistId);
            if (duplicatedSpotifyId == null && duplicatedVibeId == null) {
                artistMetadataRepository.addArtistMetadata(metadata);
                callback.onSuccess();
            }
            else if (duplicatedSpotifyId != null){
                callback.onFailure(duplicatedSpotifyId, "Spotify id is duplicated");
            }
            else{
                callback.onFailure(duplicatedVibeId, "Vibe id is duplicated");
            }
        }).start();
    }

    public void loadArtistMetadataBySpotifyId(String artistId, FavoriteArtistViewModel.MetadataCallback callback){
        new Thread(() -> {
          ArtistMetadata metadata = artistMetadataRepository.getArtistMetadataBySpotifyId(artistId);
          if (metadata != null) {
              callback.onSuccess(metadata);
          }
          else{
              callback.onFailure("No Metadata found for " + artistId);
          }
        }).start();
    }

    public void deleteArtistMetadataBySpotifyId(String spotifyId, Consumer<String> callback){
        new Thread(() -> {
            int removed = artistMetadataRepository.removeArtistMetadataBySpotifyId(spotifyId);
            if (removed > 0){
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.accept("Remove Success");
                });
            }
            else{
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.accept("Remove Fail");
                });
            }
        }).start();
    }

    public int getScrollPosition() {
        return scrollPosition;
    }

    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = scrollPosition;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    public int getReenterScrollPosition() {
        return reenterScrollPosition;
    }

    public void setReenterScrollPosition(int reenterScrollPosition) {
        this.reenterScrollPosition = reenterScrollPosition;
    }

    public int getReenterScrollOffset() {
        return reenterScrollOffset;
    }

    public void setReenterScrollOffset(int reenterScrollOffset) {
        this.reenterScrollOffset = reenterScrollOffset;
    }


    public interface MetadataCallback{
        void onSuccess(ArtistMetadata metadata);
        void onFailure(String reason);
    }

    public interface addMetadataCallback{
        void onSuccess();
        void onFailure(ArtistMetadata metadata, String reason);
    }

}
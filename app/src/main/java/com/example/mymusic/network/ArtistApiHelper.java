package com.example.mymusic.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Consumer;

import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;
import com.example.mymusic.ui.search.SearchFragment;

import java.util.ArrayList;
import java.util.List;

public class ArtistApiHelper {
    private final String TAG = "ArtistApiHelper";

    private Context context;
    private Activity activity; //Ui 작업 actvity.runOnUiThread()
    private TokenRepository tokenRepository;
    public ArtistApiHelper(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
        tokenRepository = new TokenRepository(context);
    }

    private void getAccessToken(Consumer<String> onSuccess, Consumer<String> onFailure){
        new Thread(() -> {
            Token token = tokenRepository.getAccessTokenSync();
            if (token != null && token.getAccessToken()!= null) {
                String accessToken = token.getAccessToken();
                new Handler(Looper.getMainLooper()).post(() -> onSuccess.accept(accessToken));
            }
            else {
                //refresh accessToken
                refreshToken(refreshedToken -> {
                    if (refreshedToken != null)
                        new Handler(Looper.getMainLooper()).post(() -> onSuccess.accept(refreshedToken));
                    else
                        new Handler(Looper.getMainLooper()).post(() -> onFailure.accept("토큰 재발급 실패"));
                });

            }
        }).start();
    }

    private void refreshToken(Consumer<String> callback){
        TokenHelper.refreshTokenWithUI(context, null, accessToken -> {
            if (accessToken != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(accessToken));
            } else {
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
            }
        }, error -> {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
        });
    }

    public void alertError(String errorMessage){
        String[] parts = errorMessage.split(",", 2);
        String titleMessage = parts[0];
        String message = parts[1];
        activity.runOnUiThread(() -> {
            new AlertDialog.Builder(context)
                    .setTitle(titleMessage)
                    .setMessage(message)
                    .setPositiveButton("확인", null)
                    .show();
        });
    }

    public interface ApiExecutor<T> {
        void execute(String refreshedToken, Consumer<T> onSuccess, Consumer<String> onFailure);
    }

    private <T> void executeWithToken(ApiExecutor<T> apiExecutor, Consumer<T> callback){
        Log.d(TAG, "try fetch access token to db");
        getAccessToken(token -> {
            Log.d(TAG, "success to fetch access token\nstarting to API call");
            apiExecutor.execute(token, callback, errorMessage -> {
                if (errorMessage.contains("만료") || errorMessage.contains("expired")){
                    refreshToken(refreshed -> {
                        if (refreshed != null) {
                            apiExecutor.execute(refreshed, callback, error -> new Handler(Looper.getMainLooper()).post(() -> callback.accept(null)));
                        }
                        else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
                        }
                    });
                } else{
                    alertError(errorMessage);
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
                }
            });
        }, error -> {
            Log.d(TAG, "토큰 발급 실패, 탐색을 종료합니다");
            alertError("Error, 토큰 발급 실패");
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
        });
    }


    public void searchTrackByArtist(String refreshedToken, String artistId, Consumer<List<Track>> callback){
        if (refreshedToken != null){
            TrackSearchService.searchTrackByArtist(artistId, refreshedToken, callback, this::alertError);
        }
        else {
            executeWithToken((refreshed_, onSuccess, onFailure) ->
                            TrackSearchService.searchTrackByArtist(artistId, refreshed_, onSuccess, onFailure),
                    callback
            );
        }
    }

    public void getArtist(String refreshedToken, String artistId, Consumer<Artist> callback){
        if (refreshedToken != null){
            ArtistSearchService.searchArtistByArtistId(artistId, refreshedToken, callback, this::alertError);
        }
        else {
            executeWithToken((refreshed_, onSuccess, onFailure) ->
                            ArtistSearchService.searchArtistByArtistId(artistId, refreshed_, onSuccess, onFailure),
                    callback
            );
        }
    }


    public void getAlbum(String refreshedToken, String albumId, Consumer<Album> callback){
        if (refreshedToken != null){
            AlbumSearchService.searchAlbumByAlbumId(albumId, refreshedToken, callback, this::alertError);
        }
        else {
            executeWithToken((refreshed_, onSuccess, onFailure) ->
                            AlbumSearchService.searchAlbumByAlbumId(albumId, refreshed_, onSuccess, onFailure),
                    callback
            );
        }
    }

    public void searchAlbumsByArtist(String refreshedToken ,String artistId, Consumer<List<Album>> callback) {
        if (refreshedToken != null){
            AlbumSearchService.searchAlbumByArtist(artistId, this.context, refreshedToken, callback, this::alertError);
        }
        else {
            executeWithToken((refreshed_, onSuccess, onFailure) ->
                            AlbumSearchService.searchAlbumByArtist(artistId, this.context, refreshed_, onSuccess, onFailure),
                    callback
            );
        }
    }


    public void searchTrackByAlbum(String refreshedToken, Album album, Consumer<List<Track>> callback){
        if (refreshedToken != null){
            TrackSearchService.searchTrackByAlbum(album, refreshedToken, callback, this::alertError);
        }
        else {
            executeWithToken((refreshed_, onSuccess, onFailure) ->
                            TrackSearchService.searchTrackByAlbum(album, refreshed_, onSuccess, onFailure),
                    callback
            );
        }
    }





}


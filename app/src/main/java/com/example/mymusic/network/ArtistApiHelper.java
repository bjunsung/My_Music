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
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.Track;
import com.example.mymusic.ui.search.SearchFragment;

import java.util.List;

public class ArtistApiHelper {

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
                TokenHelper.refreshTokenWithUI(context, null, () -> {
                    //get accessToken
                    Token refreshed = tokenRepository.getAccessTokenSync();
                    if (refreshed != null && refreshed.getAccessToken() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> onSuccess.accept(refreshed.getAccessToken()));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> onFailure.accept("토큰 발급 실패"));
                    }
                }, error -> {new Handler(Looper.getMainLooper()).post(() -> onFailure.accept("토큰 refresh 실패" + error));
                });
            }
        }).start();
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

    public void searchAlbumsByArtist(String artistId, Consumer<List<Album>> callback) {
        getAccessToken(token -> {
            AlbumSearchService.searchAlbumByArtist(artistId, token, callback, this::alertError);
        }, error -> {
            // 실패 시 null 반환 (또는 오류 메시지를 별도로 처리해도 됨)
            callback.accept(null);
        });
    }

    public void searchTrackByArtist(String artistId, Consumer<List<Track>> callback){
        getAccessToken(token -> {
            TrackSearchService.searchTrackByArtist(artistId, token, callback, this::alertError);
        }, error -> {
            callback.accept(null);
        });
    }

    public void searchTrackByAlbum(Album album, Consumer<List<Track>> callback){
        getAccessToken(token -> {
            TrackSearchService.searchTrackByAlbum(album, token, callback, this::alertError);
        }, error -> {
            callback.accept(null);
        });
    }

}


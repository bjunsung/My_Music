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
        TokenHelper.refreshTokenWithUI(context, null, () -> {
            Token refreshed = tokenRepository.getAccessTokenSync();
            if (refreshed != null && refreshed.getAccessToken() != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(refreshed.getAccessToken()));
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

    public void searchAlbumsByArtist(String artistId, Consumer<List<Album>> callback, int refreshCount) {
        if (refreshCount >= 3) {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept((new ArrayList<>())));
            return;
        }
        getAccessToken(token -> {
            AlbumSearchService.searchAlbumByArtist(artistId, context, token, callback, this::alertError);
        }, error -> {
            // 실패 시 재발급 후 다시 검색
            refreshToken(refreshedToken -> {
                if (refreshedToken != null)
                    new Handler(Looper.getMainLooper()).post(() -> searchAlbumsByArtist(artistId, callback, refreshCount + 1));
                else
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept((new ArrayList<>())));
            });
        });
    }

    public void searchTrackByArtist(String artistId, Consumer<List<Track>> callback, int refreshCount){
        if (refreshCount >= 3) {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept((new ArrayList<>())));
            return;
        }
        getAccessToken(token -> {
            TrackSearchService.searchTrackByArtist(artistId, token, callback, this::alertError);
        }, error -> {
            // 실패 시 재발급 후 다시 검색
            refreshToken(refreshedToken -> {
                if (refreshedToken != null)
                    new Handler(Looper.getMainLooper()).post(() -> searchTrackByArtist(artistId, callback, refreshCount + 1));
                else
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept((new ArrayList<>())));
            });
        });
    }

    public void searchTrackByAlbum(Album album, Consumer<List<Track>> callback, int refreshCount){
        if (refreshCount >= 3) {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept((new ArrayList<>())));
            return;
        }
        getAccessToken(token -> {
            TrackSearchService.searchTrackByAlbum(album, token, callback, this::alertError);
        }, error -> {
            // 실패 시 재발급 후 다시 검색
            refreshToken(refreshedToken -> {
                if (refreshedToken != null)
                    new Handler(Looper.getMainLooper()).post(() -> searchTrackByAlbum(album, callback, refreshCount + 1));
                else
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept((new ArrayList<>())));
            });
        });
    }

    public void getArtist(String artistId, Consumer<Artist> callback, int refreshCount){
        if (refreshCount >= 3){
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
            return;
        }
        getAccessToken(token -> {
            ArtistSearchService.searchArtistByArtistId(artistId, token, callback, this::alertError);
        }, error -> {
            // 실패 시 재발급 후 다시 검색
            refreshToken(refreshedToken -> {
                if (refreshedToken != null)
                    new Handler(Looper.getMainLooper()).post(() -> getArtist(artistId, callback, refreshCount + 1));
                else
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
            });
        });
    }


    public void getAlbum(String albumId, int refreshCount, Consumer<Album> callback){
        getAccessToken(token -> {
            AlbumSearchService.searchAlbumByAlbumId(albumId, token, callback, errorMessage -> {
                // 실패 시 재발급 후 다시 검색
                if (errorMessage.equals("Error,만료된 토큰입니다.") && refreshCount < 3){
                    refreshToken(refreshed -> {
                        if (refreshed != null)
                            new Handler(Looper.getMainLooper()).post(() -> getAlbum(albumId, refreshCount + 1, callback));
                        else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));

                        }
                    });
                }
                else{ // 토큰 만료에 의한 접근 실패가 아닌 경우 에러 메시지 출력하고 null callback
                    alertError(errorMessage);
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
                }
            });
        }, error -> {
            //token access 실패
            alertError("Error,token access fail");
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
        }
        );
    }





}


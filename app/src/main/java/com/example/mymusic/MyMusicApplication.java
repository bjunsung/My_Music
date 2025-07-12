package com.example.mymusic;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.mymusic.cache.ImagePreloader;
import com.example.mymusic.cache.customCache.CustomImageCache;
import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.network.TokenHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;

public class MyMusicApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        refreshToken();
        Glide.get(this).clearMemory();
        new Thread(() -> Glide.get(this).clearDiskCache()).start();

        setCustomCache();

    }


    private void setCustomCache(){
        int cacheSize = 5;
        CustomImageCache.init(cacheSize);
    }
    private void refreshToken(){
        // Repository 초기화
        TokenRepository tokenRepository = new TokenRepository(getApplicationContext());

        //accessToken 발급
        TokenHelper.getAccessToken(getApplicationContext(), new TokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String accessToken) {
                tokenRepository.setAccessToken(accessToken);
                Log.d("MyMusicApp", "Access token 저장 완료: " + accessToken);
            }

            @Override
            public void onFailure(String error) {
                Log.e("MyMusicApp", "Access token 발급 실패: " + error);
            }
        });
    }


}

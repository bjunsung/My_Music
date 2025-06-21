package com.example.mymusic;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.network.TokenHelper;

public class MyMusicApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        refreshToken();
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

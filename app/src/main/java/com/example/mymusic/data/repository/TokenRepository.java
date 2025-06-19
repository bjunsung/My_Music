package com.example.mymusic.data.repository;

import android.content.Context;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.local.TokenDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenRepository {
    private final TokenDao tokenDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public TokenRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        tokenDao = db.tokenDao();
    }
    // 비동기로 accessToken 저장
    public void setAccessToken(String accessToken) {
        executor.execute(() -> {
            Token token = new Token(accessToken); // tokenId는 생성자에서 자동 0
            tokenDao.setAccessToken(token);
        });
    }

    public Token getAccessTokenSync(){
        return tokenDao.getAccessToken(); // 반드시 백그라운드 스레드에서 호출
    }
}

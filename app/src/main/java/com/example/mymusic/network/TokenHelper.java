package com.example.mymusic.network;

import android.content.Context;
import android.util.Base64;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.local.TokenDao;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class TokenHelper {
    private static final String CLIENT_ID = "a4a54268175942f1b1d8d8389428145d";
    private static final String CLIENT_SECRET = "822bbcc3bc154517962e027b1a39260b";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    public interface TokenCallback {
        void onSuccess(String accessToken);
        void onFailure(String error);
    }

    public static void getAccessToken(Context context, TokenCallback callback) {
        OkHttpClient client = new OkHttpClient();

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(new FormBody.Builder().add("grant_type", "client_credentials").build())
                .addHeader("Authorization", basicAuth)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("HTTP " + response.code());
                    return;
                }
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String accessToken = json.getString("access_token");
                    callback.onSuccess(accessToken);

                    //DB 에 저장
                    AppDatabase tokenDb = AppDatabase.getInstance(context.getApplicationContext());
                    TokenDao tokenDao = tokenDb.tokenDao();
                    Token tokenEntity = new Token(accessToken);
                    tokenDao.setAccessToken(tokenEntity);
                } catch (Exception e) {
                    callback.onFailure("Parse error: " + e.getMessage());
                }
            }
        });
    }
}

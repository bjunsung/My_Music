package com.example.mymusic.network;

import android.util.Base64;

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

    public static void getAccessToken(TokenCallback callback) {
        OkHttpClient client = new OkHttpClient();

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .addHeader("Authorization", basicAuth)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        client.newCall(request).enqueue(new Callback() {
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("TokenHelper", "Connection failed: " + e.getMessage());
                callback.onFailure("Connection error: " + e.getMessage());
            }

            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                android.util.Log.d("TokenHelper", "HTTP " + response.code());
                android.util.Log.d("TokenHelper", "Body: " + body);

                if (!response.isSuccessful()) {
                    callback.onFailure("Token error: " + response.code() + "\n" + body);
                    return;
                }

                try {
                    JSONObject json = new JSONObject(body);
                    String token = json.getString("access_token");
                    callback.onSuccess(token);
                } catch (Exception e) {
                    callback.onFailure("Parse error: " + e.getMessage());
                }
            }
        });
    }

}

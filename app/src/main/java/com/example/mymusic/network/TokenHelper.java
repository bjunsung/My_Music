package com.example.mymusic.network;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.local.TokenDao;
import com.example.mymusic.data.repository.TokenRepository;

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
                    //DB 에 저장
                    TokenRepository repository = new TokenRepository(context.getApplicationContext());
                    repository.setAccessToken(accessToken);
                    callback.onSuccess(accessToken);
                } catch (Exception e) {
                    callback.onFailure("Parse error: " + e.getMessage());
                }
            }
        });
    }


    public static void refreshTokenWithUI(
            Context context,
            @Nullable Fragment fragment, // null 가능
            @Nullable Consumer<String> onSuccess,
            @Nullable Consumer<String> onFailure) {

        getAccessToken(context, new TokenCallback() {
            @Override
            public void onSuccess(String accessToken) {
                Runnable runTask = () -> {
                    Log.d("TokenHelper", "SUCCESS to get new Access Token: " + accessToken);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (onSuccess != null){
                            onSuccess.accept(accessToken);
                        }
                    });
                };

                if (fragment != null && fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(runTask);
                } else {
                    new Handler(Looper.getMainLooper()).post(runTask);
                }
            }

            @Override
            public void onFailure(String error) {
                Runnable runTask = () -> {
                    if (onFailure != null) onFailure.accept(error);
                    new AlertDialog.Builder(context)
                            .setTitle("token error")
                            .setMessage(error)
                            .setPositiveButton("exit", null)
                            .show();
                };

                if (fragment != null && fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(runTask);
                } else {
                    new Handler(Looper.getMainLooper()).post(runTask);
                }
            }
        });
    }


}

package com.example.mymusic.network;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

public class ArtistVibeLinkBridge {
    private static final String TAG = "ArtistVibeBridge";

    public interface OnLinkListener {
        void onLinkFound(String name, String link);
        void onLinkNotFound(String name); // [추가됨] 링크를 못찾았을 때 호출될 인터페이스
    }

    private OnLinkListener listener;
    private boolean isFinished = false;

    public void setListener(OnLinkListener listener) {
        this.listener = listener;
        this.isFinished = false;
    }

    @JavascriptInterface
    public void receiveMetadata(String json) {
        if (isFinished) return;
        isFinished = true;

        Log.d(TAG, "SUCCESS JSON: " + json);
        try {
            Gson gson = new Gson();
            LinkRaw raw = gson.fromJson(json, LinkRaw.class);

            if (listener != null) {
                listener.onLinkFound(raw.name, raw.link);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing metadata", e);
        }
    }

    // [추가됨] Javascript가 '찾을 수 없음'을 알리는 메소드
    @JavascriptInterface
    public void reportNotFound(String name) {
        if (isFinished) return;
        isFinished = true;

        Log.w(TAG, "NOT FOUND for: " + name);
        if (listener != null) {
            listener.onLinkNotFound(name);
        }
    }

    static class LinkRaw {
        String name;
        String link;
    }
}
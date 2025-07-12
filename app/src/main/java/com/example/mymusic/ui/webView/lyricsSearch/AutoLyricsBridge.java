package com.example.mymusic.ui.webView.lyricsSearch;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.mymusic.model.TrackMetadata;
import com.google.gson.Gson;

import java.util.Arrays;

/**
 * 최종 수정된 AutoLyricsBridge 클래스
 */
public class AutoLyricsBridge {
    private final String TAG = "AutoLyricsBridge";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isFinished = false;

    // --- Listener for UI Visibility ---
    public interface Listener {
        void onLyricsOpened();
        void onLyricsClosed();
    }
    private Listener visibilityListener;
    public void setVisibilityListener(Listener listener) {
        this.visibilityListener = listener;
    }

    // --- Listener for Metadata Result ---
    public interface OnMetadataListener {
        void onSuccess(TrackMetadata metadata);
        void onFailure(String reason);
    }
    private OnMetadataListener metadataListener;
    public void setMetadataListener(OnMetadataListener metadataListener) {
        this.metadataListener = metadataListener;
        this.isFinished = false;
    }

    // --- Listener for Member Link Result ---
    public interface OnMemberLinkListener {
        void onMemberLinkFound(String name, String link);
        void onMemberLinkNotFound(String name);
    }
    private OnMemberLinkListener memberLinkListener;
    private boolean isMemberSearchFinished = false;

    public void setMemberLinkListener(OnMemberLinkListener listener) {
        this.memberLinkListener = listener;
    }

    public void resetMemberSearch() {
        this.isMemberSearchFinished = false;
    }

    // --- JavascriptInterface Methods ---
    @JavascriptInterface
    public void lyricsDetected(boolean isVisible) {
        if (visibilityListener == null) return;
        mainHandler.post(() -> {
            if (isVisible) {
                visibilityListener.onLyricsOpened();
            } else {
                visibilityListener.onLyricsClosed();
            }
        });
    }

    @JavascriptInterface
    public void receiveMetadata(String json) {
        if (isFinished || json == null || json.trim().isEmpty()) return;
        isFinished = true;
        if (metadataListener != null) {
            try {
                Gson gson = new Gson();
                MetadataRaw raw = gson.fromJson(json, MetadataRaw.class);
                TrackMetadata metadata = new TrackMetadata();

                // ✅ 2. 전달받은 vibeTrackId를 메타데이터 객체에 설정
                metadata.vibeTrackId = raw.vibeTrackId;

                metadata.title = raw.title;
                metadata.lyrics = raw.lyrics;
                metadata.artistLink = raw.artistLink; // artistLink도 함께 처리
                if (raw.lyricists != null) metadata.lyricists = Arrays.asList(raw.lyricists);
                if (raw.composers != null) metadata.composers = Arrays.asList(raw.composers);
                if (raw.vocalists != null) metadata.addVocalists(Arrays.asList(raw.vocalists));

                mainHandler.post(() -> metadataListener.onSuccess(metadata));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse metadata", e);
                mainHandler.post(() -> metadataListener.onFailure("JSON 파싱 실패: " + e.getMessage()));
            }
        }
    }

    @JavascriptInterface
    public void reportFailure(String reason) {
        if (isFinished) return;
        isFinished = true;
        if (metadataListener != null) {
            mainHandler.post(() -> metadataListener.onFailure(reason));
        }
    }

    @JavascriptInterface
    public void receiveMemberLink(String json) {
        if (isMemberSearchFinished) return;
        isMemberSearchFinished = true;
        mainHandler.post(() -> {
            if (memberLinkListener != null) {
                try {
                    Gson gson = new Gson();
                    LinkRaw raw = gson.fromJson(json, LinkRaw.class);
                    memberLinkListener.onMemberLinkFound(raw.name, raw.link);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing member link json", e);
                }
            }
        });
    }

    @JavascriptInterface
    public void reportMemberNotFound(String name) {
        if (isMemberSearchFinished) return;
        isMemberSearchFinished = true;
        mainHandler.post(() -> {
            if (memberLinkListener != null) {
                memberLinkListener.onMemberLinkNotFound(name);
            }
        });
    }

    // --- DTO Classes ---
    static class MetadataRaw {
        String vibeTrackId; // ✅ 1. vibeTrackId 필드 추가
        String title, artistLink, lyrics;
        String[] vocalists, lyricists, composers;
    }
    static class LinkRaw {
        String name, link;
    }
}
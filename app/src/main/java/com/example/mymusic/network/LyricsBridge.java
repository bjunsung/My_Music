package com.example.mymusic.network;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.mymusic.model.TrackMetadata;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

public class LyricsBridge {

    public interface OnMetadataReceivedListener {
        void onMetadataReceived(TrackMetadata metadata);
    }

    private OnMetadataReceivedListener listener;
    private boolean metadataSent = false; // 중복 방지 플래그

    public void setListener(OnMetadataReceivedListener listener) {
        this.listener = listener;
        this.metadataSent = false; // 매 요청마다 초기화
    }

    @JavascriptInterface
    public void receiveMetadata(String json) {
        if (metadataSent || json == null || json.trim().isEmpty()) {
            Log.d("LyricsBridge", "Skipped duplicate or empty metadata.");
            return;
        }

        Log.d("LyricsBridge", "Received JSON: " + json);



        if (listener != null) {
            try {
                Gson gson = new Gson();
                MetadataRaw raw = gson.fromJson(json, MetadataRaw.class);

                TrackMetadata metadata = new TrackMetadata(
                        null,
                        raw.title,
                        raw.lyrics,
                        Arrays.asList(raw.lyricists),
                        Arrays.asList(raw.composers)
                );
                metadata.setArtistLink(raw.artistLink);
                metadata.addVocalists(Arrays.asList(raw.vocalists));
                listener.onMetadataReceived(metadata);
                metadataSent = true; // 이 위치가 중요
            } catch (Exception e) {
                Log.e("LyricsBridge", "Failed to parse metadata", e);
            }
        }
    }

    // JSON 파싱용 내부 클래스
    static class MetadataRaw {
        String title;
        String artistLink;
        String lyrics;
        String[] vocalists;
        String[] lyricists;
        String[] composers;
    }
}

package com.example.mymusic.network;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.mymusic.model.TrackMetadata;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

public class LyricsBridge {

    // [수정됨] 성공과 실패를 모두 처리하는 리스너로 변경
    public interface OnMetadataListener {
        void onSuccess(TrackMetadata metadata);
        void onFailure(String reason);
    }

    private OnMetadataListener listener;
    private boolean isFinished = false; // 중복 호출 방지 플래그

    public void setListener(OnMetadataListener listener) {
        this.listener = listener;
        this.isFinished = false;
    }



    @JavascriptInterface
    public void receiveMetadata(String json) {
        if (isFinished || json == null || json.trim().isEmpty()) {
            Log.d("LyricsBridge", "Skipped duplicate or empty metadata.");
            return;
        }
        isFinished = true;
        Log.d("LyricsBridge", "Received JSON: " + json);

        if (listener != null) {
            try {
                Gson gson = new Gson();
                MetadataRaw raw = gson.fromJson(json, MetadataRaw.class);

                TrackMetadata metadata = new TrackMetadata();
                metadata.title = raw.title;
                metadata.lyrics = raw.lyrics;
                if(raw.lyricists != null) metadata.lyricists = Arrays.asList(raw.lyricists);
                if(raw.composers != null) metadata.composers = Arrays.asList(raw.composers);
                metadata.artistLink = raw.artistLink;
                if(raw.vocalists != null) metadata.addVocalists(Arrays.asList(raw.vocalists));

                listener.onSuccess(metadata); // onSuccess 호출로 변경

            } catch (Exception e) {
                Log.e("LyricsBridge", "Failed to parse metadata", e);
                listener.onFailure("JSON 파싱 실패: " + e.getMessage());
            }
        }
    }

    // [추가됨] Javascript가 실패를 보고할 수 있는 통로
    @JavascriptInterface
    public void reportFailure(String reason) {
        if (isFinished) return;
        isFinished = true;
        Log.w("LyricsBridge", "Failure reported from Javascript: " + reason);
        if (listener != null) {
            listener.onFailure(reason);
        }
    }

    // [수정 없음] 보내주신 코드와 동일
    static class MetadataRaw {
        String title;
        String artistLink;
        String lyrics;
        String[] vocalists;
        String[] lyricists;
        String[] composers;
    }
}
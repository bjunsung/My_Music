package com.example.mymusic.ui.webView.artistMetadata;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.ui.webView.lyricsSearch.AutoLyricsBridge;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ArtistMetadataWebViewBridge
{
    private final String TAG = "ArtistMetadataWebViewBridge";

    private boolean isFinished = false;
    private MetadataListener metadataListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public void setMetadataListener(MetadataListener metadataListener){
        this.metadataListener = metadataListener;
    }

    public interface VisibleListener {
        void onArtistOpened();
        void onArtistClosed();
    }
    private VisibleListener visibilityListener;
    public void setVisibilityListener(VisibleListener listener) {
        this.visibilityListener = listener;
    }
    public interface MetadataListener{
        void onSuccess(ArtistMetadata metadata);
        void onFailure(String reason);
    }

    @JavascriptInterface
    public void artistLinkDetected(boolean isVisible){
        if (visibilityListener == null) return;
        mainHandler.post(() -> {
            if (isVisible)
                visibilityListener.onArtistOpened();
            else
                visibilityListener.onArtistClosed();
        });
    }
    @JavascriptInterface
    public void receiveMetadata(String json){
        if (isFinished || json == null || json.trim().isEmpty()) {
            Log.d(TAG, "received null json file");
            return;
        }

        if (metadataListener != null){
            try{
                Gson gson = new Gson();
                MetadataRaw raw = gson.fromJson(json, MetadataRaw.class);
                ArtistMetadata metadata = new ArtistMetadata();

                metadata.artistNameKr = raw.artistNameKr;
                metadata.vibeArtistId = raw.vibeArtistId;
                metadata.debutDate = raw.debutDate;
                metadata.biography = raw.biography;
               if (raw.images != null)  metadata.images = Arrays.asList(raw.images);
               if (raw.yearsOfActivity != null) metadata.yearsOfActivity = Arrays.asList(raw.yearsOfActivity);
               if (raw.agency != null) metadata.agency = (Arrays.asList(raw.agency));
               if (raw.members != null) {
                    metadata.members = Arrays.stream(raw.members)
                            .map(Arrays::asList)
                            .collect(Collectors.toList());
                } else { metadata.members = null; }
                if (raw.activity != null) {
                    metadata.activity = Arrays.stream(raw.activity)
                            .map(Arrays::asList)
                            .collect(Collectors.toList());
                } else { metadata.activity = null; }


                metadataListener.onSuccess(metadata);
                isFinished = true;
            } catch(Exception e){
                Log.e(TAG, "Failed to parse metadata", e);
                mainHandler.post(() -> metadataListener.onFailure("Failed to parse metadata: " + e.getMessage()));
            }
        } else{
            Log.d(TAG, "metadataListener is null");
        }

    }

    @JavascriptInterface
    public void reportFailure(String reason) {
        if (isFinished) return;
        isFinished = true;
        Log.w(TAG, "Failure reported from Javascript: " + reason);
        if (metadataListener != null) {
            metadataListener.onFailure(reason);
        }
    }


    static class MetadataRaw{
        String artistNameKr, vibeArtistId, debutDate, biography;
        String[] yearsOfActivity, agency, images;
        String[][] members, activity;
    }
}

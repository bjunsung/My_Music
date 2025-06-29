package com.example.mymusic.network;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ArtistVibeLinkBridge {

    public interface OnLinkListener {
        void onMetadataReceived(List<List<String>> updatedVocalistList);
    }

    private OnLinkListener listener;
    private List<List<String>> vocalistList;

    public void setListener(OnLinkListener listener, List<List<String>> vocalistList) {
        this.listener = listener;
        this.vocalistList = vocalistList;
    }

    @JavascriptInterface
    public void receiveMetadata(String json) {
        Log.d("ArtistVibeBridge", "Parsed JSON: " + json);
        try {
            Gson gson = new Gson();
            LinkRaw raw = gson.fromJson(json, LinkRaw.class);

            if (raw == null || raw.name == null) {
                Log.e("ArtistVibeBridge", "Invalid data received");
                return;
            }

            for (List<String> pair : vocalistList) {
                if (pair.get(0).equals(raw.name)) {
                    pair.set(1, raw.link);  // link may be null
                    break;
                }
            }

            if (listener != null) {
                listener.onMetadataReceived(new ArrayList<>(vocalistList));
            }

        } catch (Exception e) {
            Log.e("ArtistVibeBridge", "Error parsing metadata or updating list", e);
        }
    }

    static class LinkRaw {
        String name;
        String link;
    }
}

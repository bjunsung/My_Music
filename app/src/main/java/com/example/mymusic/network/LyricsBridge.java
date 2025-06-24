package com.example.mymusic.network;

import android.util.Log;
import android.webkit.JavascriptInterface;



public class LyricsBridge {
    public interface onLyricsReceivedListener{
        void onLyricsReceived(String lyrics);
    }

    private onLyricsReceivedListener listener;

    public void setListener(onLyricsReceivedListener listener){
        this.listener = listener;
    }

    @JavascriptInterface
    public void receiveLyrics(String lyricsText){
        Log.d("LyricsBridge", "received: " + lyricsText);
        if (listener != null){
            listener.onLyricsReceived(lyricsText);
        }
    }

}

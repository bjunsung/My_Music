package com.example.mymusic.network;

import android.companion.WifiDeviceFilter;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArtistImageFetchBridge {
    public interface OnImageFetchListener{
        void onSuccess(List<String> images);
        void onFailure(String reasonh);
    }

    private final String TAG = "ArtistImageFetchBridge";
    private OnImageFetchListener imageFetchListener;
    private boolean isFinished = false;

    public void setImageFetchListener(OnImageFetchListener imageFetchListener){
        this.imageFetchListener = imageFetchListener;
        this.isFinished = false;
    }

    @JavascriptInterface
    public void receivedImages(String json){
        if (isFinished || json == null || json.isEmpty()) {
            Log.d(TAG, "skipped duplicated or empty metadata");
            return;
        }

        isFinished = true;
        Log.d(TAG, "Received json: " + json);

        if (imageFetchListener != null){
            try{
                Gson gson = new Gson();
                ImagesRaw raw = gson.fromJson(json, ImagesRaw.class);
                List<String> urls = new ArrayList<>();
                urls = Arrays.asList(raw.imageUrls);
                if (!urls.isEmpty()){
                    imageFetchListener.onSuccess(urls);
                } else{
                    imageFetchListener.onFailure("image not found");
                }
            }catch (Exception e){
                Log.e(TAG, "Failed to parse metadata", e);
                imageFetchListener.onFailure("JSON 파싱 실패: " + e.getMessage());
            }
        }

    }




    static class ImagesRaw{
        String[] imageUrls;
    }

}

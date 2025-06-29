package com.example.mymusic.network;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.mymusic.model.ArtistMetadata;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArtistMetadataBridge {

    public interface OnMetadataListener{
        void onSuccess(ArtistMetadata metadata);
        void onFailure(String reason);
    }

    private final String TAG = "ArtistMetadataBridge";
    private ArtistMetadataBridge.OnMetadataListener listener;
    private boolean isFinished = false; // 중복 호출 방지 플래그

    public void setListener(ArtistMetadataBridge.OnMetadataListener listener) {
        this.listener = listener;
        this.isFinished = false;
    }

    @JavascriptInterface
    public void receiveMetadata(String json){
        if (isFinished || json == null || json.isEmpty()){
            Log.d(TAG, "skipped duplicated or empty metadata");
            return;
        }

        isFinished = true;
        Log.d(TAG, "Received json: " + json);



        if (listener != null){
            try{
                Gson gson = new Gson();
                MetadataRaw raw = gson.fromJson(json, MetadataRaw.class);
                ArtistMetadata metadata = new ArtistMetadata();
                metadata.debutDate = raw.debutDate;
                metadata.yearsOfActivity = Arrays.asList(raw.yearsOfActivity);
                metadata.agency = Arrays.asList(raw.agency);
                metadata.biography = raw.biography;
                metadata.images = Arrays.asList(raw.images);
                if (raw.members != null) {
                    // raw.members (String[][])를 List<List<String>>으로 변환
                    metadata.members = Arrays.stream(raw.members) // String[]의 스트림 생성
                            .map(Arrays::asList)   // 각 String[]를 List<String>으로 변환
                            .collect(Collectors.toList()); // List<List<String>>으로 최종 수집
                } else {
                    metadata.members = null; // raw.members가 null이면 metadata.members도 null로 유지
                }
                if (raw.activity != null) {
                    metadata.activity = Arrays.stream(raw.activity)
                            .map(Arrays::asList)
                            .collect(Collectors.toList());
                } else {
                    metadata.activity = null;
                }

                Log.d(TAG + "-DEBUG", "members raw: " + metadata.members);
                for (List<String> member : metadata.members) {
                    Log.d(TAG + "-DEBUG", "each member: " + member);
                }


                listener.onSuccess(metadata);

            }catch (Exception e){
                Log.e(TAG, "Failed to parse metadata", e);
                listener.onFailure("JSON 파싱 실패: " + e.getMessage());
            }
        }
    }

    //실패 보고
    @JavascriptInterface
    public void reportFailure(String reason) {
        if (isFinished) return;
        isFinished = true;
        Log.w(TAG, "Failure reported from Javascript: " + reason);
        if (listener != null) {
            listener.onFailure(reason);
        }
    }


    static class MetadataRaw {
        String debutDate;
        String[] yearsOfActivity;
        String[] agency;
        String biography;
        String[] images;
        String[][] members;
        String[][] activity;
    }

}

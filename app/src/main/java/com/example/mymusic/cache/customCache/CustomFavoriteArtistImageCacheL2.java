package com.example.mymusic.cache.customCache;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

public class CustomFavoriteArtistImageCacheL2 {
    private static final String TAG = "CustomFavoriteArtistImageCacheL2";
    private static CustomFavoriteArtistImageCacheL2 instance;
    private final LruCache<String, Bitmap> memoryCache;
    private String pinnedKey = null;

    private static int cacheSize;

    private CustomFavoriteArtistImageCacheL2(int cacheSize) {
        CustomFavoriteArtistImageCacheL2.cacheSize = cacheSize;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                return value.getByteCount() / 1024; // 모든 항목의 크기를 1로 설정 → 개수 기준
            }

            @Override
            protected void entryRemoved(boolean evicted, @NonNull String key, @NonNull Bitmap oldValue, @Nullable Bitmap newValue) {
                if (key.equals(pinnedKey)){
                    Log.d(TAG, "Pinned key tried to be evicted. Restoring: " + key);
                    memoryCache.put(key, oldValue);
                }
            }
        };
    }

    public static synchronized CustomFavoriteArtistImageCacheL2 getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CustomFavoriteArtistImageCacheL2 is not initialized. Call init() first.");
        }
        return instance;
    }

    public void put(String key, Bitmap bitmap) {
        if (memoryCache.get(key) == null) {
            Log.d(TAG, "image cache saved, key: " + key);
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap get(String key) {
        return memoryCache.get(key);
    }


    public void remove(String key) {
        memoryCache.remove(key);
    }

    public void clear() {
        memoryCache.evictAll();
    }

    public static synchronized void init(int size) {
        if (instance == null) {
            instance = new CustomFavoriteArtistImageCacheL2(size);
        }
    }

    public int getSize(){
        return memoryCache.size(); //현재 저장된 항목 개수 반환.
    }

    public int getMaxSize() {
        return memoryCache.maxSize(); // 최대 크기
    }

    public void pin(String key){
        Log.d(TAG, "image cache pinned, key: " + key);
        pinnedKey = key;
    }

    public void unpin(String key){
        Log.d(TAG, "image cache unpinned, key: " + key);
        pinnedKey = null;
    }

    public boolean isPinned(String key) {
        return key != null && key.equals(pinnedKey);
    }


}

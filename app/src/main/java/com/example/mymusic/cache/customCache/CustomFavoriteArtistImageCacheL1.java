package com.example.mymusic.cache.customCache;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import java.util.HashSet;
import java.util.Set;

public class CustomFavoriteArtistImageCacheL1 {
    private static final String TAG = "CustomFavoriteArtistImageCacheL1";
    private static CustomFavoriteArtistImageCacheL1 instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final Set<String> pinnedKeys = new HashSet<>();

    private  static final int maxCacheSize = 50*480*480*4/1024; //27MB 아티스트 50명

    private CustomFavoriteArtistImageCacheL1() {
        memoryCache = new LruCache<String, Bitmap>(maxCacheSize) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                return value.getByteCount() / 1024;
            }

            @Override
            protected void entryRemoved(boolean evicted, @NonNull String key, @NonNull Bitmap oldValue, @Nullable Bitmap newValue) {
                if (pinnedKeys.contains(key)){
                    Log.d(TAG, "Pinned key tried to be evicted. Restoring: " + key);
                    memoryCache.put(key, oldValue);
                }
            }

        };
    }

    public static synchronized CustomFavoriteArtistImageCacheL1 getInstance() {
        if (instance == null) {
            instance = new CustomFavoriteArtistImageCacheL1();
        }
        return instance;
    }

    public void put(String key, Bitmap bitmap) {
        if (memoryCache.get(key) == null) {
            Log.d(TAG, "image cache saved to L1 cache, key: " + key);
            memoryCache.put(key, bitmap);
        }else{
            Log.d(TAG, "image cache is already exist " + key);
        }
    }

    public Bitmap get(String key) {
        return memoryCache.get(key);
    }


    public void remove(String key) {
        memoryCache.remove(key);
        Log.d(TAG, "memory cache removed from L1 cache: " + key);
        Log.d(TAG, "current FavoriteArtistImage L1 cache size: " + getSize() + " bytes");
    }

    public void clear() {
        memoryCache.evictAll();
    }


    /**
     * 기존 캐시는 전부 삭제되고 새 용량으로 재생성됩니다.
     */
    public static synchronized void init() {
        if (instance == null) {
            Log.d(TAG, "create CustomFavoriteArtist L1 cache");
            instance = new CustomFavoriteArtistImageCacheL1();
        }else{
            Log.d(TAG, "CustomFavoriteArtist L1 cache is already exist");
        }
    }


    public int getSize(){
        return memoryCache.size();
    }


    public int getMaxSize() {
        return memoryCache.maxSize();
    }


    public void pin(String key){
        Log.d(TAG, "image cache pinned, key: " + key);
        pinnedKeys.add(key);
    }

    public void unpin(String key){
        Log.d(TAG, "image cache unpinned, key: " + key);
        pinnedKeys.remove(key);
    }

    public boolean isPinned(String key){
        return pinnedKeys.contains(key);
    }

}

package com.example.mymusic.cache.customCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Build;
import android.util.Base64;
import android.util.Log;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import java.util.Map;


public class CustomFavoriteArtistImageDiskCacheL3 {
    private static final String TAG = "CustomFavoriteArtistImageDiskCacheL3";
    private static CustomFavoriteArtistImageDiskCacheL3 instance;

    private static final long MAX_CACHE_SIZE_BYTES = 90L * 1024 * 1024; //90MB
    private static final String CACHE_DIR_NAME = "custom_favorite_artist_image_disk_cache";

    private final File cacheDir;

    private CustomFavoriteArtistImageDiskCacheL3(Context context) {
        cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        if (!cacheDir.exists()){
            if (!cacheDir.mkdirs()) {
                Log.e(TAG, "Failed to create cache directory: " + cacheDir.getAbsolutePath());
            }
        }
    }

    public static synchronized CustomFavoriteArtistImageDiskCacheL3 getInstance(Context context){
        if (instance == null){
            instance = new CustomFavoriteArtistImageDiskCacheL3(context);
        }
        return instance;
    }

    public void put(String key, Bitmap bitmap){
        File file = new File(cacheDir, keyToFilename(key));
        if (file.exists()) return;

        try (FileOutputStream out = new FileOutputStream((file))) {
            boolean success;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                success = bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out);
            } else{
                success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            out.flush();
            enforceCacheLimit();
            if (!success) {
                Log.w(TAG, "Bitmap compression failed for key: " + key);
            } else {
                Log.d(TAG, "Saved to disk cache: " + file.getName());
            }
        } catch (IOException e){
            Log.e(TAG, "Error writing to disk ", e);
        }
    }

    public Bitmap get(String key){
        File file = new File(cacheDir, keyToFilename(key));
        if (file.exists()){
            if (!file.setLastModified(System.currentTimeMillis())) {
                Log.w(TAG, "Failed to update lastModified for: " + file.getAbsolutePath());
            }
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    public Map<String, Bitmap> getAllEntries(Context context){
        Map<String, Bitmap> result = new HashMap<>();
        File[] files = cacheDir.listFiles();

        if (files == null) return result;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".webp")) {
                String key = filenameToKey(file.getName()); // 키 복원
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    result.put(key, bitmap);
                }
            }
        }
        return result;
    }

    public void remove(String key){
        File file = new File(cacheDir, keyToFilename(key));
        if (file.exists()){
            if (!file.delete()) {
                Log.w(TAG, "Failed to delete file: " + file.getAbsolutePath());
            }
            Log.d(TAG, "Disk cache removed from L3 cache, key " + key  + " name: " + file.getName());
            Log.d(TAG, "current FavoriteArtistImage L3 cache size: " + getCacheSizeBytes() + " bytes");
        }
    }

    public void clear(){
        File[] files = cacheDir.listFiles();
        if (files != null){
            for (File f : files){
                if (!f.delete()) {
                    Log.w(TAG, "Failed to delete file: " + f.getAbsolutePath());
                }
            }
        }
        Log.d(TAG, "Disk cache cleared.");
    }

    public boolean contains(String key){
        File file = new File(cacheDir, keyToFilename(key));
        return file.exists();
    }



    private String keyToFilename(String key){
        return Base64.encodeToString(key.getBytes(), Base64.NO_WRAP) + ".webp";
    }

    private String filenameToKey(String filename){
        return new String(Base64.decode(filename.replace(".webp", ""), Base64.NO_WRAP));
    }
    private void enforceCacheLimit(){
        File[] files = cacheDir.listFiles();
        if (files == null) return;

        long totalSize = 0;
        for (File f : files){
            totalSize += f.length();
        }

        if (totalSize > MAX_CACHE_SIZE_BYTES) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (File file : files){
                totalSize -= file.length();
                file.delete();
                if (totalSize <= MAX_CACHE_SIZE_BYTES) break;
            }
            Log.d(TAG, "Disk cache trimmed to fit limit.");
        }
    }


    public long getCacheSizeBytes(){
        File[] files = cacheDir.listFiles();
        if (files == null) return 0;
        long total = 0;
        for (File f : files) total += f.length();
        return total;
    }


}









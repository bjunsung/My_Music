package com.example.mymusic.cache;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL1;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL2;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageDiskCacheL3;

import java.io.File;

public class CacheUtil {

    public static void celarAllCaches(Context context){
        clearGlideCache(context);
        clearAllCustomCache(context);
        clearAppDiskCache(context);
        clearWebView(context);
    }
    public static void clearAllCustomCache(Context context){
        CustomFavoriteArtistImageDiskCacheL3.getInstance(context).clear();
        CustomFavoriteArtistImageCacheL1.getInstance().clear();
        CustomFavoriteArtistImageCacheL2.getInstance().clear();
        clearGlideCache(context);
    }

    public static void clearMemoryCache(Context context){
        CustomFavoriteArtistImageCacheL1.getInstance().clear();
        CustomFavoriteArtistImageCacheL2.getInstance().clear();
        Glide.get(context).clearMemory();
    }

    public static void clearDiskCache(Context context){
        CustomFavoriteArtistImageDiskCacheL3.getInstance(context).clear();
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
        File extCache = context.getExternalCacheDir();
        if (extCache != null) deleteDir(extCache);
        clearAppDiskCache(context);
    }

    public static void clearGlideCache(Context context){
        Glide.get(context).clearMemory();
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
        File extCache = context.getExternalCacheDir();
        if (extCache != null) deleteDir(extCache);

    }

    public static void clearAppDiskCache(Context context){
        try {
            File dir = context.getCacheDir();
            new Thread(() -> deleteDir(dir)).start();
        }catch (Exception e){
            e.printStackTrace();;
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private static void clearWebView(Context context){
        try {
            File webviewCache = new File(context.getCacheDir().getParent(), "app_webview");
            deleteDir(webviewCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

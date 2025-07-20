package com.example.mymusic.cache.reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL1;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageDiskCacheL3;
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.util.SortFilterArtistUtil;

import java.util.List;

public class CustomFavoriteArtistImageReader {

    private final static String TAG = "CustomFavoriteArtistImageReader";

    public static Bitmap get(Context context, String key) {
        // 1. L1 (메모리) 캐시 확인
        Bitmap bitmap = CustomFavoriteArtistImageCacheL1.getInstance().get(key);
        if (bitmap != null) {
            Log.d(TAG, "Image fetch Hit from L1 Memory cache");
            return bitmap;
        }

        // 2. L3 (디스크) 캐시 확인
        bitmap = CustomFavoriteArtistImageDiskCacheL3.getInstance(context).get(key);
        if (bitmap != null) {
            // 3. 디스크에서 가져왔으면 L1에도 추가
            Log.d(TAG, "Image fetch Hit from L3 Disk cache");
            CustomFavoriteArtistImageCacheL1.getInstance().put(key, bitmap);
        }

        return bitmap;
    }

    public static Bitmap getL1Cache(Context context, String key){
        Bitmap bitmap = CustomFavoriteArtistImageCacheL1.getInstance().get(key);
        if (bitmap == null){
            Log.d(TAG, "Image fetch Miss from L1 Memory cache");
            return null;
        }
        Log.d(TAG, "Image fetch Hit from L1 Memory cache");
        return bitmap;
    }

    public static Bitmap getL3Cache(Context context, String key){
        Bitmap bitmap = CustomFavoriteArtistImageDiskCacheL3.getInstance(context).get(key);
        if (bitmap == null){
            Log.d(TAG, "Image fetch Miss from L3 Disk cache");
            return null;
        }
        Log.d(TAG, "Image fetch Hit from L3 Disk cache");
        return bitmap;
    }

}





    /*

    public static void fetchFavoriteArtistCache(Context context){
        boolean successToFetchDiskCache = CustomFavoriteArtistImageWriter.saveRepresentativeImageFromL3DiskCacheToL1Cache(context);

        if (successToFetchDiskCache) {
            Log.d(TAG, "success to load L3 disk cache and store to L1 memory cache");
        } else{
            Log.d(TAG, "fail to load L3 disk cache, start to load FavoriteArtist List from room db and store to L1, L3 cache");
            FavoriteArtistReader.loadFavoritesOriginalForm(context, favoriteArtistList -> {
                List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(context, favoriteArtistList);
                CustomFavoriteArtistImageWriter.saveRepresentativeImages(context, filtered);
            });
        }
    }

     */


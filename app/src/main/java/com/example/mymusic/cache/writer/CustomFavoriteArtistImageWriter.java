package com.example.mymusic.cache.writer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL1;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageDiskCacheL3;
import com.example.mymusic.cache.reader.CustomFavoriteArtistImageReader;
import com.example.mymusic.model.FavoriteArtist;

import java.util.List;
import java.util.Map;

public class CustomFavoriteArtistImageWriter {
    private static final int L1_CACHE = 1;
    private static final int L3_CACHE = 3;
    public static void removeUrls(Context context, List<String> selectedImageUrls) {
        for (String url : selectedImageUrls){
            if (url != null){
                removeUrl(context, url);
            }
        }
    }

    public static interface BitmapCallback{
        void onBitmapReady(Bitmap bitmap, int cacheLevel);
        void onError(String reason);
    }
    private static final String TAG = "CustomFavoriteArtistImageWriter";
    public static void saveRepresentativeImagesByFavoriteArtistList(Context context, List<FavoriteArtist> favoriteArtistList, int overrideWidth, int overrideHeight){
        if (favoriteArtistList == null) return;
        for (FavoriteArtist item : favoriteArtistList){
            if (item == null || item.artist == null || item.artist.artworkUrl == null || item.artist.artworkUrl.isEmpty())
                continue;
            String artworkUrl = item.artist.artworkUrl;
            storeImageByUrlLink(context, artworkUrl, overrideWidth, overrideHeight);
        }
    }

    public static boolean saveRepresentativeImageFromL3DiskCacheToL1Cache(Context context){
        Map<String, Bitmap> diskCacheEntries = CustomFavoriteArtistImageDiskCacheL3.getInstance(context).getAllEntries(context);
        if (diskCacheEntries == null || diskCacheEntries.isEmpty()) return false;
        for (Map.Entry<String, Bitmap> entry : diskCacheEntries.entrySet()){
            String key = entry.getKey();
            Bitmap bitmap = entry.getValue();
            Log.d(TAG, "cache entry, key: " + key + " value: " + bitmap);
            if (bitmap == null){
                Log.w(TAG, "bitmap is null, skip to storing (key: " + key + ")");
            }
            else if (CustomFavoriteArtistImageCacheL1.getInstance().get(key) != null) {
                Log.d(TAG, "already exist in L1 memory cache and skip to storing (key: " + key + ")");
            }
            else{
                CustomFavoriteArtistImageCacheL1.getInstance().put(key, bitmap);
                Log.d(TAG, "load cache entry from L3 disk and store to L1 memory completed (key: " + key + ")");
            }
        }
        return true;
    }


    public static void storeImageByBitmap(Context context, String artworkUrl, Bitmap bitmap, int overrideWidth, int overrideHeight){
        if (bitmap == null){
            Log.d(TAG, "empty bitmap received");
            return;
        }
        Bitmap cachedL1 = CustomFavoriteArtistImageReader.getL1Cache(context, artworkUrl);
        if (cachedL1 == null){
            CustomFavoriteArtistImageCacheL1.getInstance().put(artworkUrl, bitmap);
            Log.d(TAG, "onBitmapReady and now save bitmap to L1 cache");
        }
        Bitmap cachedL3 = CustomFavoriteArtistImageReader.getL3Cache(context, artworkUrl);
        if (cachedL3 == null){
            CustomFavoriteArtistImageDiskCacheL3.getInstance(context).put(artworkUrl, bitmap);
            Log.d(TAG, "onBitmapReady and now save bitmap to L3 cache");
            Log.d(TAG, "current L3 Cache Size: " + CustomFavoriteArtistImageDiskCacheL3.getInstance(context).getCacheSizeBytes());
        }
    }
    public static void storeImageByUrlLink(Context context, String artworkUrl, int overrideWidth, int overrideHeight){
        Bitmap cachedL1 = CustomFavoriteArtistImageReader.getL1Cache(context, artworkUrl);
        Bitmap cachedL3 = CustomFavoriteArtistImageReader.getL3Cache(context, artworkUrl);

        if (cachedL1 != null && cachedL3 != null){
            Log.d(TAG, "already exist in L1 cache & L3 cache, skip to storing");
            return;
        }

        getBitmapByUrl(context, artworkUrl, overrideWidth, overrideHeight, new BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap, int cacheLevel) {
                if (cacheLevel == CustomFavoriteArtistImageWriter.L1_CACHE) {
                    CustomFavoriteArtistImageCacheL1.getInstance().put(artworkUrl, bitmap);
                    Log.d(TAG, "onBitmapReady and now save bitmap to L1 cache");
                }
                else if (cacheLevel == CustomFavoriteArtistImageWriter.L3_CACHE) {
                    CustomFavoriteArtistImageDiskCacheL3.getInstance(context).put(artworkUrl, bitmap);
                    Log.d(TAG, "onBitmapReady and now save bitmap to L3 cache");
                }
            }

            @Override
            public void onError(String reason) {
                Log.e(TAG, "Failed to load image for: " + artworkUrl + " reason: " + reason);
            }
        });
    }

    public static void removeUrl(Context context, String key){
        CustomFavoriteArtistImageCacheL1.getInstance().remove(key);
        CustomFavoriteArtistImageDiskCacheL3.getInstance(context).remove(key);
    }

    private static void getBitmapByUrl(Context context, String artworkUrl, int overrideWidth, int overrideHeight, BitmapCallback bitmapCallback){
        Glide.with(context)
                .asBitmap()
                .load(artworkUrl)
                .override(overrideWidth, overrideHeight)
                .centerCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        if (CustomFavoriteArtistImageCacheL1.getInstance().get(artworkUrl) == null) {
                            bitmapCallback.onBitmapReady(resource, CustomFavoriteArtistImageWriter.L1_CACHE);
                            Log.d(TAG, "current L1 Cache Size: " + CustomFavoriteArtistImageCacheL1.getInstance().getSize());
                        }

                        if(!CustomFavoriteArtistImageDiskCacheL3.getInstance(context).contains(artworkUrl)){
                            bitmapCallback.onBitmapReady(resource, CustomFavoriteArtistImageWriter.L3_CACHE);
                            Log.d(TAG, "current L3 Cache Size: " + CustomFavoriteArtistImageDiskCacheL3.getInstance(context).getCacheSizeBytes());
                        }

                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        bitmapCallback.onError("Failed to load image for: " + artworkUrl);
                    }
                });
    }
}

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
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;

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
    public static void saveRepresentativeImages(Context context, List<FavoriteArtist> favoriteArtistList){
        if (favoriteArtistList == null) return;
        for (FavoriteArtist item : favoriteArtistList){
            if (item == null || item.artist == null || item.artist.artworkUrl == null || item.artist.artworkUrl.isEmpty())
                continue;
            String artworkUrl = item.artist.artworkUrl;
            saveRepresentativeImage(context, artworkUrl);
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


    public static void saveRepresentativeImage(Context context, String artworkUrl){
        getBitmapByUrl(context, artworkUrl, new BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap, int cacheLevel) {
                Log.d(TAG, "onBitmapReady and now save bitmap to L1 cache and L3 cache");
                if (cacheLevel == CustomFavoriteArtistImageWriter.L1_CACHE) {
                    CustomFavoriteArtistImageCacheL1.getInstance().put(artworkUrl, bitmap);
                }
                else if (cacheLevel == CustomFavoriteArtistImageWriter.L3_CACHE) {
                    CustomFavoriteArtistImageDiskCacheL3.getInstance(context).put(artworkUrl, bitmap);
                }
            }

            @Override
            public void onError(String reason) {}
        });
    }

    public static void removeUrl(Context context, String key){
        CustomFavoriteArtistImageCacheL1.getInstance().remove(key);
        CustomFavoriteArtistImageDiskCacheL3.getInstance(context).remove(key);
    }

    private static void getBitmapByUrl(Context context, String artworkUrl, BitmapCallback bitmapCallback){
        Glide.with(context)
                .asBitmap()
                .load(artworkUrl)
                .override(ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE)
                .centerCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        if (CustomFavoriteArtistImageCacheL1.getInstance().get(artworkUrl) != null) {
                            Log.d(TAG, "Image already cached in L1 Memory cache: " + artworkUrl);
                            bitmapCallback.onError("Image already cached in L1 Memory cache: " + artworkUrl);
                            return;
                        } else{
                            bitmapCallback.onBitmapReady(resource, CustomFavoriteArtistImageWriter.L1_CACHE);
                            Log.d(TAG, "current L1 Cache Size: " + CustomFavoriteArtistImageCacheL1.getInstance().getSize());
                        }

                        if(CustomFavoriteArtistImageDiskCacheL3.getInstance(context).contains(artworkUrl)){
                            Log.d(TAG, "Image already cached in L3 Disk cache: " + artworkUrl);
                            bitmapCallback.onError("Image already cached in L3 Disk cache: " + artworkUrl);
                            return;
                        } else{
                            bitmapCallback.onBitmapReady(resource, CustomFavoriteArtistImageWriter.L3_CACHE);
                            Log.d(TAG, "current L3 Cache Size: " + CustomFavoriteArtistImageDiskCacheL3.getInstance(context).getCacheSizeBytes());
                        }

                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        bitmapCallback.onError("Failed to load image for: " + artworkUrl);
                        Log.e(TAG, "Failed to load image for: " + artworkUrl);
                    }
                });
    }
}

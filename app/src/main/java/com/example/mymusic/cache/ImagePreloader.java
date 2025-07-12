package com.example.mymusic.cache;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.MyMusicApplication;
import com.example.mymusic.R;
import com.example.mymusic.data.repository.ArtistMetadataRepository;
import com.example.mymusic.data.repository.FavoriteArtistRepository;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.util.SortFilterArtistUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImagePreloader {
    private static final String TAG = "ImagePreloader";

    public static void preloadRepresentativeFavoriteArtistImageWithDataLoad(Context context, int widthSize, int heightSize){
        loadFavoritesOriginalForm(context, favoriteArtistList -> {
            List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(context, favoriteArtistList);
            for (FavoriteArtist item : filtered){
                if (item != null && item.artist != null && item.artist.artworkUrl != null && !item.artist.artworkUrl.isEmpty()){
                    preloadByImageSize(context, item.artist.artworkUrl, widthSize, heightSize);
                }
            }
        });
    }

    public static void preloadRepresentativeFavoriteArtistImageWithDataLoad(Context context, List<List<Integer>> sizeList){
        loadFavoritesOriginalForm(context, favoriteArtistList -> {
            List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(context, favoriteArtistList);
            for (List<Integer> pair : sizeList){
                for (FavoriteArtist item : filtered){
                    if (item != null && item.artist != null && item.artist.artworkUrl != null && !item.artist.artworkUrl.isEmpty()){
                        preloadByImageSize(context, item.artist.artworkUrl, pair.get(0), pair.get(1));
                    }
                }
            }
        });
    }


    public static void preloadRepresentativeFavoriteArtistImage(Context context, List<FavoriteArtist> favoriteArtistList, List<List<Integer>> sizeList){
        for (List<Integer> pair : sizeList) {
            for (FavoriteArtist item : favoriteArtistList) {
                if (item != null && item.artist != null && item.artist.artworkUrl != null && !item.artist.artworkUrl.isEmpty()) {
                    preloadByImageSize(context, item.artist.artworkUrl, pair.get(0), pair.get(1));
                }
            }
        }
    }

    public static void preloadRepresentativeFavoriteArtistImage(Context context, List<FavoriteArtist> favoriteArtistList, int widthSize, int heightSize){
        for (FavoriteArtist item : favoriteArtistList){
            if (item != null && item.artist != null && item.artist.artworkUrl != null && !item.artist.artworkUrl.isEmpty()) {
                preloadByImageSize(context, item.artist.artworkUrl, widthSize, heightSize);
            }
        }
    }



    public static void preloadByImageSize(Context context, String imageUrl, int widthSize, int heightSize){
        Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .override(widthSize, heightSize)
                .centerCrop()
                .error(R.drawable.ic_image_not_found_foreground)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Log.d(TAG, "Preload complete for: " + imageUrl + " Size: " + widthSize + " X " + heightSize);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Log.e(TAG, "Failed to preload: " + imageUrl);
                    }
                });

    }

    public static void preloadByImageSize(Context context, List<String> imageUrls, int widthSize, int heightSize){
        for (String url : imageUrls){
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(widthSize, heightSize)
                    .centerCrop()
                    .error(R.drawable.ic_image_not_found_foreground)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            Log.d(TAG, "Preload complete for: " + url + " Size: " + widthSize + " X " + heightSize);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) { }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            Log.e(TAG, "Failed to preload: " + url);
                        }
                    });
        }
    }
    private static void loadFavoritesOriginalForm(Context context, Consumer<List<FavoriteArtist>> callback){

        FavoriteArtistRepository favoriteArtistRepository = new FavoriteArtistRepository(context);

        ArtistMetadataRepository artistMetadataRepository = new ArtistMetadataRepository(context);

        new Thread(() -> {
            List<Artist> artists = favoriteArtistRepository.getAllFavoriteArtist();
            List<com.example.mymusic.model.FavoriteArtist> result = new ArrayList<>();
            for (Artist artist : artists){
                com.example.mymusic.data.local.FavoriteArtist favoriteArtist = favoriteArtistRepository.getFavoriteArtist(artist.artistId);
                String addedDate = "";
                if (favoriteArtist != null && favoriteArtist.addedDate != null) {
                    addedDate = favoriteArtist.addedDate;
                }
                ArtistMetadata metadata = artistMetadataRepository.getArtistMetadataBySpotifyId(artist.artistId);
                if (addedDate.equals("")) addedDate = null;
                result.add(new com.example.mymusic.model.FavoriteArtist(artist, addedDate, metadata));
            }
            if (result.isEmpty()) new Handler(Looper.getMainLooper()).post(() -> callback.accept(null));
            else new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
        }).start();
    }

}

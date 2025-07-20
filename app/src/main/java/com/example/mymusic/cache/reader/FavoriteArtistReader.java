package com.example.mymusic.cache.reader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.mymusic.data.repository.ArtistMetadataRepository;
import com.example.mymusic.data.repository.FavoriteArtistRepository;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FavoriteArtistReader {
    public static void loadFavoritesOriginalForm(Context context, Consumer<List<FavoriteArtist>> callback){

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

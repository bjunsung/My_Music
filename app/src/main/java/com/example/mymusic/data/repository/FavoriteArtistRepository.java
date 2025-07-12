package com.example.mymusic.data.repository;

import android.content.Context;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.FavoriteArtist;
import com.example.mymusic.data.local.FavoriteArtistDao;
import com.example.mymusic.model.Artist;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteArtistRepository {
    private final FavoriteArtistDao favoriteArtistDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public FavoriteArtistRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        favoriteArtistDao = db.favoriteArtistDao();
    }

    public long saveFavoriteArtist(Artist artist, String addedDate){
        FavoriteArtist favoriteArtist = new FavoriteArtist(artist, addedDate);
        return favoriteArtistDao.saveFavoriteArtist(favoriteArtist);
    }
    public int deleteFavoriteArtist(String artistId){
        FavoriteArtist favoriteArtist = favoriteArtistDao.getFavoriteArtist(artistId);
        if (favoriteArtist == null) return -1;
        String artistName = favoriteArtist.artistName;
        return favoriteArtistDao.deleteFavoriteArtist(artistId);
    }

    public int getFavoriteArtistCount(){
        return favoriteArtistDao.getFavoriteArtistCount();
    }

    public List<Artist> getAllFavoriteArtist(){
        List<FavoriteArtist> favoriteArtists = favoriteArtistDao.getAllFavoriteArtists();
        List<Artist> artists = new ArrayList<>();
        for(FavoriteArtist favoriteArtist : favoriteArtists){
            artists.add(new Artist(
                    favoriteArtist.artistId,
                    favoriteArtist.artistName,
                    favoriteArtist.artworkUrl,
                    favoriteArtist.genres,
                    favoriteArtist.followers,
                    favoriteArtist.popularity
            ));
        }
        return artists;
    }

    public int deleteFavoriteArtistsByIds(List<String> artistIds){
        return favoriteArtistDao.deleteFavoriteArtistsByIds(artistIds);
    }

    public String getAddedDate(String artistId){
        FavoriteArtist artist = favoriteArtistDao.getFavoriteArtist(artistId);
        if (artist == null) return null;
        return artist.addedDate;
    }

    public FavoriteArtist getFavoriteArtist(String artistId){
        return favoriteArtistDao.getFavoriteArtist(artistId);
    }



}

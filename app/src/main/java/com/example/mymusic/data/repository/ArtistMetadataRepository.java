package com.example.mymusic.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.ArtistMetadata;
import com.example.mymusic.data.local.ArtistMetadataDao;

import java.util.function.Consumer;

public class ArtistMetadataRepository {
    private final ArtistMetadataDao artistMetadataDao;
    public ArtistMetadataRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        artistMetadataDao = db.artistMetadataDao();
    }


    public void addArtistMetadata(com.example.mymusic.model.ArtistMetadata artistMetadata){
        com.example.mymusic.data.local.ArtistMetadata metadataDb = new com.example.mymusic.data.local.ArtistMetadata(
                artistMetadata.vibeArtistId,
                artistMetadata.spotifyArtistId,
                artistMetadata.debutDate,
                artistMetadata.yearsOfActivity,
                artistMetadata.agency,
                artistMetadata.biography,
                artistMetadata.images,
                artistMetadata.members,
                artistMetadata.activity
        );
        artistMetadataDao.saveArtistMetadata(metadataDb);
    }

    public int removeArtistMetadata(String vibeId){
        return artistMetadataDao.removeArtistMetadata(vibeId);
    }

    public int removeArtistMetadataBySpotifyId(String spotifyId){
        Log.d("ArtistMetadataRepository", "removeArtistMetadataBySpotifyId, id : " + spotifyId);
        return artistMetadataDao.removeArtistMetadataBySpotifyId(spotifyId);
    }

    public com.example.mymusic.model.ArtistMetadata getArtistMetadata(String vibeId){
        ArtistMetadata metadata = artistMetadataDao.getArtistMetadata(vibeId);
        if (metadata == null){
            return null;
        }
        return new com.example.mymusic.model.ArtistMetadata(
                metadata.vibeArtistId,
                metadata.spotifyArtistId,
                metadata.debutDate,
                metadata.yearsOfActivity,
                metadata.agency,
                metadata.biography,
                metadata.images,
                metadata.members,
                metadata.activity);
    }

    public com.example.mymusic.model.ArtistMetadata getArtistMetadataBySpotifyId(String spotifyId){
        ArtistMetadata metadata = artistMetadataDao.getArtistMetadataBySpotifyId(spotifyId);
        if (metadata != null)
            return new com.example.mymusic.model.ArtistMetadata(
                metadata.vibeArtistId,
                metadata.spotifyArtistId,
                metadata.debutDate,
                metadata.yearsOfActivity,
                metadata.agency,
                metadata.biography,
                metadata.images,
                metadata.members,
                metadata.activity);
        else
            return null;
    }



    public void updateArtistMetadata(com.example.mymusic.model.ArtistMetadata artistMetadata, Consumer<Boolean> callback){
        com.example.mymusic.data.local.ArtistMetadata metadataDb = new com.example.mymusic.data.local.ArtistMetadata(
                artistMetadata.vibeArtistId,
                artistMetadata.spotifyArtistId,
                artistMetadata.debutDate,
                artistMetadata.yearsOfActivity,
                artistMetadata.agency,
                artistMetadata.biography,
                artistMetadata.images,
                artistMetadata.members,
                artistMetadata.activity
        );
        int result = artistMetadataDao.updateArtistMetadata(metadataDb);
        callback.accept(result > 0);
    }



}

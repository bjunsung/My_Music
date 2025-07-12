package com.example.mymusic.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.mymusic.data.local.AppDatabase;
import com.example.mymusic.data.local.ArtistMetadata;
import com.example.mymusic.data.local.ArtistMetadataDao;

import java.util.ArrayList;
import java.util.List;
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
                artistMetadata.artistNameKr,
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
                metadata.artistNameKr,
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
                    metadata.artistNameKr,
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
                artistMetadata.artistNameKr,
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

    public long updateImagesBySpotifyId(String spotifyId, List<String> newImages) {
        ArtistMetadata existingMetadata = artistMetadataDao.getArtistMetadataBySpotifyId(spotifyId);

        if (existingMetadata != null) {
            // 순서 유지하며 중복 없이 합치기
            List<String> currentImages = existingMetadata.images != null
                    ? new ArrayList<>(existingMetadata.images)
                    : new ArrayList<>();

            for (String url : newImages) {
                if (!currentImages.contains(url)) {
                    currentImages.add(url); // 순서 유지하면서 추가
                }
            }

            // 업데이트 후 저장
            existingMetadata.images = currentImages;
            long result = artistMetadataDao.saveArtistMetadata(existingMetadata); // REPLACE 전략으로 저장
            return result;
        }
        return 0;
    }


    public long setRepresentativeImage(String vibeId, int position) {
        ArtistMetadata existingMetadata = artistMetadataDao.getArtistMetadata(vibeId);
        if (existingMetadata == null || existingMetadata.images == null || existingMetadata.images.isEmpty()) {
            return 0;  // 실패
        }

        List<String> originalImages = existingMetadata.images;

        if (position < 0 || position >= originalImages.size()) {
            return 0;  // 유효하지 않은 인덱스
        }

        String selectedImage = originalImages.get(position);
        List<String> reorderedImages = new ArrayList<>();
        reorderedImages.add(selectedImage);

        for (int i = 0; i < originalImages.size(); i++) {
            if (i == position) continue;
            reorderedImages.add(originalImages.get(i));
        }

        existingMetadata.images = reorderedImages;

        // 저장 (업데이트)
        return artistMetadataDao.updateArtistMetadata(existingMetadata);
    }


}

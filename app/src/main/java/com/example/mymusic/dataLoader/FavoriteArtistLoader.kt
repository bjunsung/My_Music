package com.example.mymusic.dataLoader

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.mymusic.data.repository.ArtistMetadataRepository
import com.example.mymusic.data.repository.FavoriteArtistRepository
import com.example.mymusic.model.Artist
import com.example.mymusic.model.ArtistMetadata
import com.example.mymusic.model.FavoriteArtist


class FavoriteArtistLoader {
    companion object {
        public interface OnLoadListener {
            fun onLoadSuccess (fav: FavoriteArtist)
            fun onLoadFailed ()
        }
        public fun loadFavoriteArtistById(context: Context, id: String, listener: OnLoadListener) {
            val favArtistRepository = FavoriteArtistRepository(context)
            val artistMetadataRepository = ArtistMetadataRepository(context)
            Thread {
                val loaded: com.example.mymusic.data.local.FavoriteArtist? = favArtistRepository.getFavoriteArtist(id)
                if (loaded == null) {
                    Handler(Looper.getMainLooper()).post {
                        listener.onLoadFailed()
                    }
                }
                else {
                    val artist = Artist(loaded.artistId, loaded.artistName, loaded.artworkUrl, loaded.genres, loaded.followers, loaded.popularity)
                    val addedDate: String = favArtistRepository.getAddedDate(id)
                    val metadata: ArtistMetadata =
                        artistMetadataRepository.getArtistMetadataBySpotifyId(id)
                    val result = FavoriteArtist(artist, addedDate, metadata)
                    Handler(Looper.getMainLooper()).post {
                        listener.onLoadSuccess(result)
                    }

                }
            }.start()
        }
    }
}
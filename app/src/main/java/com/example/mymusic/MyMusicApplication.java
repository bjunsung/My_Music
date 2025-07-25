package com.example.mymusic;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL1;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL2;
import com.example.mymusic.cache.reader.FavoriteArtistReader;
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter;
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.network.TokenHelper;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;
import com.example.mymusic.util.SortFilterArtistUtil;

import java.util.List;

public class MyMusicApplication extends Application {
    private final static String TAG = "MyMusicApplication";
    @Override
    public void onCreate() {
        super.onCreate();
        refreshToken();
        /**
         * Custom L1 L3 캐시 데이터 삭제 메소드 (테스트용, 기본 주석처리)
         */
        //Glide.get(this).clearMemory();
        //new Thread(() -> Glide.get(this).clearDiskCache()).start();

        customCacheSetting(this);
        setCustomCache();

    }
    private void customCacheSetting(Context context) {
        /**
         * Custom L1 L3 캐시 데이터 삭제 메소드 (테스트용, 기본 주석처리)
         */
        // CustomFavoriteArtistImageCacheL1.getInstance().clear();
        // CustomFavoriteArtistImageDiskCacheL3.getInstance(viewGroupContext).clear();

        boolean successToFetchDiskCache =
                CustomFavoriteArtistImageWriter.saveRepresentativeImageFromL3DiskCacheToL1Cache(
                        context
                );

        if (successToFetchDiskCache) {
            Log.d(TAG, "success to load L3 disk cache and store to L1 memory cache");
        } else {
            Log.d(
                    TAG,
                    "fail to load L3 disk cache, start to load FavoriteArtist List from room db and store to L1, L3 cache"
            );

            FavoriteArtistReader.loadFavoritesOriginalForm(
                    context,
                    favoriteArtistList -> {
                        List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(
                                context,
                                favoriteArtistList
                        );

                        CustomFavoriteArtistImageWriter.saveRepresentativeImagesByFavoriteArtistList(
                                context,
                                filtered,
                                ArtistInfoFragment.ARTIST_ARTWORK_SIZE,
                                ArtistInfoFragment.ARTIST_ARTWORK_SIZE
                        );
                    }
            );
        }
    }



    private void setCustomCache(){
        CustomFavoriteArtistImageCacheL1.init();

        int cacheSize = 5 * (ArtistInfoFragment.ARTIST_ARTWORK_SIZE * ArtistInfoFragment.ARTIST_ARTWORK_SIZE * 4 / 1024);

        CustomFavoriteArtistImageCacheL2.init(cacheSize);
    }
    private void refreshToken(){
        // Repository 초기화
        TokenRepository tokenRepository = new TokenRepository(getApplicationContext());

        //accessToken 발급
        TokenHelper.getAccessToken(getApplicationContext(), new TokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String accessToken) {
                tokenRepository.setAccessToken(accessToken);
                Log.d("MyMusicApp", "Access token 저장 완료: " + accessToken);
            }

            @Override
            public void onFailure(String error) {
                Log.e("MyMusicApp", "Access token 발급 실패: " + error);
            }
        });
    }


}

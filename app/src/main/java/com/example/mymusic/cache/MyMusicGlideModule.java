package com.example.mymusic.cache;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.module.AppGlideModule;


import java.io.File;


@GlideModule
public class MyMusicGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        int diskCacheSizeBytes = 200 * 1024 * 1024; // 200MB

        builder.setDiskCache(
                new DiskLruCacheFactory(
                        () -> new File(context.getCacheDir(), "glide_cache"), // 캐시 폴더 직접 지정
                        diskCacheSizeBytes
                )
        );
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

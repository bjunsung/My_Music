package com.example.mymusic;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

// 새로운 AppGlideModule 구현. 내용은 비워두어도 됩니다.
@GlideModule
public final class MyMusicGlideModule extends AppGlideModule {
    // 이 클래스는 Glide의 전역 설정을 변경하고 싶을 때 사용합니다.
    // 예를 들어, 기본 RequestOptions를 설정하거나 디스크 캐시 크기를 조절할 수 있습니다.
    // 지금은 비워두어도 에러가 해결됩니다.
}
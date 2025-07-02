package com.example.mymusic.util;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mymusic.R; // 자신의 R 클래스 경로
import com.example.mymusic.animation.StrokePulseView;
import com.example.mymusic.util.ImageSaveUtil; // 자신의 유틸 경로



/*
  <!-- ✅ 화면 디밍용 오버레이 -->
    <FrameLayout
        android:id="@+id/overlay_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone">

        <View
            android:id="@+id/dim_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="#99000000"
            android:clickable="true"
            android:focusable="true" />

        <ImageView
            android:id="@+id/overlay_image_clone"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:scaleType="centerCrop" />

        <TextView
            android:id="@+id/download_button_overlay"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="DOWNLOAD"
            android:textColor="@android:color/white"
            android:padding="6dp"
            android:elevation="4dp" />
    </FrameLayout>
 */




public class ImageOverlayManager {
    private Activity activity;
    private FrameLayout overlayContainer;
    private ImageView overlayImageClone;
    private TextView downloadButton;
    private View dimView;
    private Drawable originalBottomNavBackground;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavView;
    private double scale = 1.025f;
    private int downloadButton_delta_x = 0;
    private int downloadButton_delta_y = 0;
    private int primaryColor = 0;
    private int selectedColor = 0;
    private int unselectedColor = 0;
    private StrokePulseView strokePulseView;
    public ImageOverlayManager (Activity activity, View rootView){
        this.activity = activity;
        overlayContainer = rootView.findViewById(R.id.overlay_container);
        overlayImageClone = rootView.findViewById(R.id.overlay_image_clone);
        downloadButton = rootView.findViewById(R.id.download_button_overlay);
        dimView = rootView.findViewById(R.id.dim_view);
        strokePulseView = rootView.findViewById(R.id.stroke_pulse_view);
        bottomNavView = activity.findViewById(R.id.nav_view);
        if(bottomNavView != null){
            originalBottomNavBackground = bottomNavView.getBackground();
        }

        setupDismissListeners();
    }

    public void setNavBarColor(int primaryColor, int selectedColor, int unselectedColor){
        this.primaryColor = primaryColor;
        this.selectedColor = selectedColor;
        this.unselectedColor = unselectedColor;
    }

    public void showOverlay(ImageView originalImageView, String imageUrl, float touchX, float touchY){
        originalImageView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        // 1. 위치 계산
        int[] location = new int[2];
        originalImageView.getLocationOnScreen(location);
        int imageX = location[0];
        int imageY = location[1];

        // ✅ 애니메이션 시작 (이미지 중앙에서 시작)
        strokePulseView.startPulse(touchX, touchY);

        // 2. 복제 이미지 설정
        FrameLayout.LayoutParams imageParams = (FrameLayout.LayoutParams) overlayImageClone.getLayoutParams();
        imageParams.width = (int)(scale * originalImageView.getWidth());
        imageParams.height = (int)(scale * originalImageView.getHeight());
        imageParams.leftMargin = imageX;
        imageParams.topMargin = imageY;
        overlayImageClone.setLayoutParams(imageParams);
        overlayImageClone.setImageDrawable(originalImageView.getDrawable());

        // 3. 다운로드 버튼 위치 설정
        FrameLayout.LayoutParams buttonParams = (FrameLayout.LayoutParams) downloadButton.getLayoutParams();
        buttonParams.leftMargin = originalImageView.getWidth() + downloadButton_delta_x; // X 좌표: 이미지 오른쪽 끝
        buttonParams.topMargin = originalImageView.getHeight() + downloadButton_delta_y; // Y 좌표: 이미지 하단
        downloadButton.setLayoutParams(buttonParams);

        // 4. 오버레이 및 배경 제어
        if (bottomNavView != null){
            bottomNavView.setBackgroundColor(Color.TRANSPARENT);
        }
        overlayContainer.setVisibility(View.VISIBLE);

        // 5. 다운로드 리스너 설정
        downloadButton.setOnClickListener((v-> {
            ImageSaveUtil.saveImageFromUrl(activity, imageUrl);
            dismissOverlay();
        }));

    }

    private void dismissOverlay(){
         overlayContainer.setVisibility(View.GONE);
         if (bottomNavView != null && primaryColor != 0 && selectedColor != 0 && unselectedColor != 0){
             bottomNavView.setBackgroundColor(primaryColor);
             int[] colors = new int[] {
                     selectedColor,   // 선택됐을 때의 텍스트 색
                     unselectedColor  // 선택되지 않았을 때의 텍스트 색
             };
             int[][] states = new int[][] {
                     new int[] { android.R.attr.state_checked },  // 선택된 상태
                     new int[] { -android.R.attr.state_checked }  // 선택되지 않은 상태
             };

             android.content.res.ColorStateList textColorStateList = new android.content.res.ColorStateList(states, colors);
             android.content.res.ColorStateList iconColorStateList = new android.content.res.ColorStateList(states, colors);

             // 4. BottomNavigationView에 최종 적용합니다.
             // BottomNavigationView 타입으로 캐스팅해야 관련 메서드를 쓸 수 있습니다.
             if (bottomNavView != null) {
                 com.google.android.material.bottomnavigation.BottomNavigationView bnv =
                         (com.google.android.material.bottomnavigation.BottomNavigationView) bottomNavView;

                 bnv.setItemTextColor(textColorStateList);
                 bnv.setItemIconTintList(iconColorStateList);
             }
         }
         else if (bottomNavView != null){
             bottomNavView.setBackground(originalBottomNavBackground);
         }
    }

    private void setupDismissListeners(){
        dimView.setOnClickListener(v -> dismissOverlay());
    }

    public void setScale(int percentage){
        this.scale = (100 + percentage)/100f;
    }

    public void setDownloadButtonLocation(int delta_x, int delta_y){
        this.downloadButton_delta_x = delta_x;
        this.downloadButton_delta_y = delta_y;
    }


}

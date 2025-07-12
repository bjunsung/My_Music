package com.example.mymusic.util;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

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
    private LinearLayout longClickOverlay;
    private ImageView overlayImageClone;
    private CardView downloadButton;
    private View dimView;
    private CardView imageFetchButton;
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
        downloadButton = rootView.findViewById(R.id.download_button_overlay);
        longClickOverlay = rootView.findViewById(R.id.long_click_overlay); //nullable
        imageFetchButton = rootView.findViewById(R.id.image_fetch_button_overlay); //nullable
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
    public void showOverlay(ImageView originalImageView,
                            String imageUrl, float touchX, float touchY) {
        Log.d("OverlayDebug", "showOverlay called");
        Log.d("OverlayDebug", "touchX=" + touchX + ", touchY=" + touchY);
        overlayContainer.setVisibility(View.VISIBLE);

        originalImageView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        strokePulseView.startPulse(touchX, touchY);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) longClickOverlay.getLayoutParams();
        if (lp == null) {           // 혹시 null 이면 새로 만들어 줌
            lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        lp.leftMargin = (int) touchX;
        lp.topMargin  = (int) touchY;
        longClickOverlay.setLayoutParams(lp);

        longClickOverlay.setVisibility(View.VISIBLE);
        downloadButton.setVisibility(View.VISIBLE);
        if (imageFetchButton != null) imageFetchButton.setVisibility(View.VISIBLE);

        downloadButton.setOnClickListener(v -> {
            ImageSaveUtil.saveImageFromUrl(activity, imageUrl);
            dismissOverlay();
        });
    }




    private void dismissOverlay(){
         overlayContainer.setVisibility(View.GONE);
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

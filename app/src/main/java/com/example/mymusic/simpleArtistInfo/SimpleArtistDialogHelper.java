package com.example.mymusic.simpleArtistInfo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;


import com.example.mymusic.R;

import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;

import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import java.util.List;


public class SimpleArtistDialogHelper {
    public static final int OFF_DETAILS = 0;
    public static final int ON_DETAILS = 1;
    private final String TAG = "SimpleArtistDialogHelper";
    public final static int ARTWORK_SIZE = 480;
    private Context context;
    private SimpleImagePagerAdapter pagerAdapter;
    private SimpleRectangleImagePagerAdapter pagerAdapter2;
    List<FavoriteArtist> artistList;

    /**
     * 이미지 뷰 텍스트뷰
     */
    private ViewPager2 rectangleImagePager;
    private ViewPager2 circleImagePager;
    private TextView artistNameTextView;
    private ImageView expandButton;
    private ImageView contractButton;
    private ScrollView detailScrollView;
    private LinearLayout debutLayout;
    private TextView debutDateTextView;
    private LinearLayout activityYearsLayout;
    private TextView activityYearsTextView;
    private LinearLayout membersLayout;
    private TextView membersTextView;
    private LinearLayout agencyLayout;
    private TextView agencyTextView;
    private LinearLayout activityLayout;
    private TextView activityTextView;
    private MaterialCardView circleImageCardView;
    private Dialog dialog;
    private View emptySpace;
    private FrameLayout paletteFrame;
    private GradientDrawable currentGradient;
    private OnPositionChangedListener positionChangedListener;
    private OnDialogDismissListener dismissListener;

    public interface OnDialogDismissListener{
        void dialogDismissed();
    }

    public interface OnPositionChangedListener{
        void positionChanged(int position, int detail_visible_state, GradientDrawable currentGradient);
    }

    public void setPositionChangedListener(OnPositionChangedListener positionChangedListener){
        this.positionChangedListener = positionChangedListener;
    }

    public void setDismissListener(OnDialogDismissListener dismissListener){
        this.dismissListener = dismissListener;
    }

    public SimpleArtistDialogHelper(Context context, List<FavoriteArtist> artistList){
        this.context = context;
        this.artistList = artistList;

        bindView();

        List<String> imageUrls = new ArrayList<>();
        for (FavoriteArtist artist : artistList){
            if (artist.metadata != null && artist.metadata.images != null && !artist.metadata.images.isEmpty())
                imageUrls.add(artist.metadata.images.get(0));
        }


        pagerAdapter = new SimpleImagePagerAdapter(imageUrls);
        circleImagePager.setAdapter(pagerAdapter);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // 다이얼로그가 닫힐 때 수행할 작업
                Log.d("Dialog", "Dialog was dismissed");
                if (dismissListener != null){
                    dismissListener.dialogDismissed();
                }
            }
        });


        circleImagePager.setOffscreenPageLimit(1);

        circleImagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                rectangleImagePager.setCurrentItem(position, false);
                showArtistDialog(position);
                Log.d(TAG, "position changed Circ: " + position);
                if (positionChangedListener != null) {
                    int detailVisibleState = (rectangleImagePager.getVisibility() == View.VISIBLE) ? SimpleArtistDialogHelper.ON_DETAILS : SimpleArtistDialogHelper.OFF_DETAILS;
                    Log.d(TAG, "detailVisibleState: " + detailVisibleState);
                    positionChangedListener.positionChanged(position, detailVisibleState, currentGradient);
                }

            }
        });

        pagerAdapter2 = new SimpleRectangleImagePagerAdapter(imageUrls);
        rectangleImagePager.setAdapter(pagerAdapter2);
        rectangleImagePager.setOffscreenPageLimit(1);

        rectangleImagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                circleImagePager.setCurrentItem(position, false);
                showArtistDialog(position);
                Log.d(TAG, "position changed Rec: " + position);
                if (positionChangedListener != null) {
                    //int detailVisibleState = (rectangleImagePager.getVisibility() == View.VISIBLE) ? SimpleArtistDialogHelper.ON_DETAILS : SimpleArtistDialogHelper.OFF_DETAILS;
                    //Log.d(TAG, "detailVisibleState: " + detailVisibleState);
                    //Log.d(TAG, "state changed callback transmitted");
                    //positionChangedListener.positionChanged(position, detailVisibleState, currentGradient);
                }

            }
        });


    }

    private void bindView(){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_simple_artist_info);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);             // 뒤로가기 버튼으로 닫히는 것 비활성화 (선택)

        rectangleImagePager = dialog.findViewById(R.id.rectangle_image_view);
        circleImagePager = dialog.findViewById(R.id.circle_image_pager);
        artistNameTextView = dialog.findViewById(R.id.artist_name);
        expandButton = dialog.findViewById(R.id.expand_button);
        contractButton = dialog.findViewById(R.id.contract_button);
        detailScrollView = dialog.findViewById(R.id.detail_container);
        debutLayout = dialog.findViewById(R.id.debut_layout);
        debutDateTextView = dialog.findViewById(R.id.debut_date);
        activityYearsLayout = dialog.findViewById(R.id.activity_years_layout);
        activityYearsTextView =  dialog.findViewById(R.id.activity_years);
        membersLayout = dialog.findViewById(R.id.members_layout);
        membersTextView = dialog.findViewById(R.id.members);
        agencyLayout = dialog.findViewById(R.id.agency_layout);
        agencyTextView = dialog.findViewById(R.id.agency);
        activityLayout = dialog.findViewById(R.id.activity_layout);
        activityTextView = dialog.findViewById(R.id.activity);
        circleImageCardView = dialog.findViewById(R.id.circle_image_card);
        emptySpace = dialog.findViewById(R.id.empty_space);
    }

    private void setView(int position){

        ArtistMetadata artistMetadata;
        FavoriteArtist favoriteArtist = artistList.get(position);
        Artist artist = favoriteArtist.artist;
        artistMetadata = favoriteArtist.metadata;



        if (artistMetadata == null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "아티스트 정보가 없습니다.", Toast.LENGTH_SHORT));
            return;
        }

        if (artistMetadata.artistNameKr != null && !artistMetadata.artistNameKr.isEmpty()){
            artistNameTextView.setText(artistMetadata.artistNameKr);
        }
        else {
            artistNameTextView.setText(artist.artistName);
        }

        if (artistMetadata.debutDate != null && !artistMetadata.debutDate.isEmpty()){
            debutLayout.setVisibility(View.VISIBLE);
            debutDateTextView.setText(artistMetadata.debutDate);
        } else{
            debutLayout.setVisibility(View.GONE);
        }

        if (artistMetadata.yearsOfActivity != null && !artistMetadata.yearsOfActivity.isEmpty()){
            activityYearsLayout.setVisibility(View.VISIBLE);
            activityYearsTextView.setText(String.join(", ", artistMetadata.yearsOfActivity));
        } else{
            activityYearsLayout.setVisibility(View.GONE);
        }

        if (artistMetadata.members != null && !artistMetadata.members.isEmpty()){
            membersLayout.setVisibility(View.VISIBLE);
            membersTextView.setText(artistMetadata.membersToString());
        } else{
            membersLayout.setVisibility(View.GONE);
        }

        if (artistMetadata.agency != null && !artistMetadata.agency.isEmpty()){
            agencyLayout.setVisibility(View.VISIBLE);
            agencyTextView.setText(String.join(", ", artistMetadata.agency));
        } else{
            agencyLayout.setVisibility(View.GONE);
        }

        if (artistMetadata.activity != null && !artistMetadata.activity.isEmpty()){
            activityLayout.setVisibility(View.VISIBLE);
            activityTextView.setText(artistMetadata.activityToString());
        } else{
            activityLayout.setVisibility(View.GONE);
        }
        paletteFrame = dialog.findViewById(R.id.palette_frame);
        if (currentGradient != null){
            paletteFrame.setBackground(currentGradient);
        }

        ImageColorAnalyzer.analyzePrimaryColor(context, artistMetadata.images.get(0), new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
            @Override
            public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                /*
                int[] colorPair = MyColorUtils.generateContrastColors(primaryColor, 1.5f, 0.42f, 0.1f, 0.9f, 0.3f);
int[] colorPair = MyColorUtils.generateBoundedContrastColors(MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.7f), 0.85f, 0.15f, 0.3f, 0.7f, 0.6f,0.9f);
                 */
                int[] colorPair = MyColorUtils.generateContrastColors(MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.5f), 1.5f, 0.42f, 0.1f, 0.9f, 0.3f);

                gradiantToFrame(paletteFrame, colorPair[0], colorPair[1]);

            }

            @Override
            public void onFailure() {
                if (currentGradient != null){
                    paletteFrame.setBackground(currentGradient);
                } else {
                    Log.d(TAG, "gradient is null");
                    int brightenColor = Color.parseColor("#B39DDB"); // 밝은 보라
                    int darkenColor = Color.parseColor("#4527A0");   // 진한 보라
                    gradiantToFrame(paletteFrame, brightenColor, darkenColor);
                }
            }
        });
        expandButton.setOnClickListener(v -> {
            expandDetails();;
        });

        contractButton.setOnClickListener(v -> {
            contractButton.setVisibility(View.GONE);
            detailScrollView.setVisibility(View.GONE);
            expandButton.setVisibility(View.VISIBLE);
            circleImageCardView.setVisibility(View.VISIBLE);
            rectangleImagePager.setVisibility(View.GONE);
            emptySpace.setVisibility(View.VISIBLE);
            paletteFrame.setBackground(currentGradient);
            if (positionChangedListener != null){
                Log.d(TAG, "state changed callback transmitted");
                positionChangedListener.positionChanged(circleImagePager.getCurrentItem(), SimpleArtistDialogHelper.OFF_DETAILS, currentGradient);
            }
        });

        dialog.show();
    }

    public void dismissDialog(){
        dialog.dismiss();
    }

    private void expandDetails(){
        expandButton.setVisibility(View.GONE);
        detailScrollView.setVisibility(View.VISIBLE);
        contractButton.setVisibility(View.VISIBLE);
        circleImageCardView.setVisibility(View.GONE);
        rectangleImagePager.setVisibility(View.VISIBLE);
        emptySpace.setVisibility(View.GONE);
        paletteFrame.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        if (positionChangedListener != null){
            Log.d(TAG, "state changed callback transmitted");
            positionChangedListener.positionChanged(circleImagePager.getCurrentItem(), SimpleArtistDialogHelper.ON_DETAILS, currentGradient);
        }
    }

    public int getRealPosition(int position) {
        return position % pagerAdapter.getRealCount();
    }

    public void showArtistDialogFirstTime(int position){
        int initialPosition = pagerAdapter.getRealCount() * 500 + position;
        showArtistDialog(initialPosition);
    }
    public void showArtistDialog(int position){
        int realPosition = getRealPosition(position);
        setView(realPosition);
        circleImagePager.setCurrentItem(position, false);
        rectangleImagePager.setCurrentItem(position, false);
    }

    public void showArtistDialog(int position, List<FavoriteArtist> favoriteArtistList, GradientDrawable lastGradient){ // 화면 회전 등 재생성 대비 빠른 시간안에 dialog 생성을 위해 count FavorieFragment에서 받아오기
        if (favoriteArtistList.isEmpty()) return;

        this.artistList = favoriteArtistList;
        currentGradient = lastGradient;
        int realPosition = position % favoriteArtistList.size();
        Log.d(TAG, "received position: " + position + " real position: " + realPosition);
        setView(realPosition);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            circleImagePager.setCurrentItem(position, true);
            rectangleImagePager.setCurrentItem(position, false );
        }, 50);
    }


    public void showArtistDialogWithExpand(int position){
        int realPosition = getRealPosition(position);
        setView(realPosition);
        circleImagePager.setCurrentItem(position, false);
        rectangleImagePager.setCurrentItem(position, false);
        expandDetails();
    }

    public void showArtistDialogWithExpand(int position, List<FavoriteArtist> favoriteArtistList, GradientDrawable lastGradient){  // 화면 회전 등 재생성 대비 빠른 시간안에 dialog 생성을 위해 count FavorieFragment에서 받아오기
        if (favoriteArtistList.isEmpty()) return;

        this.artistList = favoriteArtistList;
        currentGradient = lastGradient;
        int realPosition = position % favoriteArtistList.size();
        Log.d(TAG, "On Details Mode: received position: " + position + " real position: " + realPosition);
        setView(realPosition);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            rectangleImagePager.setCurrentItem(position, true);
            circleImagePager.setCurrentItem(position, false);

        }, 50);

        expandDetails();
    }



    private void gradiantToFrame(FrameLayout frameLayout, int brightenColor, int darkenColor){
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{darkenColor, brightenColor, darkenColor}
        );
        gradient.setCornerRadius(5f); // 필요 시 곡률

        currentGradient = gradient;

        if (rectangleImagePager.getVisibility() == View.VISIBLE){
            frameLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
        else{
            frameLayout.setBackground(gradient);
        }


    }

    public void updateList(List<FavoriteArtist> artistList){
        this.artistList = artistList;
        List<String> imageUrls = new ArrayList<>();
        for (FavoriteArtist artist : artistList){
            String secondaryImageUrl = artist.getSecondaryImageUrl();
            if (secondaryImageUrl != null) {
                imageUrls.add(secondaryImageUrl);
            }
        }
        pagerAdapter.updateList(imageUrls);
        pagerAdapter2.updateList(imageUrls);
    }

}

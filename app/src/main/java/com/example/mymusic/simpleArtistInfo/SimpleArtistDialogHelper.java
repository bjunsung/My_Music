package com.example.mymusic.simpleArtistInfo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;

import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.R;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.ui.artistInfo.ImagePagerAdapter;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SimpleArtistDialogHelper {
    private final String TAG = "SimpleArtistDialogHelper";
    public final static int ARTWORK_SIZE = 360;
    private Context context;
    private SimpleImagePagerAdapter pagerAdapter;
    List<FavoriteArtist> artistList;
    List<Bitmap> bitmapList;
    /**
     * 이미지 뷰 텍스트뷰
     */
    private ImageView rectangleImageView;
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

    public SimpleArtistDialogHelper(Context context, List<FavoriteArtist> artistList){
        this.context = context;
        this.artistList = artistList;
        this.bitmapList = new ArrayList<>(Collections.nCopies(artistList.size(), null));

        bindView();

        List<String> imageUrls = new ArrayList<>();
        for (FavoriteArtist artist : artistList){
            if (artist.metadata != null && artist.metadata.images != null && !artist.metadata.images.isEmpty())
                imageUrls.add(artist.metadata.images.get(0));
        }


        pagerAdapter = new SimpleImagePagerAdapter(imageUrls);
        pagerAdapter.setImageLoadListener(new SimpleImagePagerAdapter.OnImageLoadListener() {
            @Override
            public void onLoadSuccess(int position, Bitmap bitmap) {
                if (position >= 0 && position < bitmapList.size()) {
                    bitmapList.set(position, bitmap);
                } else {
                    Log.e(TAG, "Position out of bounds: " + position);
                }
            }
        });
        circleImagePager.setAdapter(pagerAdapter);
        circleImagePager.setOffscreenPageLimit(1);

        circleImagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                showArtistDialog(position);
            }
        });
    }

    private void bindView(){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_simple_artist_info);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);
        rectangleImageView = dialog.findViewById(R.id.rectangle_image_view);
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
    }

    private void setView(int position){
        FavoriteArtist favoriteArtist = artistList.get(position);
        Artist artist = favoriteArtist.artist;
        ArtistMetadata artistMetadata = favoriteArtist.metadata;

        if (artistMetadata == null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "아티스트 정보가 없습니다.", Toast.LENGTH_SHORT));
            return;
        }

        if (artistMetadata.artistNameKr != null && !artistMetadata.artistNameKr.isEmpty()){
            artistNameTextView.setText(artistMetadata.artistNameKr);
        } else{
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
        FrameLayout paletteFrame = dialog.findViewById(R.id.palette_frame);

        ImageColorAnalyzer.analyzePrimaryColor(context, artistMetadata.images.get(0), new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
            @Override
            public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                int[] colorPair = MyColorUtils.generateContrastColors(primaryColor, 1.5f, 0.42f, 0.1f, 0.9f, 0.3f);
                gradiantToFrame(paletteFrame, colorPair[0], colorPair[1]);

            }

            @Override
            public void onFailure() {
                int brightenColor = Color.parseColor("#B39DDB"); // 밝은 보라
                int darkenColor = Color.parseColor("#4527A0");   // 진한 보라
                gradiantToFrame(paletteFrame, brightenColor, darkenColor);
            }
        });
        expandButton.setOnClickListener(v -> {
            expandButton.setVisibility(View.GONE);
            detailScrollView.setVisibility(View.VISIBLE);
            contractButton.setVisibility(View.VISIBLE);
            circleImageCardView.setVisibility(View.GONE);
            rectangleImageView.setVisibility(View.VISIBLE);
        });

        contractButton.setOnClickListener(v -> {
            contractButton.setVisibility(View.GONE);
            detailScrollView.setVisibility(View.GONE);
            expandButton.setVisibility(View.VISIBLE);
            circleImageCardView.setVisibility(View.VISIBLE);
            rectangleImageView.setVisibility(View.GONE);
        });

        dialog.show();
    }

    public void showArtistDialog(int position){
        setView(position);
        circleImagePager.setCurrentItem(position, false);
        Bitmap bitmap = bitmapList.get(position);
        if (bitmap != null) {
            rectangleImageView.setImageBitmap(bitmapList.get(position));
        } else{
            ArtistMetadata metadata = artistList.get(position).metadata;
            if (metadata != null && metadata.images != null && !metadata.images.isEmpty()){
                Glide.with(context)
                        .asBitmap()
                        .load(metadata.images.get(0))
                        .centerCrop()
                        .override(SimpleArtistDialogHelper.ARTWORK_SIZE, SimpleArtistDialogHelper.ARTWORK_SIZE)
                        .error(R.drawable.ic_image_not_found_foreground)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                rectangleImageView.setImageBitmap(resource);
                                bitmapList.set(position, resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }
        }
    }


    private void gradiantToFrame(FrameLayout frameLayout, int brightenColor, int darkenColor){
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{darkenColor, brightenColor, darkenColor}
        );
        gradient.setCornerRadius(5f); // 필요 시 곡률
        frameLayout.setBackground(gradient);
    }

    public void updateList(List<FavoriteArtist> artistList){
        this.artistList = artistList;
        this.bitmapList = new ArrayList<>(Collections.nCopies(artistList.size(), null));
        List<String> imageUrls = new ArrayList<>();
        for (FavoriteArtist artist : artistList){
            String secondaryImageUrl = artist.getSecondaryImageUrl();
            if (secondaryImageUrl != null) {
                imageUrls.add(secondaryImageUrl);
            }
        }
        pagerAdapter.updateList(imageUrls);
    }

}

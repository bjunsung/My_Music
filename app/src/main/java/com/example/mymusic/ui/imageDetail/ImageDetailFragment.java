package com.example.mymusic.ui.imageDetail;

import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.transition.Transition;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.example.mymusic.R; // 자신의 R 클래스 경로
import com.example.mymusic.cache.ImagePreloader;
import com.example.mymusic.cache.customCache.CustomImageCache;
import com.example.mymusic.databinding.FragmentImageDetailBinding;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageDetailFragment extends Fragment {

    private ViewPager2 viewPager;
    private ArrayList<String> imageUrls;
    private int startPosition;
    private WindowInsetsControllerCompat insetsController;
    private ImageButton closeButton;
    private FrameLayout detailContainer;
    private boolean isLightMode = false;
    ImageButton backButtonImageButton, emptySpaceImageButton;
    private FragmentImageDetailBinding binding;
    private String transitionName;
    private final String TAG = "ImageDetailFragment";
    public static final String REQUEST_KEY = "details_fragment_request";
    public static final String REQUEST_KEY_POSITION = "details_fragment_request_position";
    public static final String BUNDLE_KEY_TRANSITION_END = "transition_ended";
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private AtomicBoolean isUserScrolling = new AtomicBoolean(false);
    private AtomicInteger lastConfirmedPosition = new AtomicInteger(0);

    private AtomicInteger selectedPosition = new AtomicInteger(0);
    private static final long AUTO_SLIDER_DELAY_TIME = 5000L;
    private String currentTransitionName;
    private int lastPosition;
    private Map<String, View> sharedElements;
    private LinearLayout imageCountInfoLayout;
    private TextView currentPositionNumberTextView, totalImageSizeTextView;
    private int totalImageSize;
    private Context viewGroupContext;
    private DetailImagePagerAdapter pagerAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //앨범 이미지 트랜지션 설정
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setPathMotion(new MaterialArcMotion());
        setSharedElementEnterTransition(transform);
        setSharedElementReturnTransition(new MaterialContainerTransform());


        Transition returnTransition = (Transition) getSharedElementReturnTransition();
        if (returnTransition != null) {
            returnTransition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(@NonNull Transition transition) {
                    Log.d(TAG, "transition start");
                }

                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    Log.d(TAG, "transition end");
                    Bundle result = new Bundle();
                    result.putBoolean(BUNDLE_KEY_TRANSITION_END, true);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    transition.removeListener(this);
                }
                @Override
                public void onTransitionCancel(@NonNull Transition transition) {}
                @Override
                public void onTransitionPause(@NonNull Transition transition) {}
                @Override
                public void onTransitionResume(@NonNull Transition transition) {}
            });
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageDetailBinding.inflate(inflater, container, false);
        viewGroupContext = container.getContext();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 애니메이션 시작을 잠시 지연
        postponeEnterTransition();


        // Safe Args 또는 Bundle에서 데이터 가져오기
        if (getArguments() != null) {
            imageUrls = getArguments().getStringArrayList("image_urls");
            startPosition = getArguments().getInt("start_position");
            transitionName = getArguments().getString("transitionName");
            totalImageSize = imageUrls.size();
        }

        bindView(view);
        setView();
        pagerAdapter = new DetailImagePagerAdapter(getContext(), imageUrls, (v) -> toggleUiMode());
        pagerAdapter.setZoomListener(new DetailImagePagerAdapter.ZoomListener() {
            @Override
            public void onZoomIn() {
                sliderHandler.removeCallbacks(sliderRunnable);
            }

            @Override
            public void onZoomOut() {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });

        viewPager.setAdapter(pagerAdapter);
        // 시작 위치로 바로 이동 (애니메이션 없이)
        viewPager.setCurrentItem(startPosition, false);
        if (transitionName != null)
            viewPager.setTransitionName(transitionName);

        // ViewPager2가 해당 페이지를 그릴 준비가 될 때까지 기다린 후 애니메이션 시작
        viewPager.getViewTreeObserver().addOnPreDrawListener(
                new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        viewPager.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                }
        );

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) { //페이지 전환 감지
                super.onPageSelected(position);
                currentPositionNumberTextView.setText(String.valueOf(position + 1));
                lastConfirmedPosition.set(selectedPosition.get());
                selectedPosition.set(position);
                saveCache(position);
            }
        });


        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING){ //사용자 터치 감지
                    sliderHandler.removeCallbacks(sliderRunnable);
                    isUserScrolling.set(true);
                }
                else if (state == ViewPager2.SCROLL_STATE_IDLE){ //스크롤 정지 상태 감지
                    if (isUserScrolling.get()){ // 유저 컨트롤
                        isUserScrolling.set(false);
                        int prevPos = lastConfirmedPosition.get();
                        int newPos = selectedPosition.get();
                        if (newPos != prevPos){
                            lastConfirmedPosition.set(newPos);
                            sliderHandler.postDelayed(sliderRunnable, 7500L);
                        } else{
                            sliderHandler.postDelayed(sliderRunnable, 10000L);
                        }
                    } else{ //자동 스크롤
                        sliderHandler.removeCallbacks(sliderRunnable);
                        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
                    }
                }
            }
        });


        sliderRunnable = () -> {
            if (viewPager != null && pagerAdapter != null){
                int nextItem = (viewPager.getCurrentItem() + 1) % pagerAdapter.getItemCount();
                viewPager.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
            }
        };

    }

    private void saveCache(int currentPosition) {
        for (int idx = currentPosition  ; idx < currentPosition + 1; ++idx){
            if (idx > 0 && idx < imageUrls.size()){ //0 이면 저장 x
                String url = imageUrls.get(idx);
                if (CustomImageCache.getInstance().get(url) == null) { //캐시에 없을 때만 저장
                    Glide.with(viewGroupContext)
                            .asBitmap()
                            .load(url)
                            .override(ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE)
                            .centerCrop()
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                                    CustomImageCache.getInstance().put(url, resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            });
                }
            }
        }


    }

    private void bindView(View view){
        viewPager = view.findViewById(R.id.detail_view_pager);
        closeButton = view.findViewById(R.id.close_button);
        detailContainer = view.findViewById(R.id.detail_container);
        Activity activity = requireActivity();
        if (activity != null){
            backButtonImageButton = activity.findViewById(R.id.back_button);
            emptySpaceImageButton = activity.findViewById(R.id.empty_space);
        }
        imageCountInfoLayout = view.findViewById(R.id.image_count_info_layout);
        currentPositionNumberTextView = view.findViewById(R.id.current_number);
        totalImageSizeTextView = view.findViewById(R.id.total_image_count);
    }

    private void setView(){
        closeButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        totalImageSizeTextView.setText(String.valueOf(totalImageSize));
        currentPositionNumberTextView.setText(String.valueOf(startPosition + 1));
        imageCountInfoLayout.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume called");

        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, 3000);

        Activity activity = requireActivity();
        if (activity == null || activity.getWindow() == null) return;


        //keep screen on
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // backButtonImageButton이 null이면 다시 시도하여 가져오기
        if (backButtonImageButton == null) {
            backButtonImageButton = activity.findViewById(R.id.back_button);
        }

        if (backButtonImageButton != null) {
            backButtonImageButton.setVisibility(View.GONE);
        } else {
            // 여전히 null인 경우 다시 시도 (한 번 더)
            backButtonImageButton = activity.findViewById(R.id.back_button);
            if (backButtonImageButton != null) {
                backButtonImageButton.setVisibility(View.GONE);
            }
        }

        // emptySpaceImageButton도 같은 방식 적용
        if (emptySpaceImageButton == null) {
            emptySpaceImageButton = activity.findViewById(R.id.empty_space);
        }

        if (emptySpaceImageButton != null) {
            emptySpaceImageButton.setVisibility(View.GONE);
        }

        // 시스템 바 숨기기
        insetsController = WindowCompat.getInsetsController(
                activity.getWindow(),
                activity.getWindow().getDecorView()
        );

        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause called");

        Bundle bundle = new Bundle();
        lastPosition = viewPager.getCurrentItem();
        bundle.putInt("position", lastPosition);


        getParentFragmentManager().setFragmentResult(REQUEST_KEY_POSITION, bundle);
        Log.d(TAG, "position 전달 " + viewPager.getCurrentItem());


        Activity activity = requireActivity();
        if (activity != null){
            //keep screen off
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }
        // 프래그먼트를 떠날 때 전체 화면 모드 해제
        if (insetsController != null) {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        }
        if (backButtonImageButton != null) { backButtonImageButton.setVisibility(View.VISIBLE); }
        if (emptySpaceImageButton != null) { emptySpaceImageButton.setVisibility(View.VISIBLE); }
        sliderHandler.removeCallbacks(sliderRunnable);

    }

    private void toggleUiMode(){
        isLightMode = !isLightMode;
        if (isLightMode){
            imageCountInfoLayout.setVisibility(View.VISIBLE);
            detailContainer.setBackgroundColor(Color.WHITE);
            closeButton.setVisibility(View.VISIBLE);
            sliderHandler.removeCallbacks(sliderRunnable);
        }else{
            imageCountInfoLayout.setVisibility(View.GONE);
            detailContainer.setBackgroundColor(Color.BLACK);
            closeButton.setVisibility(View.GONE);
            sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
        }
    }


}
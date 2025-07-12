package com.example.mymusic.ui.imageDetail;

import androidx.core.app.SharedElementCallback;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.transition.Explode;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.viewpager2.widget.ViewPager2;
import com.example.mymusic.R; // 자신의 R 클래스 경로
import com.example.mymusic.databinding.FragmentImageDetailBinding;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;

import java.util.ArrayList;
import java.util.List;

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
    public static final String BUNDLE_KEY_TRANSITION_END = "transition_ended";
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;



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

                }

                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    Bundle result = new Bundle();
                    result.putBoolean(BUNDLE_KEY_TRANSITION_END, true);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionCancel(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionPause(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionResume(@NonNull Transition transition) {

                }
            });
        }


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageDetailBinding.inflate(inflater, container, false);
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
        }

        bindView(view);
        setView();
        DetailImagePagerAdapter adapter = new DetailImagePagerAdapter(imageUrls, (v) -> toggleUiMode());
        adapter.setZoomListener(new DetailImagePagerAdapter.ZoomListener() {
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

        viewPager.setAdapter(adapter);
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




        sliderRunnable = () -> {
            if (viewPager != null && adapter != null){
                int nextItem = (viewPager.getCurrentItem() + 1) % adapter.getItemCount();
                viewPager.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(sliderRunnable, 5000);
            }
        };

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
    }

    private void setView(){
        closeButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, 3000);
        // 전체 화면 모드 설정 (상태 바, 내비게이션 바 숨기기)
        if (getActivity() != null && getActivity().getWindow() != null) {
            backButtonImageButton.setVisibility(View.GONE);
            emptySpaceImageButton.setVisibility(View.GONE);
            Activity activity = getActivity();
            if (activity != null) {
                insetsController = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
                if (insetsController != null) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 프래그먼트를 떠날 때 전체 화면 모드 해제
        if (insetsController != null) {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        }
        backButtonImageButton.setVisibility(View.VISIBLE);
        emptySpaceImageButton.setVisibility(View.VISIBLE);
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    private void toggleUiMode(){
        isLightMode = !isLightMode;
        if (isLightMode){
            detailContainer.setBackgroundColor(Color.WHITE);
            closeButton.setVisibility(View.VISIBLE);
            if (insetsController != null){
                insetsController.show(WindowInsetsCompat.Type.systemBars());
            }
        }else{
            detailContainer.setBackgroundColor(Color.BLACK);
            closeButton.setVisibility(View.GONE);
            if (insetsController != null){
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
            }
        }
    }

}
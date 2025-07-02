package com.example.mymusic.ui.imageDetail;

import androidx.fragment.app.Fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.transition.ChangeBounds;
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
import androidx.transition.TransitionInflater;
import androidx.viewpager2.widget.ViewPager2;
import com.example.mymusic.R; // 자신의 R 클래스 경로

import java.util.ArrayList;

public class ImageDetailFragment extends Fragment {

    private ViewPager2 viewPager;
    private ArrayList<String> imageUrls;
    private int startPosition;
    private WindowInsetsControllerCompat insetsController;
    private ImageButton closeButton;
    private FrameLayout detailContainer;
    private boolean isLightMode = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 애니메이션 설정
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_image_detail, container, false);
        setSharedElementEnterTransition(new ChangeBounds().setDuration(300));
        setSharedElementReturnTransition(new ChangeBounds().setDuration(300));
        return view;
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
        }

        bindView(view);
        setView();
        DetailImagePagerAdapter adapter = new DetailImagePagerAdapter(imageUrls, (v) -> toggleUiMode());


        viewPager.setAdapter(adapter);
        // 시작 위치로 바로 이동 (애니메이션 없이)
        viewPager.setCurrentItem(startPosition, false);

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


    }

    private void bindView(View view){
        viewPager = view.findViewById(R.id.detail_view_pager);
        closeButton = view.findViewById(R.id.close_button);
        detailContainer = view.findViewById(R.id.detail_container);
    }

    private void setView(){
        closeButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public void onResume() {
        super.onResume();
        // 전체 화면 모드 설정 (상태 바, 내비게이션 바 숨기기)
        if (getActivity() != null && getActivity().getWindow() != null) {
            insetsController = WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());
            if (insetsController != null) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
                insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
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
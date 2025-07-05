package com.example.mymusic.ui.musicInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.navigation.NavDestination;
import androidx.transition.Explode;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.R;
import com.example.mymusic.databinding.FragmentMusicInfoBinding;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.example.mymusic.util.DateUtils;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.ImageOverlayManager;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MusicInfoFragment extends Fragment {
    private final String TAG = "MusicInfoFragment";
    private Favorite favorite;
    private Track track;
    FavoritesViewModel favoritesViewModel;
    FavoriteArtistViewModel favoriteArtistViewModel;
    TextView trackTitle, trackTitleKr, addedDate;
    LinearLayout addedDateLayout;
    private boolean savedInDb;
    private ImageOverlayManager imageOverlayManager;
    private int artworkSize;
    private View bottomNavView;
    ImageButton backButton;
    private int primaryColor, selectedColor, unselectedColor;
    // ✅ ViewBinding 사용을 권장합니다
    private FragmentMusicInfoBinding binding;
    private MusicInfoViewModel viewModel;


    //ViewModel 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);


        viewModel = new ViewModelProvider(this).get(MusicInfoViewModel.class);

        // ✅ 1. 전환 애니메이션 종류를 여기서 설정합니다.
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setPathMotion(new MaterialArcMotion());
        transform.setDuration(500);
        transform.setInterpolator(new FastOutSlowInInterpolator());
        setSharedElementEnterTransition(transform);
        setSharedElementReturnTransition(transform);

        // ✅ 2. 나머지 뷰(텍스트 등)를 위한 전환 설정
        setEnterTransition(new Explode());

        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMusicInfoBinding.inflate(inflater, container, false);

        savedInDb = false;

        return binding.getRoot();
    }


    /*
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        if (getView() != null){
            String currentTransitionName = ViewCompat.getTransitionName(binding.artworkImage);
            outState.putString(KEY_TRANSITION_NAME, currentTransitionName);
            Log.d("TransitionDebug", "MusicInfoFragment: onSaveInstanceState 저장, name=" + currentTransitionName);
        }
    }

    */

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        bottomNavView = requireActivity().findViewById(R.id.nav_view);
        backButton = requireActivity().findViewById(R.id.back_button);

        //사진 다운로드를 위한 매니저 객체 설정
        imageOverlayManager = new ImageOverlayManager(requireActivity(), view);
        imageOverlayManager.setScale(0);


        favorite = getArguments().getParcelable("favorite");


        if (favorite == null)
            Log.d(TAG, "get Empty Favorite element");

        if (favorite != null && favorite.track != null) {
            track = favorite.track;
            TrackMetadata metadata = favorite.metadata;

            //ImageView artworkImage = view.findViewById(R.id.artworkImage);
            ImageView artworkImage = binding.artworkImage;


            if (viewModel.getInitialTransitionName() == null){
                String receivedTransitionName = getArguments().getString("transitionName");
                viewModel.setInitialTransitionName(receivedTransitionName);
                ViewCompat.setTransitionName(binding.artworkImage, receivedTransitionName);
                Log.d(TAG, "onViewCreated 에서 '전달받은' 이름 설정, name=" + receivedTransitionName);
            }else{
                String savedName = viewModel.getCurrentTransitionName();
                ViewCompat.setTransitionName(binding.artworkImage, savedName);
                Log.d(TAG, "MusicInfoFragment: viewModel 에 '저장된' current 이름 복원, name=" + savedName);
            }


            artworkImage.post(() -> {
                artworkSize = artworkImage.getWidth();
                imageOverlayManager.setDownloadButtonLocation(0, - (int)((0.2)* artworkSize));

            });


            trackTitle = view.findViewById(R.id.trackTitle);
            TextView artistName = view.findViewById(R.id.artistName);
            TextView albumName = view.findViewById(R.id.albumName);
            TextView releaseDate = view.findViewById(R.id.releaseDate);
            TextView durationMs = view.findViewById(R.id.durationMs);

            trackTitleKr = view.findViewById(R.id.trackTitleKr);
            LinearLayout lyricistLayout = view.findViewById(R.id.lyricists_layout);
            LinearLayout composersLayout = view.findViewById(R.id.composers_layout);
            TextView lyricist = view.findViewById(R.id.lyricists);
            TextView composers = view.findViewById(R.id.composers);
            TextView lyrics = view.findViewById(R.id.metadata_lyrics);
            LinearLayout vocalistsLayout = view.findViewById(R.id.vocalists_layout);
            TextView vocalists = view.findViewById(R.id.vocalists);
            TextView daysBetween = view.findViewById(R.id.days_between);
            addedDateLayout = view.findViewById(R.id.added_date_layout);
            addedDate = view.findViewById(R.id.added_date);
            ImageView enlargeButton = view.findViewById(R.id.enlarge_button);


            trackTitle.setText(track.trackName);
            if (metadata != null && metadata.title != null){
                trackTitleKr.setText(metadata.title);
                trackTitle.setVisibility(TextView.GONE);
                trackTitleKr.setVisibility(TextView.VISIBLE);
            }
            else {
                trackTitleKr.setVisibility(TextView.GONE);
                trackTitle.setVisibility(TextView.VISIBLE);
            }

            if (metadata != null && metadata.lyricists != null){
                lyricist.setText(String.join(", ", metadata.lyricists));
                lyricistLayout.setVisibility(TextView.VISIBLE);
            }

            if (metadata != null && metadata.composers != null){
                composers.setText(String.join(", ", metadata.composers));
                composersLayout.setVisibility(TextView.VISIBLE);
            }

            if (metadata != null && metadata.vocalists != null && !metadata.vocalists.isEmpty()){
                vocalists.setText(metadata.vocalistsToString());
                vocalistsLayout.setVisibility(TextView.VISIBLE);
            }

            if (favorite.addedDate != null && !favorite.addedDate.isEmpty()){
                addedDate.setText(favorite.addedDate);
                addedDateLayout.setVisibility(View.VISIBLE);
            }

            daysBetween.setText(String.valueOf(DateUtils.calculateDateDiffrence(track.releaseDate, DateUtils.today())));


            trackTitle.setOnClickListener(v -> {
                if (savedInDb)
                    switchTitleLanguage();
                else{
                    favoritesViewModel.loadFavoriteItem(track.trackId, loaded -> {
                        if (loaded != null) {
                            savedInDb = true;
                            switchTitleLanguage();
                        }
                    }
                    );
                }
            });
            trackTitleKr.setOnClickListener(v -> switchTitleLanguage());



            if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
                postponeEnterTransition();
                Context context = getContext();
                if (context != null) {
                    Glide.with(context)
                            .load(track.artworkUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .error(R.drawable.ic_image_not_found_foreground) // 실패 시 이미지
                            .centerCrop()
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    artworkImage.setImageDrawable(resource);
                                    startPostponedEnterTransition();
                                    artworkImage.postDelayed(() -> {
                                        artworkImage.setTransitionName(viewModel.getInitialTransitionName());
                                        Log.d(TAG, "TransitionName set to initial transName: " + viewModel.getInitialTransitionName() );
                                    }, 100);

                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                    // 필요하면 placeholder 정리
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    artworkImage.setImageResource(R.drawable.ic_image_not_found_foreground);
                                    startPostponedEnterTransition(); // ✅ 실패해도 전환 시작
                                }

                            });
                }

            }else{
                Log.d(TAG, "artworkUrl is empty");
            }

            artworkImage.post(()->{
                Context context = getContext();
                if (context!=null){
                    int[] pagerLocation = new int[2];
                    artworkImage.getLocationOnScreen(pagerLocation);
                    int pagerRightX = pagerLocation[0] + artworkImage.getWidth();    // Pager의 오른쪽 끝 X 좌표
                    int pagerBottomY = pagerLocation[1] + artworkImage.getHeight();

                    // 2. 버튼의 크기를 고려하여 위치 계산
                    // (버튼의 너비와 높이를 알아야 정확한 위치에 놓을 수 있습니다)
                    int buttonWidth = enlargeButton.getWidth();
                    int buttonHeight = enlargeButton.getHeight();

                    // 만약 버튼 크기가 0으로 나온다면, 임시로 크기를 지정해줍니다 (dp를 px로 변환)

                    final float density = getResources().getDisplayMetrics().density; //여기에서 팅김
                    if (buttonWidth == 0) buttonWidth = (int)(24 * density);
                    if (buttonHeight == 0) buttonHeight = (int)(24 * density);

                    int padding = (int)(6 * density); // 우측, 하단 여백

                    // 3. LayoutParams에 적용
                    FrameLayout.LayoutParams enlargeParams = (FrameLayout.LayoutParams) enlargeButton.getLayoutParams();
                    enlargeParams.leftMargin = pagerRightX - buttonWidth - padding;
                    enlargeParams.topMargin = pagerBottomY - buttonHeight - padding;

                    enlargeButton.setLayoutParams(enlargeParams);
                    enlargeButton.setVisibility(View.VISIBLE);
                }
            });


            ImageColorAnalyzer.analyzePrimaryColor(requireContext(), track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
                @Override
                public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                    MusicInfoFragment.this.primaryColor = primaryColor;
                    MusicInfoFragment.this.selectedColor = selectedColor;
                    MusicInfoFragment.this.unselectedColor = unselectedColor;

                    Log.d(TAG, "Success to Analyze a Primary Color of Image");
                    Activity activity = getActivity();
                    if (activity != null) {
                        android.view.Window window = activity.getWindow();
                    }

                    if (bottomNavView == null) {
                        bottomNavView = requireActivity().findViewById(R.id.nav_view);
                    }
                    if (bottomNavView != null) {
                        bottomNavView.setBackgroundColor(primaryColor);
                        backButton.setBackgroundColor(primaryColor);
                        backButton.setColorFilter(selectedColor);
                        ImageButton emptySpace = requireActivity().findViewById(R.id.empty_space);
                        emptySpace.setBackgroundColor(primaryColor);
                    }

                    // 3. 상태에 따른 색상 목록(ColorStateList)을 생성합니다.
                    int[][] states = new int[][] {
                            new int[] { android.R.attr.state_checked },  // 선택된 상태
                            new int[] { -android.R.attr.state_checked }  // 선택되지 않은 상태
                    };

                    int[] colors = new int[] {
                            selectedColor,   // 선택됐을 때의 텍스트 색
                            unselectedColor  // 선택되지 않았을 때의 텍스트 색
                    };

                    android.content.res.ColorStateList textColorStateList = new android.content.res.ColorStateList(states, colors);
                    android.content.res.ColorStateList iconColorStateList = new android.content.res.ColorStateList(states, colors);

                    // 4. BottomNavigationView에 최종 적용합니다.
                    // BottomNavigationView 타입으로 캐스팅해야 관련 메서드를 쓸 수 있습니다.
                    if (bottomNavView instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                        com.google.android.material.bottomnavigation.BottomNavigationView bnv =
                                (com.google.android.material.bottomnavigation.BottomNavigationView) bottomNavView;

                        bnv.setItemTextColor(textColorStateList);
                        bnv.setItemIconTintList(iconColorStateList);
                    }
                }

                @Override
                public void onFailure() {
                    Log.d(TAG, "Fail to Analyze a Primary Color of Image");
                }
            });


            enlargeButton.setOnClickListener(v -> {
                //Shared Element Transition

                String transitionName = "Transition_music_to_image_detail_" + track.artworkUrl;

                ViewCompat.setTransitionName(artworkImage, transitionName);
                viewModel.setCurrentTransitionName(transitionName);

                ArrayList<String> imageUrls = new ArrayList<>();
                imageUrls.add(track.artworkUrl);
                int startPosition = 0;

                Bundle args = new Bundle();
                args.putString("transitionName", transitionName);
                args.putStringArrayList("image_urls", imageUrls);
                args.putInt("start_position", startPosition);

                FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                        .addSharedElement(artworkImage, transitionName)
                        .build();
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_musicInfoFragment_to_imageDetailFragment, args, null, extras);
            });


            // 1. 롱클릭을 감지할 GestureDetector 생성
            GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent event) {
                    // 롱클릭이 감지되었을 때 실행될 코드
                    // event 객체에서 화면 절대 좌표를 가져옵니다.
                    float touchX = event.getRawX();
                    float touchY = event.getRawY();

                    // 매니저를 호출하여 오버레이와 애니메이션을 표시합니다.
                    imageOverlayManager.showOverlay(artworkImage, track.artworkUrl, touchX, touchY);
                    imageOverlayManager.setNavBarColor(MusicInfoFragment.this.primaryColor, MusicInfoFragment.this.selectedColor, MusicInfoFragment.this.unselectedColor);
                }
            });

            // 2. ImageView에 OnTouchListener 설정
            artworkImage.setOnTouchListener((v, motionEvent) -> {
                // 모든 터치 이벤트를 GestureDetector에 전달합니다.
                gestureDetector.onTouchEvent(motionEvent);

                // true를 반환하여 이 이벤트가 여기서 처리되었음을 시스템에 알립니다.
                return true;
            });

            artistName.setText(track.artistName);
            artistName.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                favoriteArtistViewModel.loadFavoriteArtistByArtistId(track.artistId, loaded -> {
                    if (loaded != null){
                        bundle.putParcelable("favorite_artist", loaded);
                        NavController navController = NavHostFragment.findNavController(this);

                        //✅ 1. navigate() 호출 전에 현재 destination이 맞는지 검사
                        NavDestination currentDestination = navController.getCurrentDestination();
                        if (currentDestination != null && currentDestination.getId() == R.id.musicInfoFragment) {
                            navController.navigate(R.id.action_musicInfoFragment_to_artistInfoFragment, bundle);;
                        } else {
                            Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨");
                        }

                    }
                    else{
                        ArtistApiHelper apiHelper = new ArtistApiHelper(getContext(), requireActivity());
                        apiHelper.getArtist(null, track.artistId,  artist -> {
                            FavoriteArtist favoriteArtist = new FavoriteArtist(artist);
                            bundle.putParcelable("favorite_artist", favoriteArtist);
                            NavController navController = NavHostFragment.findNavController(this);

                            //✅ 1. navigate() 호출 전에 현재 destination이 맞는지 검사
                            NavDestination currentDestination = navController.getCurrentDestination();
                            if (currentDestination != null && currentDestination.getId() == R.id.musicInfoFragment) {
                                navController.navigate(R.id.action_musicInfoFragment_to_artistInfoFragment, bundle);;
                            } else {
                                Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨");
                            }
                        });
                    }
                });
            });

            albumName.setText(track.albumName);

            //music_info fragment to album_info fragment : shared element transition
            albumName.setOnClickListener(v -> {
                ArtistApiHelper apiHelper = new ArtistApiHelper(getContext(), requireActivity());
                apiHelper.getAlbum(null, track.albumId, album -> {
                    if (album != null) {
                        Bundle bundle = new Bundle();
                        String transitionName = "Transition_music_to_album" + track.artworkUrl;
                        viewModel.setCurrentTransitionName(transitionName);
                        artworkImage.setTransitionName(transitionName);
                        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                                .addSharedElement(artworkImage, artworkImage.getTransitionName())
                                        .build();
                        bundle.putString("transitionName", transitionName);

                        bundle.putParcelable("album", album);
                        NavController navController = NavHostFragment.findNavController(this);

                        //✅ 1. navigate() 호출 전에 현재 destination이 맞는지 검사
                        NavDestination currentDestination = navController.getCurrentDestination();
                        if (currentDestination != null && currentDestination.getId() == R.id.musicInfoFragment) {
                            navController.navigate(R.id.action_musicInfoFragment_to_albumInfoFragment, bundle, null, extras);
                        } else {
                            Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨");
                        }
                        //navController.navigate(R.id.action_musicInfoFragment_to_albumInfoFragment, bundle, null, extras);
                    }
                });
            });


            releaseDate.setText(track.releaseDate);
            int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
            durationMs.setText(durationSec/60 + "분 " + durationSec%60 + "초");
            Log.d("TraceId - artist ID", track.artistId);
            Log.d("TraceId - album ID", track.albumId);
            Log.d("TraceId - track ID", track.trackId);
        }
        else {
            Log.e("MusicInfoFragment", "track(Track) is null!");
        }

        ImageButton addButton = view.findViewById(R.id.addButton);

        if (savedInDb){
            addButton.setVisibility(TextView.GONE);
        }
        else {
            favoritesViewModel.loadFavoriteItem(track.trackId, loaded_ -> {
                if (loaded_ != null) {
                    savedInDb = true;
                    addButton.setVisibility(TextView.GONE);
                }
            });
        }

        addButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("관심목록에 추가")
                    .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            new Thread(() -> {
                                try {
                                    favoritesViewModel.insert(track, today);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                    });
                                } catch (SQLiteConstraintException e) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }).start();
                        }
                    })
                    .show();
        });



        trackTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Title", trackTitle.getText().toString());
                clipboard.setPrimaryClip(clip);
                return true;
            }
        });

        trackTitleKr.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Title", trackTitleKr.getText().toString());
                clipboard.setPrimaryClip(clip);
                return true;
            }
        });




    }

    private void switchTitleLanguage(){
        if (trackTitle.getVisibility() == TextView.VISIBLE  && favorite.metadata != null && favorite.metadata.title != null && !favorite.metadata.title.isEmpty()){
            trackTitle.setVisibility(TextView.GONE);
            trackTitleKr.setVisibility(TextView.VISIBLE);
        }
        else if (!track.trackName.isEmpty()){
            trackTitleKr.setVisibility(TextView.GONE);
            trackTitle.setVisibility(TextView.VISIBLE);
        }
    }


}
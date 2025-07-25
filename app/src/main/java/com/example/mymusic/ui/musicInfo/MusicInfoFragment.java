package com.example.mymusic.ui.musicInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.NavDestination;
import androidx.transition.ArcMotion;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeTransform;

import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionSet;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;

import com.example.mymusic.MainActivityViewModel;
import com.example.mymusic.R;
import com.example.mymusic.databinding.FragmentMusicInfoBinding;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.network.ArtistMetadataService;
import com.example.mymusic.simpleArtistInfo.SimpleArtistDialogHelper;
import com.example.mymusic.ui.albumInfo.AlbumInfoFragment;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.example.mymusic.util.DateFormatMismatchException;
import com.example.mymusic.util.DateUtils;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.ImageOverlayManager;
import com.example.mymusic.util.MyColorUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@UnstableApi
public class MusicInfoFragment extends Fragment {
    private final String TAG = "MusicInfoFragment";
    public static final String REQUEST_BOTTOM_SHEET = "request_bottom_sheet";
    public static final String ARGUMENTS_KEY = "favorite";
    public static final String TRANSITION_NAME_KEY = "transitionName";
    public static final String TRANSITION_NAME_FORM_TITLE = "title_name_";
    public static final String TRANSITION_NAME_FORM_ARTIST = "artist_name_";
    public static final String TRANSITION_NAME_FORM_ALBUM = "album_name_";
    public static final String TRANSITION_NAME_FORM_DURATION = "duration_";
    public static final String TRANSITION_NAME_FORM_RELEASE_DATE = "release_date_";
    public static final String TRANSITION_NAME_FORM_ARTWORK_IMAGE = "artwork_image_";
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
    private TextView daysBetween;
    private FragmentMusicInfoBinding binding;
    private MusicInfoViewModel viewModel;
    private MainActivityViewModel mainActivityViewModel;
    public static final String REQUEST_KEY = "music_info_fragment_request";
    public static final String BUNDLE_KEY_TRANSITION_END = "transition_track_artwork_ended";
    private LinkedHashMap<String, ArtistMetadata> artistMetadataMap;
    private  SimpleArtistDialogHelper dialogHelper;
    private com.google.android.material.bottomnavigation.BottomNavigationView bnv;
    private ImageButton containingPlaylistButton, playtimeButton, lyricsButton, playButton;
    private final ArcMotion arc = new ArcMotion();


    private TransitionSet buildTextTransition(String targetName) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);
        set.addTransition(new ChangeBounds());
        set.addTransition(new ChangeTransform()); // scale/rotate
        set.setPathMotion(arc);
        set.setDuration(435L);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addTarget(targetName); // transitionName
        return set;
    }

    private TransitionSet buildImageTransition(String targetName) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);
        set.addTransition(new ChangeBounds());
        set.addTransition(new ChangeTransform());
        set.setPathMotion(new ArcMotion());
        set.setDuration(350L);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addTarget(targetName);
        return set;
    }

    private void prepareTransition(String trackId) {
        // arc 설정 (Kotlin의 apply 블록)
        arc.setMinimumHorizontalAngle(60f); // 좌우 곡률 최소 각도
        arc.setMinimumVerticalAngle(80f);   // 상하 곡률 최소 각도
        arc.setMaximumAngle(90f);           // 전체 곡률 최대 각도
        // playlistId 타입에 맞게 문자열로

        TransitionSet nameTrans     = buildTextTransition(TRANSITION_NAME_FORM_TITLE + trackId);
        TransitionSet artistTrans    = buildTextTransition(TRANSITION_NAME_FORM_ARTIST + trackId);
        TransitionSet albumTrans = buildTextTransition(TRANSITION_NAME_FORM_ALBUM + trackId);
        TransitionSet durationTrans = buildTextTransition(TRANSITION_NAME_FORM_DURATION + trackId);
        TransitionSet releaseDateTrans = buildTextTransition(TRANSITION_NAME_FORM_RELEASE_DATE + trackId);
        TransitionSet imageTrans    = buildImageTransition(TRANSITION_NAME_FORM_ARTWORK_IMAGE + trackId);

        TransitionSet enter = new TransitionSet();
        enter.setOrdering(TransitionSet.ORDERING_TOGETHER);
        enter.addTransition(nameTrans);
        enter.addTransition(artistTrans);
        enter.addTransition(albumTrans);
        enter.addTransition(durationTrans);
        enter.addTransition(releaseDateTrans);
        enter.addTransition(imageTrans);

        setSharedElementEnterTransition(enter);



        // clone()의 반환형은 Transition
        Transition returnTrans = enter.clone();
        setSharedElementReturnTransition(returnTrans);

        // ✅ 2. 나머지 뷰를 위한 전환 설정
        setEnterTransition(
                new Slide(Gravity.BOTTOM).
                        setDuration(450L)
        );

        setReturnTransition(
                new Fade().setDuration(50L)
        );


        // ✅ 3. transition end (Fragment) callback
        androidx.transition.Transition returnTransition = (androidx.transition.Transition) getSharedElementReturnTransition();
        if (returnTransition != null){
            returnTransition.addListener(new androidx.transition.Transition.TransitionListener() {
                @Override
                public void onTransitionStart(@NonNull androidx.transition.Transition transition) {}

                @Override
                public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                    if (isRemoving()) {
                        Log.d(TAG, "Transition end");
                        Bundle result = new Bundle();
                        result.putBoolean(BUNDLE_KEY_TRANSITION_END, true);
                        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                        transition.removeListener(this);
                    }
                }
                @Override
                public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {}
                @Override
                public void onTransitionPause(@NonNull androidx.transition.Transition transition) {}
                @Override
                public void onTransitionResume(@NonNull androidx.transition.Transition transition) {}
            });
        }
    }
    //ViewModel 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);


        viewModel = new ViewModelProvider(requireActivity()).get(MusicInfoViewModel.class);



        // ✅ 1. 전환 애니메이션 종류를 여기서 설정합니다.
        //MaterialContainerTransform transform = new MaterialContainerTransform();
        //transform.setPathMotion(new MaterialArcMotion());
        //transform.setDuration(500);
        //transform.setInterpolator(new FastOutSlowInInterpolator());
        //setSharedElementEnterTransition(transform);
        //setSharedElementReturnTransition(transform);







        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");
        if (viewModel.isOnSimpleDialog()) {
            Log.d(TAG, "is on simple dialog state");

            int lastPosition = viewModel.getSimpleArtistDialogPosition();
            List<FavoriteArtist> favoriteArtistList = viewModel.getFavoriteArtistList();
            GradientDrawable lastGradient = viewModel.getLastGradient();
            int visibleState =  viewModel.getDetailVisibleStateOnDialog();
            Log.d(TAG, "visible state saved in viewmodel " + visibleState);

            safeShowDialog(getActivity(), lastPosition, visibleState, favoriteArtistList, lastGradient);
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        restoreBottomNavColor();
        setEnterTransition(null);
    }

    private void restoreBottomNavColor(){
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int selectedColor = prefs.getInt("selected_color", Color.GRAY); // 기본값 회색

        if (prefs.getBoolean("basic_color", true)){
            selectedColor = ContextCompat.getColor(requireContext(), R.color.textPrimary);
        }

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        int[] colors = new int[] {
                selectedColor,
                Color.GRAY
        };

        ColorStateList colorStateList = new ColorStateList(states, colors);

        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.nav_view);
        bottomNav.setItemIconTintList(colorStateList);
        bottomNav.setItemTextColor(colorStateList);
        bottomNav.setBackgroundColor(getResources().getColor(R.color.navBarBasic));
    }
    private void safeShowDialog(Activity activity, int lastPosition, int detailsVisibleState, List<FavoriteArtist> favoriteArtistList, GradientDrawable lastGradient){
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable tryShow = new Runnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    if (detailsVisibleState == SimpleArtistDialogHelper.OFF_DETAILS) {
                        dialogHelper.showArtistDialog(lastPosition, favoriteArtistList, lastGradient);
                    } else{
                        dialogHelper.showArtistDialogWithExpand(lastPosition, favoriteArtistList, lastGradient);
                    }
                } else if (attempts < 5) {
                    attempts ++;
                    handler.postDelayed(this, 100);
                } else {
                    Log.w(TAG, "Activity not ready. Dialog not shown.");
                }

            }
        };
        handler.post(tryShow);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMusicInfoBinding.inflate(inflater, container, false);
        savedInDb = false;
        return binding.getRoot();
    }



    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postponeEnterTransition();

        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                NavController nav = NavHostFragment.findNavController(MusicInfoFragment.this);
                boolean popped = nav.popBackStack();

                if (viewModel.requestBottomSheet) {
                    new Handler(Looper.getMainLooper()).post(
                            () -> mainActivityViewModel.requestBottomSheet(true)
                    );
                }
                if (!popped) requireActivity().finish(); // 더 이상 pop될 곳 없을 때

                viewModel.setMetadataMap(null);
                viewModel.setFavoriteArtistList(null);
                binding.artistName.setText(null);
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);

        mainActivityViewModel = new ViewModelProvider(getActivity()).get(MainActivityViewModel.class);

        bottomNavView = requireActivity().findViewById(R.id.nav_view);

        //사진 다운로드를 위한 매니저 객체 설정
        imageOverlayManager = new ImageOverlayManager(requireActivity(), view);
        imageOverlayManager.setScale(0);


        favorite = getArguments().getParcelable(ARGUMENTS_KEY);
        viewModel.requestBottomSheet = getArguments().getBoolean(REQUEST_BOTTOM_SHEET);

        viewModel.favorite = favorite;


        if (favorite == null)
            Log.d(TAG, "get Empty Favorite element");

        if (favorite != null && favorite.track != null) {
            track = favorite.track;
            prepareTransition(track.trackId);


            //ImageView artworkImage = view.findViewById(R.id.artworkImage);
            ImageView artworkImage = binding.artworkImage;



            String receivedTransitionName = getArguments().getString(TRANSITION_NAME_KEY);

            viewModel.setInitialTransitionName(receivedTransitionName);
            //ViewCompat.setTransitionName(binding.artworkImage, receivedTransitionName);
            Log.d(TAG, "onViewCreated 에서 '전달받은' 이름 설정, name=" + receivedTransitionName);




            artworkImage.post(() -> {
                artworkSize = artworkImage.getWidth();
                imageOverlayManager.setDownloadButtonLocation(0, - (int)((0.2)* artworkSize));

            });


            trackTitle = view.findViewById(R.id.trackTitle);
            TextView artistName = view.findViewById(R.id.artistName);
            TextView albumName = view.findViewById(R.id.albumName);

            trackTitleKr = view.findViewById(R.id.trackTitleKr);

            daysBetween = view.findViewById(R.id.days_between);
            addedDateLayout = view.findViewById(R.id.added_date_layout);
            addedDate = view.findViewById(R.id.added_date);
            ImageView enlargeButton = view.findViewById(R.id.enlarge_button);
            containingPlaylistButton = view.findViewById(R.id.containing_playlist);
            playtimeButton = view.findViewById(R.id.playtime);
            lyricsButton = view.findViewById(R.id.lyrics);
            playButton = view.findViewById(R.id.play_music);


            String trackId = track.trackId;
            artistName.setTransitionName(TRANSITION_NAME_FORM_ARTIST + trackId);
            albumName.setTransitionName(TRANSITION_NAME_FORM_ALBUM + trackId);
            binding.durationLayout.setTransitionName(TRANSITION_NAME_FORM_DURATION + trackId);
            binding.releaseDateLayout.setTransitionName(TRANSITION_NAME_FORM_RELEASE_DATE + trackId);
            if(viewModel.getCurrentTransitionName() == null || viewModel.getCurrentTransitionName().equals(viewModel.getInitialTransitionName()))
                artworkImage.setTransitionName(TRANSITION_NAME_FORM_ARTWORK_IMAGE + trackId);




            trackTitle.setOnClickListener(v -> {
                if (savedInDb)
                    switchTitleLanguage();
                else{
                    new Thread(()->{
                        Favorite loaded =  favoritesViewModel.repository.getFavoriteIncludeHidden(trackId);
                        if (loaded != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                               savedInDb = true;
                               switchTitleLanguage();
                            });
                        }
                    }).start();
                }
            });
            trackTitleKr.setOnClickListener(v -> switchTitleLanguage());




            if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
                Context context = getContext();
                if (context != null) {
                    Glide.with(context)
                            .load(track.artworkUrl)
                            .dontAnimate()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .error(R.drawable.ic_image_not_found_foreground) // 실패 시 이미지
                            .centerCrop()
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition <? super Drawable> transition) {
                                    artworkImage.setImageDrawable(resource);
                                    startPostponedEnterTransition();
                                    artworkImage.post(() -> {
                                        artworkImage.setTransitionName(viewModel.getInitialTransitionName());
                                        Log.d(TAG, "TransitionName set to initial transName: " + viewModel.getInitialTransitionName() );
                                    });

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
                startPostponedEnterTransition();
            }

            artworkImage.post(()->{
                    enlargeButton.setVisibility(View.VISIBLE);
                    ImageView enlargeButtonShadow =  binding.enlargeButtonShadow;
                    enlargeButtonShadow.setVisibility(View.VISIBLE);
            });


            ImageColorAnalyzer.analyzePrimaryColor(requireContext(), track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
                @Override
                public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                    if (favorite.addedDate != null && !favorite.addedDate.isEmpty() && favorite.track.primaryColor == null) {
                        favorite.track.primaryColor = primaryColor;
                        new Thread(() -> {
                            favoritesViewModel.updateFavoriteSong(favorite, new FavoritesViewModel.OnFavoriteViewModelCallback() {
                                @Override
                                public void onSuccess() {}

                                @Override
                                public void onFailure() {}
                            });
                        }).start();
                    }

                    //MusicInfoFragment.this.primaryColor = primaryColor;
                    //MusicInfoFragment.this.selectedColor = selectedColor;
                    //MusicInfoFragment.this.unselectedColor = unselectedColor;

                    Log.d(TAG, "Success to Analyze a Primary Color of Image");
                    Activity activity = getActivity();
                    if (activity != null) {
                        android.view.Window window = activity.getWindow();


                        if (bottomNavView == null) {
                            bottomNavView = activity.findViewById(R.id.nav_view);
                        }
                        if (bottomNavView != null) {
                            bottomNavView.setBackgroundColor(primaryColor);
                        }
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
                        bnv = (com.google.android.material.bottomnavigation.BottomNavigationView) bottomNavView;

                        //originalTextColorStateList = bnv.getItemTextColor();
                        //originalIconColorStateList = bnv.getItemIconTintList();

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
                    //imageOverlayManager.setNavBarColor(MusicInfoFragment.this.primaryColor, MusicInfoFragment.this.selectedColor, MusicInfoFragment.this.unselectedColor);
                }
            });

            // 2. ImageView에 OnTouchListener 설정
            artworkImage.setOnTouchListener((v, motionEvent) -> {
                // 모든 터치 이벤트를 GestureDetector에 전달합니다.
                gestureDetector.onTouchEvent(motionEvent);

                // true를 반환하여 이 이벤트가 여기서 처리되었음을 시스템에 알립니다.
                return true;
            });


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
                        ArtistApiHelper apiHelper = new ArtistApiHelper(getContext());
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


            //music_info fragment to album_info fragment : shared element transition
            albumName.setOnClickListener(v -> {
                ArtistApiHelper apiHelper = new ArtistApiHelper(getContext());
                apiHelper.getAlbum(null, track.albumId, album -> {
                    if (album != null) {
                        Bundle bundle = new Bundle();
                        String transitionName = "Transition_music_to_album" + track.artworkUrl;
                        viewModel.setCurrentTransitionName(transitionName);
                        artworkImage.setTransitionName(transitionName);
                        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                                .addSharedElement(artworkImage, artworkImage.getTransitionName())
                                        .build();
                        bundle.putString(AlbumInfoFragment.TRANSITION_NAME_KEY, transitionName);

                        bundle.putParcelable(AlbumInfoFragment.ARGUMENTS_KEY, album);
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
        }
        else {
            Log.e("MusicInfoFragment", "track(Track) is null!");
        }

        ImageButton addButton = view.findViewById(R.id.addButton);

        if (savedInDb){
            addButton.setVisibility(TextView.GONE);
        }
        else {
            new Thread(()->{
                Favorite loaded =  favoritesViewModel.repository.getFavoriteIncludeHidden(track.trackId);
                if (loaded != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        viewModel.favorite = loaded;
                        favorite = loaded;
                        bind();
                        savedInDb = true;
                        addButton.setVisibility(TextView.GONE);
                    });
                }
            }).start();
        }

        addButton.setOnClickListener(v -> {
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_custom);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);

            TextView cancelButton = dialog.findViewById(R.id.cancel_button);
            TextView confirmButton = dialog.findViewById(R.id.confirm_button);
            confirmButton.setText("확인");

            TextView subText = dialog.findViewById(R.id.subtext);
            TextView title = dialog.findViewById(R.id.title);
            title.setText("관심목록에 추가");
            subText.setText(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?");
            cancelButton.setOnClickListener(v1 -> dialog.dismiss());

            confirmButton.setOnClickListener(v1 -> {
                dialog.dismiss();
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                new Thread(() -> {
                    try {
                        favoritesViewModel.insert(track, today);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            addButton.setVisibility(View.GONE);
                        });
                    } catch (SQLiteConstraintException e) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            });
            dialog.show();
        });


/*
        trackTitle.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Title", trackTitle.getText().toString());
            clipboard.setPrimaryClip(clip);
            return true;
        });

        trackTitleKr.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Title", trackTitleKr.getText().toString());
            clipboard.setPrimaryClip(clip);
            return true;
        });

 */

        containingPlaylistButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.containing_playlist_fragment_in_music_info);
        });

        lyricsButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.lyrics_fragment_in_music_info);
        });

        playtimeButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.playtime_calendar_fragment_in_music_info);
        });


        playButton.setOnClickListener(v -> {
            if (favorite.audioUri != null) {
                mainActivityViewModel.setPlaylist(List.of(favorite), 0);
            }
        });


        bind();
        /**
         end of onViewCreated
         * */
    }

    private void bind() {
        TrackMetadata metadata = favorite.metadata;
        String trackId = favorite.track.trackId;

        binding.trackTitle.setText(track.trackName);
        if (metadata != null && metadata.title != null){
            binding.trackTitleKr.setText(metadata.title);
            binding.trackTitle.setVisibility(TextView.GONE);
            binding.trackTitleKr.setVisibility(TextView.VISIBLE);
            binding.trackTitleKr.setTransitionName("title_name_" + trackId);
        }
        else {
            binding.trackTitleKr.setVisibility(TextView.GONE);
            binding.trackTitle.setVisibility(TextView.VISIBLE);
            binding.trackTitle.setTransitionName("title_name_" + trackId);
        }

        try {
            binding.daysBetween.setText(String.valueOf(DateUtils.calculateDateDiffrence(track.releaseDate, DateUtils.today())));
        }catch(DateFormatMismatchException e){
            Log.d(TAG, e.getMessage());
            LinearLayout daysBetweenLayout = binding.daysBetweenLayout;
            daysBetweenLayout.setVisibility(View.GONE);
        }
        binding.releaseDate.setText(track.releaseDate);
        int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
        binding.durationMs.setText(durationSec/60 + "분 " + durationSec%60 + "초");


        if (metadata != null && metadata.lyricists != null){
            binding.lyricists.setText(String.join(", ", metadata.lyricists));
            binding.lyricistsLayout.setVisibility(TextView.VISIBLE);
        }

        if (metadata != null && metadata.composers != null){
            binding.composers.setText(String.join(", ", metadata.composers));
            binding.composersLayout.setVisibility(TextView.VISIBLE);
        }

        if (favorite.addedDate != null && !favorite.addedDate.isEmpty()){
            binding.addedDate.setText(favorite.addedDate);
            binding.addedDateLayout.setVisibility(View.VISIBLE);
        }


        if (metadata != null && metadata.vocalists != null && !metadata.vocalists.isEmpty()){
            binding.vocalists.setText(metadata.vocalistsToString());
            binding.vocalistsLayout.setVisibility(TextView.VISIBLE);
            dialogHelper = new SimpleArtistDialogHelper(getContext(), new ArrayList<>());

            Log.d(TAG, "dialogHelper create with empty ArrayList");

            dialogHelper.setPositionChangedListener(new SimpleArtistDialogHelper.OnPositionChangedListener() {
                @Override
                public void positionChanged(int position, int detail_visible_state, GradientDrawable currentGradient) {
                    Log.d(TAG, "position changed callback received detail visible state is " + detail_visible_state);
                    viewModel.setSimpleArtistDialogPosition(position);
                    viewModel.setDetailVisibleStateOnDialog(detail_visible_state);
                    viewModel.setLastGradient(currentGradient);
                }
            });

            dialogHelper.setDismissListener(new SimpleArtistDialogHelper.OnDialogDismissListener() {
                @Override
                public void dialogDismissed() {
                    viewModel.setOnSimpleDialog(false);
                }
            });




            List<FavoriteArtist> favoriteArtistListSavedInViewModel = viewModel.getFavoriteArtistList();
            if (favoriteArtistListSavedInViewModel != null && !favoriteArtistListSavedInViewModel.isEmpty()) {
                dialogHelper.updateList(favoriteArtistListSavedInViewModel);
                Log.d(TAG, "dialogHelper updateList, list size: " + favoriteArtistListSavedInViewModel.size());
                artistMetadataMap = viewModel.getMetadataMap();
                setClickableArtists( binding.vocalists,  binding.vocalists.getText().toString(), artistMetadataMap);
            }
            else {

                artistMetadataMap = new LinkedHashMap<>();
                for (List<String> item : metadata.vocalists){
                    if (item.get(1) != null) {
                        artistMetadataMap.put(item.get(0), null);
                    }
                }

                fetchArtistMetadata(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "runnable starts, setClickableArtists in string and set dialogHelper");
                        setClickableArtists( binding.vocalists,  binding.vocalists.getText().toString(), artistMetadataMap);

                        List<FavoriteArtist> faList = new ArrayList<>();


                        for (String key : artistMetadataMap.keySet()) {
                            ArtistMetadata value = artistMetadataMap.get(key);

                            Artist artist = new Artist(key);
                            FavoriteArtist fa = new FavoriteArtist(artist, null, value);
                            faList.add(fa);
                        }

                        viewModel.setFavoriteArtistList(faList);
                        viewModel.setMetadataMap(artistMetadataMap);

                        dialogHelper.updateList(faList);
                    }
                });
            }
        }


        binding.artistName.setText(track.artistName);
        binding.albumName.setText(track.albumName);

        if (favorite.audioUri == null) {
            playtimeButton.setVisibility(View.GONE);
            containingPlaylistButton.setVisibility(View.GONE);
            playButton.setVisibility(View.GONE);
        }
        else {
            playtimeButton.setVisibility(View.VISIBLE);
            containingPlaylistButton.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.VISIBLE);
        }
        if (favorite.metadata == null || favorite.metadata.lyrics == null || favorite.metadata.lyrics.isEmpty()) {
            lyricsButton.setVisibility(View.GONE);
        }
        else {
            lyricsButton.setVisibility(View.VISIBLE);
        }
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


    public void fetchArtistMetadata(Runnable onAllFinished) {
        List<List<String>> vocalists = favorite.metadata.vocalists;
        int vocalistSize = vocalists.size();
        for (List<String> item : vocalists) {
            String vibeId = item.get(1);
            if (vibeId == null)
                vocalistSize --;
        }
        int total = vocalistSize;

        AtomicInteger completedCount = new AtomicInteger(0);

        for (List<String> item : vocalists) {
            String name = item.get(0);
            String vibeId = item.get(1);

            if (vibeId == null){
                continue;
            }

            WebView tempWebView = new WebView(getContext().getApplicationContext());
            ArtistMetadataService.fetchMetadata(tempWebView, vibeId, new ArtistMetadataService.MetadataCallback() {
                @Override
                public void onSuccess(ArtistMetadata metadata) {
                    new Handler(Looper.getMainLooper()).post(()->{
                        Log.e(TAG, "success to fetch artist metadata for " + name);
                        artistMetadataMap.put(name, metadata);
                        checkAllDone();
                    });
                }

                @Override
                public void onFailure(String reason) {
                    new Handler(Looper.getMainLooper()).post(()->{
                        Log.e(TAG, "fail to fetch artist metadata for " + name);
                        checkAllDone();
                    });

                }

                private void checkAllDone() {
                    if (completedCount.incrementAndGet() == total) {
                        Log.d(TAG, "✅ All artist metadata fetched.");
                        onAllFinished.run(); // 모든 fetch 완료 시 콜백 실행
                    }
                }
            });
        }

        // 예외 처리: 아무 것도 없을 경우 즉시 완료
        if (total == 0) {
            onAllFinished.run();
        }
    }


    private void setClickableArtists(TextView textView, String artists, LinkedHashMap<String, ArtistMetadata> artistMap) {
        SpannableString spannable = new SpannableString(artists);
        int start = 0;

        String[] artistArray = artists.split(",\\s*"); // 쉼표+공백 기준으로 split

        for (String artist : artistArray) {
            int index = artists.indexOf(artist, start);
            int end = index + artist.length();
            start = end;

            boolean noVibeId = false;

            for (List<String> item : favorite.metadata.vocalists){
                if (item.get(0).equals(artist) && item.get(1) == null){
                    noVibeId = true;
                }
            }
            if (noVibeId) continue;


            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    ArtistMetadata metadata = artistMap.get(artist);
                    if (metadata != null) {
                        Log.w("ArtistClick", "click event occurred: " + artist);
                        int position = MusicInfoFragment.indexOfKey(artistMap, artist);

                        dialogHelper.showArtistDialogFirstTime(position);
                        viewModel.setOnSimpleDialog(true);
                    } else {
                        Log.w("ArtistClick", "No data for: " + artist);
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false); // 밑줄 제거
                    Context context = getContext();

                    int selectedColor = MyColorUtils.getMyPrefColor(context);

                    ds.setColor(selectedColor);    // 클릭 가능한 색상
                }
            };

            spannable.setSpan(clickableSpan, index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT); // 클릭 시 배경 투명
    }

    public static <K, V> int indexOfKey(LinkedHashMap<String, ArtistMetadata> map, String targetKey) {
        int index = 0;
        for (String key : map.keySet()) {
            if (key.equals(targetKey)) {
                return index;
            }
            index++;
        }
        return -1; // key가 없는 경우
    }



}
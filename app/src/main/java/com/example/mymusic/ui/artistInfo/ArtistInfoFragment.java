   package com.example.mymusic.ui.artistInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mymusic.R;
import com.example.mymusic.adapter.SimpleAlbumAdapter;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL2;
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter;
import com.example.mymusic.data.repository.ArtistMetadataRepository;
import com.example.mymusic.databinding.FragmentArtistInfoBinding;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.network.ArtistMetadataService;
import com.example.mymusic.simpleArtistInfo.SimpleArtistDialogHelper;
import com.example.mymusic.ui.imageDetail.ImageDetailFragment;
import com.example.mymusic.ui.musicInfo.MusicInfoFragment;
import com.example.mymusic.ui.webView.photoWebView.WebViewFragment;
import com.example.mymusic.util.ImageOverlayManager;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtistInfoFragment extends Fragment implements ImagePagerAdapter.OnImageLongClickListener{
    private final String TAG = "ArtistInfoFragment";
    public static int ARTIST_ARTWORK_SIZE = 480;

    private FavoriteArtistViewModel favoriteArtistViewModel;
    private FavoritesViewModel favoritesViewModel;
    private FavoriteArtist favoriteArtist;

    public static final String ARGUMENTS_KEY = "favorite_artist";
    private Artist artist;
    private TextView artistNameTextView, genresTextView, followersTextView;
    private RecyclerView albumRecyclerView, trackRecyclerView;
    private ImageButton addArtistButton;
    private LinearLayout debutLayout, activityYearsLayout, membersLayout, agencyLayout, activityLayout, genresLayout, followersLayout, biographyLayout, addedDataLayout;
    private TextView debutTextView, activityYearsTextView, membersTextView, agencyTextView, activityTextView, biographyTextView, addedDateTextView;


    private ViewPager2 pager;
    private List<String> imageUrls;
    private List<List<String>> activities;
    private ImagePagerAdapter pageAdapter;
    private ImageView enlargeButton;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavView;
    private android.graphics.drawable.Drawable originalBottomNavBackground;
    private ImageOverlayManager imageOverlayManager;
    private ArtistInfoViewModel viewModel;
    private FragmentArtistInfoBinding binding;
    private TrackAdapter trackAdapter;
    private SimpleAlbumAdapter albumAdapter;
    private AtomicInteger readyCounter = new AtomicInteger(0);

    public static final String REQUEST_KEY = "artist_info_fragment_request";
    public static final String BUNDLE_KEY_TRANSITION_END = "transition_artist_artwork_ended";
    private CardView imageFetchButton, setRepresentativeImageButton;
    LinearLayout imageLongClickOverlay;

    private static final long AUTO_SLIDER_DELAY_TIME = 5000L;
    private static final long AUTO_SLIDER_DELAY_TIME_AFTER_USER_SCROLL = 7500L;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    AtomicBoolean isUserScrolling = new AtomicBoolean(false);
    AtomicInteger lastConfirmedPosition = new AtomicInteger(0);
    AtomicInteger selectedPosition = new AtomicInteger(0);
    private Boolean shouldNavBack = false;
    private int positionDiff = 0;
    private Context viewGroupContext;
    private NestedScrollView scrollView;
    private LinkedHashMap<String, ArtistMetadata> artistMetadataMap;
    private SimpleArtistDialogHelper dialogHelper;

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int horizontalPadding = Math.max(100 ,(int) (screenWidth * 0.0675f)); // 6% 패딩
        pager.setPadding(horizontalPadding, pager.getPaddingTop(), horizontalPadding, pager.getPaddingBottom());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        viewModel = new ViewModelProvider(this).get(ArtistInfoViewModel.class);

        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));

        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setPathMotion(new MaterialArcMotion());
        setSharedElementEnterTransition(transform);
        setSharedElementReturnTransition(transform);


        //ImageDetailFragment to ArtistFragment
        getParentFragmentManager().setFragmentResultListener(ImageDetailFragment.REQUEST_KEY_POSITION, this, (requestKey, bundle) -> {
            if (requestKey.equals(ImageDetailFragment.REQUEST_KEY_POSITION)){
                int position = bundle.getInt("position", 0);
                viewModel.setLastPositionAtImageDetailFragment(position);
                Log.d(TAG, "Receive position information from ImageDetailFragment and save position " + position + " at viewModel");
            }
        });


        //ImageDetailFragment to ArtistFragment
        getParentFragmentManager().setFragmentResultListener(ImageDetailFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            // B 프래그먼트에서 보낸 키와 일치하는지 확인
            if (requestKey.equals(ImageDetailFragment.REQUEST_KEY)) {
                boolean transitionEnded = bundle.getBoolean(ImageDetailFragment.BUNDLE_KEY_TRANSITION_END);
                if (transitionEnded) {
                    Log.d(TAG, "Return transition from DetailsFragment has ended.");
                    // ⭐ B에서 A로의 복귀 전환이 완전히 끝난 시점! ⭐
                    // 여기에 원하는 작업을 구현하면 됩니다.
                    int position = 0;
                    RecyclerView recyclerView = (RecyclerView) pager.getChildAt(0);
                    ImagePagerAdapter.ImageViewHolder holder = (ImagePagerAdapter.ImageViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                    if (holder == null) {
                        Log.d(TAG, "holder is null, cannot find imageView");
                        return;
                    }
                    ImageView imageView = holder.imageView; // 어댑터의 ViewHolder에 있는 이미지 뷰
                    Log.d(TAG, "image view connected");
                    ViewCompat.setTransitionName(imageView, viewModel.getInitialTransitionName());
                    Log.d(TAG, "set first imageview TransitionName to initial transition name");

                }
            }
        });



        getParentFragmentManager().setFragmentResultListener(WebViewFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (requestKey.equals(WebViewFragment.REQUEST_KEY)) {
                List<String> fetchedImageUrls = bundle.getStringArrayList(WebViewFragment.BUNDLE_KEY_IMAGE_URLS);
                Log.d(TAG, "Images Length: " + fetchedImageUrls.size()/2);

                ArtistMetadataRepository metadataRepository = new ArtistMetadataRepository(getContext());
                new Thread(() -> {
                    long result = metadataRepository.updateImagesBySpotifyId(artist.artistId, fetchedImageUrls);
                    if (result > 0) {
                        Log.d(TAG, "save additional artist images Success");
                        favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
                            @Override
                            public void onSuccess(ArtistMetadata metadata) {
                                if (metadata != null && metadata.images != null && !metadata.images.isEmpty()) {
                                    requireActivity().runOnUiThread(() -> {
                                        // 중복 방지하면서 순서 유지
                                        for (String url : metadata.images) {
                                            if (!imageUrls.contains(url)) {
                                                imageUrls.add(url);
                                            }
                                        }

                                        // 어댑터에 갱신 반영
                                        pageAdapter.notifyDataSetChanged();  // or create a new adapter if this doesn't work well

                                        Toast.makeText(getContext(), "새 이미지가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                        sliderHandler.removeCallbacks(sliderRunnable);
                                        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
                                    });
                                }
                            }

                            @Override
                            public void onFailure(String reason) {
                                Log.d(TAG, "Fail to load Artist Metadata due to : " + reason);
                            }
                        });
                    }
                }).start();
            }
        });

        // ArtistInfoFragment -> FavoritesFragment transition end callback
        Transition returnTransition = (Transition) getSharedElementReturnTransition();
        if (returnTransition != null){
            returnTransition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(@NonNull Transition transition) {}

                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        binding = FragmentArtistInfoBinding.inflate(inflater, container, false);
        viewGroupContext = container.getContext();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        readyCounter.set(0);
        //전환 연기
        postponeEnterTransition();


        albumRecyclerView = binding.albumResultRecyclerView;
        albumRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        trackRecyclerView = binding.trackResultRecyclerView;
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//adapter 정의
        trackAdapter = new TrackAdapter(new ArrayList<>(), getContext(), this::showTrackDetails, this::addFavoriteSong, this::onTrackClick);
        trackRecyclerView.setAdapter(trackAdapter);
        Log.d(TAG, "Empty Track adapter setting Done");
        trackRecyclerView.setNestedScrollingEnabled(false);

        albumAdapter = new SimpleAlbumAdapter(new ArrayList<>(), this::onAlbumClick);
        albumRecyclerView.setAdapter(albumAdapter);
        Log.d(TAG, "Empty Album adapter setting Done");
        albumRecyclerView.setNestedScrollingEnabled(false);

        scrollView = view.findViewById(R.id.parent_scroll_container);

        imageOverlayManager = new ImageOverlayManager(requireActivity(), view);

        // ✅ BottomNavigationView를 액티비티에서 찾아와 원래 배경을 저장
        bottomNavView = requireActivity().findViewById(R.id.nav_view);
        if (bottomNavView != null) {
            originalBottomNavBackground = bottomNavView.getBackground();
        }

        bindView(view);
        parseArgs();
        loadArtistMetadata();
        loadTopTracks();
        loadAlbums();

        addArtistButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("관심목록에 추가")
                    .setMessage(artist.artistName + " 을(를) Favorites List 에 추가할까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            new Thread(() -> {
                                try {
                                    favoriteArtistViewModel.insert(artist, today, result -> {
                                        if (result > 0){
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), artist.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                        else{
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "Artist 추가 실패, 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                                } catch (SQLiteConstraintException e) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), artist.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }).start();

                        }
                    })
                    .show();
        });


        sliderRunnable = () -> {
          if (pager != null && pageAdapter != null){
              int nextItem = (pager.getCurrentItem() + 1) % pageAdapter.getItemCount();
              pager.setCurrentItem(nextItem, true);
              sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
          }
        };


        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        int scrollY = scrollView.getScrollY();
                        int artworkHeight = getResources().getDimensionPixelSize(R.dimen.artwork_height_in_view_pager);
                        Log.d("scroll check", "test, scroll Y: " + scrollY + " artwork height " + artworkHeight);
                        if (scrollY > (int) (0.75 * artworkHeight)){
                            pager.setCurrentItem(0, false);
                            NavHostFragment.findNavController(ArtistInfoFragment.this).popBackStack();
                        }
                        else{
                            shouldNavBack = true;
                            positionDiff = pager.getCurrentItem();
                            if (positionDiff == 0){
                                NavHostFragment.findNavController(ArtistInfoFragment.this).popBackStack();
                            }
                            pager.setCurrentItem(0, true);
                        }
                    }
                }
        );


        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position){ //페이지 전환 감지(페이지가 바꼈을 때만 호출)
                Log.d("PageChangedDetect", "page changed: " + position);
                lastConfirmedPosition.set(selectedPosition.get());
                selectedPosition.set(position);
            }

            @Override
            public void onPageScrollStateChanged(int state){
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING){ //사용자 터치 감지
                    Log.d("SlideDetect", "사용자 터치 시작!");
                    // 사용자 터치 시작
                    sliderHandler.removeCallbacks(sliderRunnable);
                    isUserScrolling.set(true);
                }
                else if (state == ViewPager2.SCROLL_STATE_IDLE){ //스크롤 정지 상태 감지
                    Log.d("SlideDetect", "스크롤 정지!");

                    if (shouldNavBack){
                       NavHostFragment.findNavController(ArtistInfoFragment.this).popBackStack();
                    }

                    if (isUserScrolling.get()){
                        Log.d("SlideDetect", "사용자 스크롤 감지!");
                        isUserScrolling.set(false);

                        int newPos = selectedPosition.get();
                        int prevPos = lastConfirmedPosition.get();
                        Log.d(TAG, "previous position: (" + prevPos + "), new position: (" + newPos + ")");
                        if (newPos != prevPos) {
                            lastConfirmedPosition.set(newPos);
                            sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME_AFTER_USER_SCROLL);
                            Log.d("Slide Handler", "slide speed set to " + AUTO_SLIDER_DELAY_TIME_AFTER_USER_SCROLL);
                        }else{
                            sliderHandler.postDelayed(sliderRunnable, 10000L);
                            Log.d("Slide Handler", "slide speed set to " + 10000L);
                        }
                    } else{ // 사용자 scroll 이 아니면 기본 속도로 setting
                        sliderHandler.removeCallbacks(sliderRunnable);
                        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
                        Log.d("Slider Handler", "slide speed set to " + AUTO_SLIDER_DELAY_TIME);
                    }
                }
            }
        });



        //end of on view created
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity activity = requireActivity();
        if (activity != null){
            //keep screen off
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        }
        sliderHandler.removeCallbacks(sliderRunnable);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        Activity activity = requireActivity();
        if (activity != null){
            //keep screen on
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }


        if (viewModel.isOnSimpleDialog()) {
            Log.d(TAG, "is on simple dialog state");

            int lastPosition = viewModel.getSimpleArtistDialogPosition();
            List<FavoriteArtist> favoriteArtistList = viewModel.getFavoriteArtistList();
            GradientDrawable lastGradient = viewModel.getLastGradient();
            int visibleState =  viewModel.getDetailVisibleStateOnDialog();
            Log.d(TAG, "visible state saved in viewmodel " + visibleState);

            safeShowDialog(getActivity(), lastPosition, visibleState, favoriteArtistList, lastGradient);
        }

        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
        Log.d("Slide Handler", "slide speed set to " + AUTO_SLIDER_DELAY_TIME);
    }



    private void parseArgs() {
        String transitionName = getArguments().getString("transitionName");
        if (viewModel.isFirstFragmentCreation() && transitionName == null){
            Log.d(TAG, "receive null transitionName, consider no shared element transition in this case AND startPostponedEnterTransition() at FIRST CREATION of fragment");
            viewModel.setFirstFragmentCreation(false);
            startPostponedEnterTransition();
            return;
        } else if(!viewModel.isFirstFragmentCreation() && transitionName == null && !viewModel.isSecondPostponeFlag()){
            Log.d(TAG, "receive null transitionName, consider no shared element transition in this case AND startPostponedEnterTransition() at reenter to fragment");
            return;
        } else if(viewModel.isSecondPostponeFlag()){
            //handleReenterTransitionFromImageDetail();
        }

        String transitionNameForm = getArguments().getString("transitionNameForm");
        int initialRecyclerViewPosition = getArguments().getInt("position");

        if(viewModel.getInitialTransitionName() == null){
            Log.d(TAG, "set initial transition name first (view model) : " + transitionName);
            Log.d(TAG, "set initial transition position first (view model) : " + initialRecyclerViewPosition);
            viewModel.setInitialTransitionName(transitionName);
            viewModel.setInitialTransitionNameForm(transitionNameForm);
            viewModel.setInitialPosition(initialRecyclerViewPosition);
        }
    }




    private void loadAlbums() {
        ArtistApiHelper apiHelper = new ArtistApiHelper(this.getContext(), requireActivity());
        apiHelper.searchAlbumsByArtist(null, artist.artistId, albumList -> {
            albumAdapter.updateData(albumList);
            binding.albumResultRecyclerView.post(() -> {
                handleReenterTransitionAlbum();
                Log.d(TAG, "Album load completed");

            });
        });
    }

    private void loadTopTracks() {
        ArtistApiHelper apiHelper = new ArtistApiHelper(this.getContext(), requireActivity());
        apiHelper.searchTrackByArtist(null, artist.artistId, tracks -> {
            trackAdapter.updateData(tracks);
            binding.trackResultRecyclerView.post(() -> {
                handleReenterTransitionForTrack();
                Log.d(TAG, "Track load completed");
            });

        });
    }

    private void loadArtistMetadata() {
        //Artist images list for ViewPager2
        imageUrls = new ArrayList<>();

        assert getArguments() != null;
        favoriteArtist = getArguments().getParcelable(ARGUMENTS_KEY);


        if (favoriteArtist == null || favoriteArtist.artist == null){
            Log.e(TAG, "Artist is null");
            return;
        }
        artist = favoriteArtist.artist;

        favoriteArtistViewModel.loadFavoriteArtistByArtistId(artist.artistId, favoriteArtist -> {
            if (favoriteArtist != null)
                addArtistButton.setVisibility(View.GONE);
        });

        if (artist.artworkUrl != null && !artist.artworkUrl.isEmpty()) {
            imageUrls.add(artist.artworkUrl);

        }

        ImagePagerAdapter.OnImageLoadListener imageLoadListener = new ImagePagerAdapter.OnImageLoadListener() {
            @Override
            public void onLoadSuccess(ImageView imageView) {
                Log.d(TAG, "onImageLoadListener success call");
                String transitionName = ViewCompat.getTransitionName(imageView);
                Log.d(TAG + " DEBUG", "transitionName: " + transitionName +", viemodel.getInitialTransName: " + viewModel.getInitialTransitionName());
                if (transitionName != null && (transitionName.equals(viewModel.getInitialTransitionName()) || transitionName.equals(viewModel.getCurrentTransitionName()))){
                    Log.d(TAG, "Artist main Image Load completed");
                    if (viewModel.isFirstFragmentCreation()) {
                        startPostponedEnterTransition();
                        Log.d(TAG, "is first first fragment creation and startPostponedEnterTransition");
                    }
                    else if(viewModel.isSecondPostponeFlag()){ //L2 cache hit 가 아닌 L1 cache 또는 Glide fetch
                        //postDelayed 로 중복 호출시 조건분기 오류 방지를 위해 100 delay 를 줌
                        new Handler(Looper.getMainLooper()).postDelayed(() -> handleReenterTransitionFromImageDetail(), 100);

                        Log.d(TAG, "is reenter from detail fragment");
                    }
                } else if(viewModel.getInitialTransitionName() == null){
                    if (viewModel.isFirstFragmentCreation()) {
                        startPostponedEnterTransition();
                        Log.d(TAG, "is first first fragment creation and startPostponedEnterTransition");
                    }
                }
            }

            @Override
            public void onLoadFailed() {
                Log.d(TAG, "Artist main Image Load Failed");
                if (viewModel.isFirstFragmentCreation()) {
                    startPostponedEnterTransition();
                    Log.d(TAG, "is first first fragment creation and startPostponedEnterTransition");
                }
            }

            @Override
            public void onCustomCacheLoadSuccess() { // 0 을 제외한 다른 포지션만 가능
                Log.d(TAG, "custom cache image load success");
                if(viewModel.isSecondPostponeFlag()){
                    handleReenterTransitionFromImageDetail();
                    Log.d(TAG, "is reenter from detail fragment for position " + viewModel.getStartPositionAtImageDetailFragment());
                }
            }
        };


        favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {

            @Override
            public void onSuccess(ArtistMetadata metadata){
                requireActivity().runOnUiThread(() -> {

                    viewModel.setMetadataExist(true);
                    genresLayout.setVisibility(View.GONE);
                    followersLayout.setVisibility(View.GONE);
                    viewSetting();

                    if (metadata.images != null && !metadata.images.isEmpty()){
                        //imageUrls.addAll(metadata.images);

                        for (String img : metadata.images){
                           if (!imageUrls.contains(img)) {
                                imageUrls.add(img);
                            }
                        }
                    }

                    Context context = getContext();
                    if (context != null) {
                        pageAdapter = new ImagePagerAdapter(
                                context,
                                imageUrls,
                                ArtistInfoFragment.this,
                                ArtistInfoFragment.this::onImageClick,
                                artist,
                                viewModel);
                        pageAdapter.setImageLoadListener(imageLoadListener);
                    }

                    pager.setAdapter(pageAdapter);

                    Log.d(TAG, "item count " + pageAdapter.getItemCount());

                    pager.setOffscreenPageLimit(1);

                    int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;

                    int horizontalPadding = Math.max(100 ,(int) (screenWidth * 0.0675f)); // 6% 패딩

                    pager.setPadding(horizontalPadding, pager.getPaddingTop(), horizontalPadding, pager.getPaddingBottom());

                    int marginPx = getResources().getDimensionPixelOffset(R.dimen.artwork_side_margin);

                    ViewPager2.PageTransformer combinedTransformer = new ViewPager2.PageTransformer() {
                        private final MarginPageTransformer marginTransformer = new MarginPageTransformer(marginPx);

                        @Override
                        public void transformPage(@NonNull View page, float position) {
                            // Margin 효과 적용
                            marginTransformer.transformPage(page, position);

                            // Scale / Alpha 효과 적용
                            float scale = 0.85f + (1 - Math.abs(position)) * 0.15f;
                            page.setScaleY(scale);
                            page.setAlpha(0.75f + (1 - Math.abs(position)) * 0.25f);
                        }
                    };

                    pager.setPageTransformer(combinedTransformer);




                    if (metadata.debutDate != null && !metadata.debutDate.isEmpty())
                        debutTextView.setText(metadata.debutDate);
                    else
                        debutLayout.setVisibility(View.GONE);

                    if (metadata.yearsOfActivity != null && !metadata.yearsOfActivity.isEmpty())
                        activityYearsTextView.setText(String.join(", ", metadata.yearsOfActivity));
                    else
                        activityYearsLayout.setVisibility(View.GONE);

                    if (metadata.members != null && !metadata.members.isEmpty()) {
                        membersTextView.setText(metadata.membersToString());
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
                            setClickableArtists(membersTextView, membersTextView.getText().toString(), artistMetadataMap);
                        }
                        else {

                            artistMetadataMap = new LinkedHashMap<>();
                            for (List<String> item : metadata.members){
                                if (item.get(1) != null) {
                                    artistMetadataMap.put(item.get(0), null);
                                }
                            }

                            if (favoriteArtist.metadata != null && favoriteArtist.metadata.members != null && !favoriteArtist.metadata.members.isEmpty()) {

                                fetchArtistMetadata(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "runnable starts, setClickableArtists in string and set dialogHelper");
                                        setClickableArtists(membersTextView, membersTextView.getText().toString(), artistMetadataMap);

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
                    }
                    else
                        membersLayout.setVisibility(View.GONE);

                    if (metadata.agency != null && !metadata.agency.isEmpty())
                        agencyTextView.setText(String.join(", ", metadata.agency));
                    else
                        agencyLayout.setVisibility(View.GONE);

                    if (metadata.activity != null && !metadata.activity.isEmpty()) {
                        activities = metadata.activity;
                        activityTextView.setText(metadata.activityToString());
                    }
                    else
                        activityLayout.setVisibility(View.GONE);

                    if (metadata.biography !=null && !metadata.biography.isEmpty())
                        biographyTextView.setText(metadata.biography);
                    else {
                        biographyLayout.setVisibility(View.GONE);
                        trackRecyclerView.setPadding(0,0,0,48);
                    }

                    if (favoriteArtist.addedDate != null && !favoriteArtist.addedDate.isEmpty()){
                        addedDateTextView.setText(favoriteArtist.addedDate);
                    }

                });
            }



            @Override
            public void onFailure(String reason) {
                requireActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Image Metadata loading fail");
                    viewModel.setMetadataExist(false);
                    pageAdapter = new ImagePagerAdapter(
                            requireContext(),
                            imageUrls,
                            ArtistInfoFragment.this,
                            ArtistInfoFragment.this::onImageClick,
                            artist,
                            viewModel);

                    pageAdapter.setImageLoadListener(imageLoadListener);
                    pager.setAdapter(pageAdapter);
                    trackRecyclerView.setPadding(0,0,0,48);
                    Log.d(TAG, reason);
                    debutLayout.setVisibility(View.GONE);
                    activityYearsLayout.setVisibility(View.GONE);
                    membersLayout.setVisibility(View.GONE);
                    agencyLayout.setVisibility(View.GONE);
                    activityLayout.setVisibility(View.GONE);
                    biographyLayout.setVisibility(View.GONE);
                    addedDataLayout.setVisibility(View.GONE);

                    if (artist.genres != null && !artist.genres.isEmpty() && !artist.genres.get(0).isEmpty()) {
                        genresTextView.setText(artist.getJoinedGenres());
                    }
                    else{
                        genresLayout.setVisibility(View.GONE);
                    }
                    followersTextView.setText(NumberUtils.formatWithComma(artist.followers));
                    viewSetting();
                });
            }
        });

    }

    private void bindView(View view){
        //pager = view.findViewById(R.id.image_pager);
        pager = binding.imagePager;
        artistNameTextView = view.findViewById(R.id.artist_name);
        genresTextView = view.findViewById(R.id.genres);
        followersTextView = view.findViewById(R.id.followers);

        addArtistButton = view.findViewById(R.id.add_button);
        debutTextView = view.findViewById(R.id.debut_date);
        activityYearsTextView = view.findViewById(R.id.activity_years);
        membersTextView = view.findViewById(R.id.members);
        agencyTextView = view.findViewById(R.id.agency);
        biographyTextView = view.findViewById(R.id.biography_text);
        activityTextView = view.findViewById(R.id.activity);
        addedDateTextView = view.findViewById(R.id.added_date);
        enlargeButton = view.findViewById(R.id.enlarge_button);
        //layout
        debutLayout = view.findViewById(R.id.debut_layout);
        activityYearsLayout = view.findViewById(R.id.activity_years_layout);
        membersLayout = view.findViewById(R.id.members_layout);
        agencyLayout = view.findViewById(R.id.agency_layout);
        activityLayout = view.findViewById(R.id.activity_layout);
        genresLayout = view.findViewById(R.id.genres_layout);
        followersLayout = view.findViewById(R.id.followers_layout);
        biographyLayout = view.findViewById(R.id.biography_layout);
        addedDataLayout = view.findViewById(R.id.added_date_layout);

        imageFetchButton = view.findViewById(R.id.image_fetch_button_overlay);
        setRepresentativeImageButton = view.findViewById(R.id.set_representative_image_button_overlay);
        imageLongClickOverlay = view.findViewById(R.id.long_click_overlay);
    }


    private void viewSetting(){
        artistNameTextView.setText(artist.artistName);
        //enlargeButton.setVisibility(View.VISIBLE);
        ImageView enlargeButtonShadow =  binding.enlargeButtonShadow;
        //enlargeButtonShadow.setVisibility(View.VISIBLE);

        pager.post(() -> {
            imageOverlayManager.setDismissListener(new ImageOverlayManager.OnDismissListener() {
                @Override
                public void onDismiss() {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
                    Log.d("Slider Handler", "slide speed set to " + AUTO_SLIDER_DELAY_TIME);
                }
            });

        });



        enlargeButton.setOnClickListener(v -> {
            int currentPosition = pager.getCurrentItem();

            if (currentPosition != 0)
                CustomFavoriteArtistImageCacheL2.getInstance().pin(imageUrls.get(currentPosition));
            viewModel.setStartPositionAtImageDetailFragment(currentPosition);

            // 2. 현재 페이지의 ViewHolder를 찾아, 그 안의 ImageView를 가져오기
            RecyclerView recyclerView = (RecyclerView) pager.getChildAt(0);
            ImagePagerAdapter.ImageViewHolder holder = (ImagePagerAdapter.ImageViewHolder) recyclerView.findViewHolderForAdapterPosition(currentPosition);

            // ViewHolder를 찾지 못하는 예외 상황 방지
            if (holder == null) {
                Toast.makeText(getContext(), "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            ImageView currentImageView = holder.imageView; // 어댑터의 ViewHolder에 있는 이미지 뷰
            Log.d(TAG, "image view connected");

            // 3. 전환할 뷰(currentImageView)에 고유한 transitionName 설정
            String transitionName = "Transition_artist_to_image_detail" + imageUrls.get(currentPosition) + currentPosition;
            ViewCompat.setTransitionName(currentImageView, transitionName);
            viewModel.setCurrentTransitionName(transitionName);
            viewModel.setSecondPostponeFlag(true);
            viewModel.setFirstFragmentCreation(false);
            Log.d(TAG, "set 2nd postpone flag true");

            // 4. 전달할 데이터 준비 (전체 URL 리스트, 현재 위치)
            Bundle args = new Bundle();
            args.putString("transitionName", transitionName);
            args.putStringArrayList("image_urls", (ArrayList<String>) imageUrls);
            args.putInt("start_position", currentPosition);

            // 5. 전환 애니메이션 정보(Extras) 생성
            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(currentImageView, transitionName)
                    .build();




            // 6. NavController로 데이터와 애니메이션 정보를 함께 전달하며 이동
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_artistInfoFragment_to_imageDetailFragment, args, null, extras);
        });

        // ArtistInfoFragment.java -> onViewCreated 또는 viewSetting 등 버튼을 설정하는 곳

// [수정!] 이 코드를 imageFetchButton의 클릭 리스너 안에 넣으세요.
        imageFetchButton.setOnClickListener(v -> imageFetch());


        setRepresentativeImageButton.setOnClickListener(v -> {
            int currentPosition = pager.getCurrentItem();
            Log.d(TAG, "setRepresentativeImageButton click event occurred at position: " + currentPosition);
            setRepresentativeImage(currentPosition);
        });





    }

    private void setRepresentativeImage(int currentPosition){
        String prevRepresentativeUrl = artist.artworkUrl;
        if (currentPosition > 0){
            if (favoriteArtist.metadata != null && favoriteArtist.metadata.vibeArtistId != null) {
                Log.d(TAG, "metadata exist");
                favoriteArtistViewModel.setRepresentativeArtistImage(favoriteArtist.metadata.vibeArtistId, currentPosition, prevRepresentativeUrl,updateMetadata -> {
                    if (updateMetadata) {
                        Log.d(TAG, "메타데이터에 대표 이미지 업데이트 완료");
                        setRepresentativeImageInDb(currentPosition);

                        //cache 에 저장하기
                        CustomFavoriteArtistImageWriter.removeUrl(viewGroupContext, prevRepresentativeUrl);
                        CustomFavoriteArtistImageWriter.storeImageByUrlLink(viewGroupContext, imageUrls.get(currentPosition), ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE);
                    }
                });
            } else{
                Log.d(TAG, "metadata 없음");
                favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
                    @Override
                    public void onSuccess(ArtistMetadata metadata) {
                        Log.d(TAG, "metadata 불러오기 성곧");
                        if (metadata != null && metadata.vibeArtistId != null) {
                            favoriteArtist.metadata = metadata;
                            favoriteArtistViewModel.setRepresentativeArtistImage(favoriteArtist.metadata.vibeArtistId, currentPosition, prevRepresentativeUrl, updateMetadata -> {
                                if (updateMetadata) {
                                    Log.d(TAG, "메타데이터에 대표 이미지 업데이트 완료");
                                    setRepresentativeImageInDb(currentPosition);

                                    //cache 에 저장하기
                                    CustomFavoriteArtistImageWriter.removeUrl(viewGroupContext, prevRepresentativeUrl);
                                    CustomFavoriteArtistImageWriter.storeImageByUrlLink(viewGroupContext, imageUrls.get(currentPosition), ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE);

                                }
                            });
                        }
                    }
                    @Override
                    public void onFailure(String reason) {
                        Log.d(TAG, "metadata 불러오기  실패");
                    }

                });
            }
        }
    }
    private void imageFetch(){
        favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
            @Override
            public void onSuccess(ArtistMetadata metadata) {
                if (metadata != null && metadata.vibeArtistId != null) {
                    favoriteArtist.metadata = metadata;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // 1. 전달할 데이터(artistId)를 Bundle에 담기
                        Bundle bundle = new Bundle();
                        bundle.putString("artist_id", metadata.vibeArtistId); // 현재 artist 객체에서 ID 가져오기
                        Log.d(TAG, "vibe artist id: " + metadata.vibeArtistId);

                        // 2. NavController를 사용해 action 실행
                        NavController navController = NavHostFragment.findNavController(ArtistInfoFragment.this);
                        navController.navigate(R.id.fragment_web_view, bundle);
                    });

                }
            }
            @Override
            public void onFailure(String reason) {
                Log.e(TAG, "metadata or vibe id is null");
            }

        });
    }


    private void setRepresentativeImageInDb(int currentPosition){
        List<String> imageUrls = pageAdapter.getImageUrls();
        if (currentPosition < imageUrls.size()) {
            String currentImageUrl = imageUrls.get(currentPosition);
            favoriteArtist.artist.artworkUrl = currentImageUrl;
            favoriteArtistViewModel.updateFavoriteArtist(favoriteArtist, result -> {
                if (result) {
                    //new Handler(Looper.getMainLooper()).post(() -> pageAdapter.notifyDataSetChanged());  // or create a new adapter if this doesn't work well

                    imageLongClickOverlay.setVisibility(View.GONE);
                    Log.d(TAG, "FavoriteArtist db에 대표 이미지 update 완료" + currentImageUrl);
                    Log.d(TAG, "image 개수: " + pageAdapter.getItemCount());
                }
                else Log.d(TAG, "FavoriteArtist db에 대표 이미지 update 실패" + currentImageUrl);
            });
            Log.d("ArtistInfoFragment", "현재 이미지 URL: " + currentImageUrl);
        }

    }



    public void onLongClick(ImageView imageView, MotionEvent event, String imageUrl){
        float touchX = event.getRawX();
        float touchY = event.getRawY();

        sliderHandler.removeCallbacks(sliderRunnable);

        // 매니저의 showOverlay 메서드 호출 시 좌표 전달
        imageOverlayManager.showOverlay((ImageView) imageView, imageUrl, touchX, touchY, viewModel.isMetadataExist());
    }



    private void addFavoriteSong(Track track, int position){
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
                        //Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        trackAdapter.notifyItemChanged(position);
                    });
                } catch (SQLiteConstraintException e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private void showTrackDetails(Track track) {
        new AlertDialog.Builder(getContext())
                .setTitle("세부사항")
                .setMessage("제목: " + track.trackName +
                        "\n아티스트: " + track.artistName +
                        "\n앨범: " + track.albumName +
                        "\n발매일: " + track.releaseDate.substring(0, 10))
                .setPositiveButton("닫기", null)
                .show();
    }


    private void handleReenterTransitionFromImageDetail(){
        Log.d(TAG, "handleReenterTransitionFromImageDetail() 호출됨");
        int lastPosition = viewModel.getLastPositionAtImageDetailFragment();
        if (lastPosition != -1) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                pager.setCurrentItem(lastPosition, true);
                Log.d(TAG, "viewPager position set to " + lastPosition);
                viewModel.setLastPositionAtImageDetailFragment(-1);
                viewModel.setSecondPostponeFlag(false);
            }, 500);

        }
        startPostponedEnterTransition();
    }
    private void handleReenterTransitionForTrack() {
        Log.d(TAG, "handleReenterAndStartTransition() 호출됨");

        int position = viewModel.getTrackPosition();
        viewModel.setTrackPosition(-1);

        if (position == -1) {
            Log.d(TAG, "(Track) reenter state 아님, startPostponedEnterTransition() 호출 취소");
            return;
        }

        scrollView.post(() -> {
            int y = viewModel.getScrollY();
            scrollView.scrollTo(0, y);
            startPostponedEnterTransition();
        });
    }

    private void handleReenterTransitionAlbum(){
        Log.d(TAG, "handleReenterTransitionAlbum() 호출됨");
        int position = viewModel.getAlbumPosition();
        viewModel.setAlbumPosition(-1);

        if (position == -1){
            Log.d(TAG, "(Album) reenter state 아님, startPostponedEnterTransition() 호출 취소");
            return;
        }

        binding.albumResultRecyclerView.scrollToPosition(position);

        int y = viewModel.getScrollY();
        scrollView.scrollTo(0, y);

        binding.albumResultRecyclerView.post(() -> {
            LinearLayoutManager layoutManager = (LinearLayoutManager) albumRecyclerView.getLayoutManager();
            if (layoutManager != null) layoutManager.scrollToPositionWithOffset(viewModel.getReenterAlbumScrollPosition(), viewModel.getReenterAlbumScrollOffset());
            startPostponedEnterTransition();
            Log.d(TAG, "startPostponedEnterTransition() 호출됨");
        });

    }


    public void onTrackClick(Track track, ImageView sharedImageView, int position){
        Log.d(TAG, "onTrackClick() 호출됨");
        Bundle bundle = new Bundle();
        Favorite favorite = new Favorite(track);
        bundle.putParcelable("favorite", favorite);
        String transitionName = ViewCompat.getTransitionName(sharedImageView);
        viewModel.setTrackPosition(position);
        viewModel.setFirstFragmentCreation(false);

        viewModel.setScrollY(scrollView.getScrollY());


        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();
        bundle.putString("transitionName", transitionName);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.musicInfoFragment, bundle, null, extras);

    }

    public void onAlbumClick(Album album, ImageView sharedImageView, int position){
        Log.d(TAG, "onAlbumClick() 호출됨");
        Bundle args = new Bundle();
        args.putParcelable("album", album);
        String transitionName = ViewCompat.getTransitionName(sharedImageView);
        viewModel.setAlbumPosition(position);
        viewModel.setFirstFragmentCreation(false);

        viewModel.setScrollY(scrollView.getScrollY());

        LinearLayoutManager layoutManager = (LinearLayoutManager) albumRecyclerView.getLayoutManager();
        if (layoutManager != null){
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            View firstVisibelView = layoutManager.findViewByPosition(firstVisiblePosition);
            int offset = firstVisibelView.getLeft();
            viewModel.setReenterAlbumScrollPosition(firstVisiblePosition);
            viewModel.setReenterAlbumScrollOffset(offset);
        }

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();
        args.putString("transitionName", transitionName);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.album_info, args, null, extras);
    }


    public void onImageClick(){
        int currentPosition = pager.getCurrentItem();

        if (currentPosition != 0)
            CustomFavoriteArtistImageCacheL2.getInstance().pin(imageUrls.get(currentPosition));
        viewModel.setStartPositionAtImageDetailFragment(currentPosition);

        // 2. 현재 페이지의 ViewHolder를 찾아, 그 안의 ImageView를 가져오기
        RecyclerView recyclerView = (RecyclerView) pager.getChildAt(0);
        ImagePagerAdapter.ImageViewHolder holder = (ImagePagerAdapter.ImageViewHolder) recyclerView.findViewHolderForAdapterPosition(currentPosition);

        // ViewHolder를 찾지 못하는 예외 상황 방지
        if (holder == null) {
            Toast.makeText(getContext(), "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        ImageView currentImageView = holder.imageView; // 어댑터의 ViewHolder에 있는 이미지 뷰
        Log.d(TAG, "image view connected");

        // 3. 전환할 뷰(currentImageView)에 고유한 transitionName 설정
        String transitionName = "Transition_artist_to_image_detail" + imageUrls.get(currentPosition) + currentPosition;
        ViewCompat.setTransitionName(currentImageView, transitionName);
        viewModel.setCurrentTransitionName(transitionName);
        viewModel.setSecondPostponeFlag(true);
        viewModel.setFirstFragmentCreation(false);
        Log.d(TAG, "set 2nd postpone flag true");

        // 4. 전달할 데이터 준비 (전체 URL 리스트, 현재 위치)
        Bundle args = new Bundle();
        args.putString("transitionName", transitionName);
        args.putStringArrayList("image_urls", (ArrayList<String>) imageUrls);
        args.putInt("start_position", currentPosition);

        // 5. 전환 애니메이션 정보(Extras) 생성
        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(currentImageView, transitionName)
                .build();




        // 6. NavController로 데이터와 애니메이션 정보를 함께 전달하며 이동
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_artistInfoFragment_to_imageDetailFragment, args, null, extras);
    }

    public void fetchArtistMetadata(Runnable onAllFinished) {
        List<List<String>> vocalists = favoriteArtist.metadata.members;
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

            for (List<String> item : favoriteArtist.metadata.members){
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
                    SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                    int selectedColor = prefs.getInt("selected_color", Color.GRAY); // 기본값 회색

                    ds.setColor(selectedColor);    // 클릭 가능한 색상
                }
            };

            spannable.setSpan(clickableSpan, index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT); // 클릭 시 배경 투명
    }

}

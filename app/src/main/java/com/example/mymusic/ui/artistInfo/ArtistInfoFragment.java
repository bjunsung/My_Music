   package com.example.mymusic.ui.artistInfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mymusic.R;
import com.example.mymusic.adapter.SimpleAlbumAdapter;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.data.repository.ArtistMetadataRepository;
import com.example.mymusic.databinding.FragmentArtistInfoBinding;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.ui.imageDetail.ImageDetailFragment;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtistInfoFragment extends Fragment implements ImagePagerAdapter.OnImageLongClickListener{

    private FavoriteArtistViewModel favoriteArtistViewModel;
    private FavoritesViewModel favoritesViewModel;
    private FavoriteArtist favoriteArtist;

    private Artist artist;
    private TextView artistNameTextView, genresTextView, followersTextView;
    private RecyclerView albumRecyclerView, trackRecyclerView;
    private ImageButton addArtistButton;
    private LinearLayout debutLayout, activityYearsLayout, membersLayout, agencyLayout, activityLayout, genresLayout, followersLayout, biographyLayout, addedDataLayout;
    private TextView debutTextView, activityYearsTextView, membersTextView, agencyTextView, activityTextView, biographyTextView, addedDateTextView;
    private final String TAG = "ArtistInfoFragment";

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
    private boolean isTransitionStarted = false;

    private long currentOnDataReadyTime = 0;
    public static final String REQUEST_KEY = "artist_info_fragment_request";
    public static final String BUNDLE_KEY_TRANSITION_END = "transition_artist_artwork_ended";
    private CardView imageFetchButton, setRepresentativeImageButton;
    LinearLayout imageLongClickOverlay;

    private static final long AUTO_SLIDER_DELAY_TIME = 5000L;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
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


        getParentFragmentManager().setFragmentResultListener(ImageDetailFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            // B 프래그먼트에서 보낸 키와 일치하는지 확인
            if (requestKey.equals(ImageDetailFragment.REQUEST_KEY)) {
                boolean transitionEnded = bundle.getBoolean(ImageDetailFragment.BUNDLE_KEY_TRANSITION_END);
                if (transitionEnded) {
                    Log.d("ListFragment", "Return transition from DetailsFragment has ended.");
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

                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(WebViewFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (requestKey.equals(WebViewFragment.REQUEST_KEY)) {
                List<String> fetchedImageUrls = bundle.getStringArrayList(WebViewFragment.BUNDLE_KEY_IMAGE_URLS);
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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        readyCounter.set(0);
        isTransitionStarted = false;
        currentOnDataReadyTime = 0;
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



        //end of on view created
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
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
            onDataReady();
            return;
        }

        String transitionNameForm = getArguments().getString("transitionNameForm");
        int initialRecyclerViewPosition = getArguments().getInt("position");

        if(viewModel.getInitialTransitionName() == null){
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
                Log.d(TAG, "Album load completed");
                onDataReady();
            });
        });
    }

    private void loadTopTracks() {
        ArtistApiHelper apiHelper = new ArtistApiHelper(this.getContext(), requireActivity());
        apiHelper.searchTrackByArtist(null, artist.artistId, tracks -> {
            trackAdapter.updateData(tracks);
            binding.trackResultRecyclerView.post(() -> {
                //handleReenterTransitionTrack();
                Log.d(TAG, "Track load completed");
                onDataReady();
            });

        });
    }

    private void loadArtistMetadata() {
        //Artist images list for ViewPager2
        imageUrls = new ArrayList<>();

        assert getArguments() != null;
        favoriteArtist = getArguments().getParcelable("favorite_artist");


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

        favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
            ImagePagerAdapter.OnImageLoadListener imageLoadListener = new ImagePagerAdapter.OnImageLoadListener() {
                @Override
                public void onLoadSuccess(ImageView imageView) {
                    Log.d(TAG, "onImageLoadListener success call");
                    String transitionName = ViewCompat.getTransitionName(imageView);
                    if (transitionName != null && (transitionName.equals(viewModel.getInitialTransitionName()) || transitionName.equals(viewModel.getCurrentTransitionName()))){
                        Log.d(TAG, "Artist main Image Load completed");
                        onDataReady();
                    }
                }

                @Override
                public void onLoadFailed() {
                    Log.d(TAG, "Artist main Image Load Failed");
                    onDataReady();
                }
            };
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
                                artist,
                                viewModel);
                        pageAdapter.setImageLoadListener(imageLoadListener);
                    }

                    pager.setAdapter(pageAdapter);

                    pager.setOffscreenPageLimit(3);

                    ViewPager2.PageTransformer transformer = (page, position) -> {
                        float scale = 0.85f + (1 - Math.abs(position)) * 0.15f;
                        page.setScaleY(scale);
                        page.setAlpha(0.5f + (1 - Math.abs(position)) * 0.5f);
                    };

                    pager.setPageTransformer(transformer);


                    if (metadata.debutDate != null && !metadata.debutDate.isEmpty())
                        debutTextView.setText(metadata.debutDate);
                    else
                        debutLayout.setVisibility(View.GONE);

                    if (metadata.yearsOfActivity != null && !metadata.yearsOfActivity.isEmpty())
                        activityYearsTextView.setText(String.join(", ", metadata.yearsOfActivity));
                    else
                        activityYearsLayout.setVisibility(View.GONE);

                    if (metadata.members != null && !metadata.members.isEmpty())
                        membersTextView.setText(metadata.membersToString());
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

                    if (artist.genres != null && !artist.genres.isEmpty()) {
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

        pager.post(() -> {
            imageOverlayManager.setDownloadButtonLocation(- (int)(pager.getWidth()/6.5f), pager.getWidth()/14);

            int[] pagerLocation = new int[2];
            pager.getLocationOnScreen(pagerLocation);
            int pagerRightX = pagerLocation[0] + pager.getWidth();    // Pager의 오른쪽 끝 X 좌표
            int pagerBottomY = pagerLocation[1] + pager.getHeight();

            // 2. 버튼의 크기를 고려하여 위치 계산
            // (버튼의 너비와 높이를 알아야 정확한 위치에 놓을 수 있습니다)
            int buttonWidth = enlargeButton.getWidth();
            int buttonHeight = enlargeButton.getHeight();

            // 만약 버튼 크기가 0으로 나온다면, 임시로 크기를 지정해줍니다 (dp를 px로 변환)
            final float density = getResources().getDisplayMetrics().density;
            if (buttonWidth == 0) buttonWidth = (int)(24 * density);
            if (buttonHeight == 0) buttonHeight = (int)(24 * density);

            int padding = (int)(6 * density); // 우측, 하단 여백

            // 3. LayoutParams에 적용
            FrameLayout.LayoutParams enlargeParams = (FrameLayout.LayoutParams) enlargeButton.getLayoutParams();
            enlargeParams.leftMargin = pagerRightX - buttonWidth - padding;
            enlargeParams.topMargin = pagerBottomY - buttonHeight - padding;

            enlargeButton.setLayoutParams(enlargeParams);
            enlargeButton.setVisibility(View.VISIBLE);

            ImageView enlargeButtonShadow =  binding.enlargeButtonShadow;
            FrameLayout.LayoutParams enlargeShadowParams = (FrameLayout.LayoutParams) enlargeButtonShadow.getLayoutParams();
            enlargeShadowParams.leftMargin = pagerRightX - buttonWidth - padding + 2;
            enlargeShadowParams.topMargin = pagerBottomY - buttonHeight - padding + 2;
            enlargeButtonShadow.setLayoutParams(enlargeShadowParams);
            enlargeButtonShadow.setVisibility(View.VISIBLE);

            imageOverlayManager.setDismissListener(new ImageOverlayManager.OnDismissListener() {
                @Override
                public void onDismiss() {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDER_DELAY_TIME);
                }
            });

        });



        enlargeButton.setOnClickListener(v -> {
            int currentPosition = pager.getCurrentItem();

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
        imageFetchButton.setOnClickListener(v -> {
            favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
                @Override
                public void onSuccess(ArtistMetadata metadata) {
                    if (metadata != null && metadata.vibeArtistId != null) {
                        favoriteArtist.metadata = metadata;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            // 1. 전달할 데이터(artistId)를 Bundle에 담기
                            Bundle bundle = new Bundle();
                            bundle.putString("artist_id", metadata.vibeArtistId); // 현재 artist 객체에서 ID 가져오기

                            // 2. NavController를 사용해 action 실행
                            NavController navController = NavHostFragment.findNavController(ArtistInfoFragment.this);
                            navController.navigate(R.id.action_artistInfoFragment_to_webViewFragment, bundle);
                        });

                    }
                }
                @Override
                public void onFailure(String reason) {}

            });

        });

        setRepresentativeImageButton.setOnClickListener(v -> {

            int currentPosition = pager.getCurrentItem();
            Log.d(TAG, "setRepresentativeImageButton click event occurred at position: " + currentPosition);
            if (currentPosition > 0){
                if (favoriteArtist.metadata != null && favoriteArtist.metadata.vibeArtistId != null) {
                    Log.d(TAG, "metadata exist");
                    favoriteArtistViewModel.setRepresentativeArtistImage(favoriteArtist.metadata.vibeArtistId, currentPosition, updateMetadata -> {
                        if (updateMetadata) {
                            Log.d(TAG, "메타데이터에 대표 이미지 업데이트 완료");
                            setRepresentativeImageInDb(currentPosition);
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
                                favoriteArtistViewModel.setRepresentativeArtistImage(favoriteArtist.metadata.vibeArtistId, currentPosition, updateMetadata -> {
                                    if (updateMetadata) {
                                        Log.d(TAG, "메타데이터에 대표 이미지 업데이트 완료");
                                        setRepresentativeImageInDb(currentPosition);
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
        new AlertDialog.Builder(getContext())
                .setTitle("관심목록에 추가")
                .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        favoritesViewModel.loadFavoriteItem(track.trackId, loaded -> {
                            if (loaded != null){
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                                });
                            }
                            else{
                                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                                new Thread(() -> {
                                    try {
                                        favoritesViewModel.insert(track, today);
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                            if (trackAdapter != null){
                                                trackAdapter.notifyItemChanged(position);
                                            }
                                        });
                                    } catch (SQLiteConstraintException e) {
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }).start();
                            }
                        });
                    }
                })
                .show();
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

    private void onDataReady(){
        int currentCount = readyCounter.incrementAndGet();
        if (currentOnDataReadyTime != 0){
            long temp = System.currentTimeMillis();
            Log.d(TAG, "시간차이: " + (temp - currentOnDataReadyTime) + "ms");
            currentOnDataReadyTime = temp;
        }else{
            currentOnDataReadyTime = System.currentTimeMillis();
        }
        Log.d(TAG, "onDataReady() 호출됨 - readyCounter num: " + currentCount);
        Log.d(TAG, "transition started state: " + isTransitionStarted);
        if (currentCount == 3 && !isTransitionStarted){
            Log.d(TAG, "every data is ready, now start transition first time");
            isTransitionStarted = true;
            if (viewModel.getTrackPosition() != -1) {
                Log.d(TAG, "onDataReady Debug - viewModel.getTrackPosition() != -1");
                handleReenterTransitionForTrack();
            } else if (viewModel.getAlbumPosition() != -1){
                Log.d(TAG, "onDataReady Debug - viewModel.getAlbumPosition() != -1");
                handleReenterTransitionAlbum();
            } else if (viewModel.isSecondPostponeFlag()){
                Log.d(TAG, "onDataReady Debug - reenter from image detail fragment");
                handleReenterTransitionFromImageDetail();
            } else {
                pager.post(() -> startPostponedEnterTransition());
            }
        }
    }

    private void handleReenterTransitionFromImageDetail(){
        Log.d(TAG, "handleReenterTransitionFromImageDetail() 호출됨");
        viewModel.setSecondPostponeFlag(false);
        startPostponedEnterTransition();

        Log.d(TAG, "onDataReady Debug - initial enter");
    }
    private void handleReenterTransitionForTrack() {
        Log.d(TAG, "handleReenterAndStartTransition() 호출됨");
        // ViewModel에서 돌아갈 위치를 가져옵니다.
        int position = viewModel.getTrackPosition();
        viewModel.setTrackPosition(-1);
        // 돌아갈 위치 정보가 없으면 바로 전환 시작
        if (position == -1) {
            Log.d(TAG, "(Track) reenter state 아님, startPostponedEnterTransition() 호출 취소");
            return;
        }

        // ✅ RecyclerView에게 해당 위치의 뷰를 '준비'하라고 먼저 알립니다.
        binding.trackResultRecyclerView.scrollToPosition(position);

        // ✅ post()를 사용해, 위 스크롤 요청이 반영된 '다음' 프레임에 로직을 실행합니다.
        binding.trackResultRecyclerView.post(() -> {
            // 이제 해당 위치의 ViewHolder를 찾습니다.
            RecyclerView.ViewHolder holder = binding.trackResultRecyclerView.findViewHolderForAdapterPosition(position);

            // ViewHolder를 찾았다면, 그 뷰의 Y좌표로 부모 스크롤뷰를 강제로 스크롤합니다.
            if (holder != null) {
                // ❗ fragment_artist_info.xml에서 ViewPager2와 RecyclerView를 모두 감싸는
                // 최상위 NestedScrollView 또는 ScrollView의 ID로 변경해야 합니다.
                binding.parentScrollContainer.scrollTo(0, (int) holder.itemView.getY() + 220);
                Log.d(TAG, "viewholder detected! force parent scroll view to locate (0, Y of shared item view)");
            }
            else{
                Log.d(TAG, "Error, viewholder does not detected!");
            }

            // 모든 스크롤이 강제로 완료된 이 시점에 전환을 시작합니다.

            startPostponedEnterTransition();
            Log.d(TAG, "startPostponedEnterTransition() 호출됨");

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

        binding.albumResultRecyclerView.post(() -> {
            RecyclerView.ViewHolder holder = binding.albumResultRecyclerView.findViewHolderForAdapterPosition(position);

            if (holder != null){
                binding.parentScrollContainer.scrollTo((int) holder.itemView.getX() , 0);
            }else{
                Log.d(TAG, "Error, viewholder does not detected!");
            }
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
        //viewModel.setAlbumPosition(-1);


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

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();
        args.putString("transitionName", transitionName);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.album_info, args, null, extras);
    }




}

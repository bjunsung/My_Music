package com.example.mymusic.ui.artistInfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
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
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionInflater;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mymusic.R;
import com.example.mymusic.adapter.SimpleAlbumAdapter;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.databinding.FragmentArtistInfoBinding;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.util.ImageOverlayManager;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;

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



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));

        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        viewModel = new ViewModelProvider(this).get(ArtistInfoViewModel.class);
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

        albumAdapter = new SimpleAlbumAdapter(new ArrayList<>());
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
                                    favoriteArtistViewModel.insert(artist, today);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), artist.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
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

    }

    private void parseArgs() {
        String transitionName = getArguments().getString("transitionName");
        String transitionNameForm = getArguments().getString("transitionNameForm");
        int position = getArguments().getInt("postiion");

        if(viewModel.getInitialTransitionName() == null){
            viewModel.setInitialTransitionName(transitionName);
            viewModel.setInitialTransitionNameForm(transitionNameForm);
            viewModel.setInitialPosition(position);
            viewModel.setInitialTransitionNameForm(transitionNameForm);
            int initialRecyclerViewPosition = getArguments().getInt("position");
            viewModel.setInitialPosition(initialRecyclerViewPosition);
        }
    }


    private void handleReenterTransitionForTrack() {
        Log.d(TAG, ":::: handleReenterAndStartTransition() 호출됨 ::::");
        // ViewModel에서 돌아갈 위치를 가져옵니다.
        int position = viewModel.getTrackPosition();
        viewModel.setTrackPosition(-1);
        // 돌아갈 위치 정보가 없으면 바로 전환 시작
        if (position == -1) {
            Log.d(TAG, "reenter state 아님, startPostponedEnterTransition() 호출 취소");
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
            }else if (viewModel.getAlbumPosition() != -1){
                Log.d(TAG, "onDataReady Debug - viewModel.getAlbumPosition() != -1");
                handleReenterTransitionAlbum();
            } else{
                Log.d(TAG, "onDataReady Debug - initial enter or reenter from image detail");
                pager.post(() -> startPostponedEnterTransition());
            }
        }
    }

    private void loadAlbums() {
        ArtistApiHelper apiHelper = new ArtistApiHelper(this.getContext(), requireActivity());
        apiHelper.searchAlbumsByArtist(null, artist.artistId, albumList -> {
            albumAdapter.updateData(albumList);
            binding.albumResultRecyclerView.post(() -> {
                handleReenterTransitionAlbum();
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

        if (artist.artworkUrl != null && !artist.artworkUrl.isEmpty()) {
            imageUrls.add(artist.artworkUrl);
        }

        favoriteArtistViewModel.loadArtistMetadataBySpotifyId(artist.artistId, new FavoriteArtistViewModel.MetadataCallback() {
            @Override
            public void onSuccess(ArtistMetadata metadata){
                requireActivity().runOnUiThread(() -> {
                    addArtistButton.setVisibility(View.GONE);
                    genresLayout.setVisibility(View.GONE);
                    followersLayout.setVisibility(View.GONE);
                    viewSetting();

                    if (metadata.images != null && !metadata.images.isEmpty()){
                        imageUrls.addAll(metadata.images);
                    }

                    Context context = getContext();
                    if (context != null) {
                        pageAdapter = new ImagePagerAdapter(
                                context,
                                imageUrls,
                                ArtistInfoFragment.this,
                                artist,
                                viewModel.getInitialTransitionNameForm(),
                                viewModel.getInitialPosition());
                        pageAdapter.setImageLoadListener(new ImagePagerAdapter.OnImageLoadListener() {

                            @Override
                            public void onLoadSuccess(ImageView imageView) {
                                String transitionName = ViewCompat.getTransitionName(imageView);
                                if (transitionName.equals(viewModel.getInitialTransitionName())){
                                    Log.d(TAG, "Artist main Image Load completed");

                                    onDataReady();
                                }
                            }

                            @Override
                            public void onLoadFailed() {
                                Log.d(TAG, "Artist main Image Load Failed");
                                onDataReady();
                            }
                        });
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
                    // onDataReady();
                    pageAdapter = new ImagePagerAdapter(
                            requireContext(),
                            imageUrls,
                            ArtistInfoFragment.this,
                            artist,
                            viewModel.getInitialTransitionNameForm(),
                            viewModel.getInitialPosition());
                    pageAdapter.setImageLoadListener(new ImagePagerAdapter.OnImageLoadListener() {

                        @Override
                        public void onLoadSuccess(ImageView imageView) {
                            String transitionName = ViewCompat.getTransitionName(imageView);
                            if (transitionName.equals(viewModel.getInitialTransitionName())){
                                Log.d(TAG, "Artist main Image Load completed");
                                onDataReady();
                            }
                        }

                        @Override
                        public void onLoadFailed() {
                            Log.d(TAG, "Artist main Image Load Failed");
                            onDataReady();
                        }
                    });
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
            //onDataReady(); //임시방편, 삭제해야 하는 코드

        });


/*
        pager.post(() -> {
            Log.d(TAG, "this is PAGER2 AREA");
            imageOverlayManager.setDownloadButtonLocation(- (int)(pager.getWidth() / 6.5f), pager.getWidth() / 14);

            int[] pagerLocation = new int[2];
            pager.getLocationOnScreen(pagerLocation);
            int pagerRightX = pagerLocation[0] + pager.getWidth();
            int pagerBottomY = pagerLocation[1] + pager.getHeight();

            int buttonWidth = enlargeButton.getWidth();
            int buttonHeight = enlargeButton.getHeight();

            final float density = getResources().getDisplayMetrics().density;
            if (buttonWidth == 0) buttonWidth = (int)(24 * density);
            if (buttonHeight == 0) buttonHeight = (int)(24 * density);

            int padding = (int)(6 * density);

            FrameLayout.LayoutParams enlargeParams = (FrameLayout.LayoutParams) enlargeButton.getLayoutParams();
            enlargeParams.leftMargin = pagerRightX - buttonWidth - padding;
            enlargeParams.topMargin = pagerBottomY - buttonHeight - padding;

            enlargeButton.setLayoutParams(enlargeParams);
            enlargeButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "ViewPager2 load completed");
            onDataReady();
        });
*/

        /*
        pager.postDelayed(() -> {
            if (!((viewModel.getTrackPosition() != -1 && !isTrackImageReady) || (viewModel.getAlbumPosition() != -1 && !isArtistImageReady))) {

                //startPostponedEnterTransition();
                //Log.d(TAG, "startPostponedEnterTransition()");
                Log.d(TAG, "track position: " + viewModel.getTrackPosition());
                Log.d(TAG, "is Track Ready: " + isTrackImageReady);
                Log.d(TAG, "album position: " + viewModel.getAlbumPosition());
                Log.d(TAG, "is album Ready: " + isAlbumImageReady);
            }

        }, 10);
         */





        // ArtistInfoFragment.java 의 onViewCreated 또는 setView 내부

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

            // 3. 전환할 뷰(currentImageView)에 고유한 transitionName 설정
            String transitionName = "Transition_artist_to_image_detail" + imageUrls.get(currentPosition) + currentPosition;
            ViewCompat.setTransitionName(currentImageView, transitionName);
            viewModel.setCurrentTransitionName(transitionName);

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


    }



    private void handleReenterTransitionAlbum(){
        //todo track 과 같은 로직으로 수정
    }


    public void onLongClick(ImageView imageView, MotionEvent event, String imageUrl){
        float touchX = event.getRawX();
        float touchY = event.getRawY();

        // 매니저의 showOverlay 메서드 호출 시 좌표 전달
        imageOverlayManager.showOverlay((ImageView) imageView, imageUrl, touchX, touchY);
    }



    private void addFavoriteSong(Track track){
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

    public void onTrackClick(Track track, ImageView sharedImageView, int position){
        Log.d(TAG, "onTrackClick() 호출됨");
        Bundle bundle = new Bundle();
        Favorite favorite = new Favorite(track);
        bundle.putParcelable("favorite", favorite);
        String transitionName = ViewCompat.getTransitionName(sharedImageView);
        viewModel.setTrackPosition(position);
        viewModel.setAlbumPosition(-1);


        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();
        bundle.putString("transitionName", transitionName);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.musicInfoFragment, bundle, null, extras);

    }




}

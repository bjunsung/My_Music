package com.example.mymusic.ui.artistInfo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mymusic.R;
import com.example.mymusic.adapter.SimpleAlbumAdapter;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.util.EdgeSwipeBackGestureHelper;
import com.example.mymusic.util.ImageOverlayManager;
import com.example.mymusic.util.ImageSaveUtil;
import com.example.mymusic.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_artist_info, container, false);
        new EdgeSwipeBackGestureHelper().attachToView(view.findViewById(R.id.gesture_overlay), view.findViewById(R.id.swipe_content),this);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        imageOverlayManager = new ImageOverlayManager(requireActivity(), view);

        // ✅ BottomNavigationView를 액티비티에서 찾아와 원래 배경을 저장
        bottomNavView = requireActivity().findViewById(R.id.nav_view);
        if (bottomNavView != null) {
            originalBottomNavBackground = bottomNavView.getBackground();
        }


        bindView(view);
        //setupOverlayListeners();

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
                    pageAdapter = new ImagePagerAdapter(requireContext(), imageUrls, ArtistInfoFragment.this);

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
                    pageAdapter = new ImagePagerAdapter(requireContext(), imageUrls, ArtistInfoFragment.this);
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

    private void bindView(View view){
        pager = view.findViewById(R.id.image_pager);
        artistNameTextView = view.findViewById(R.id.artist_name);
        genresTextView = view.findViewById(R.id.genres);
        followersTextView = view.findViewById(R.id.followers);
        albumRecyclerView = view.findViewById(R.id.album_result_recycler_view);
        albumRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        trackRecyclerView = view.findViewById(R.id.track_result_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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



        ArtistApiHelper apiHelper = new ArtistApiHelper(this.getContext(), requireActivity());

        apiHelper.searchAlbumsByArtist(null, artist.artistId, albumList -> {
            SimpleAlbumAdapter albumAdapter = new SimpleAlbumAdapter(albumList);
            albumRecyclerView.setAdapter(albumAdapter);
            albumRecyclerView.setNestedScrollingEnabled(false);
        });

        apiHelper.searchTrackByArtist(null, artist.artistId, tracks -> {
            TrackAdapter trackAdapter = new TrackAdapter(tracks, getContext(), this::showTrackDetails, this::addFavoriteSong);
            trackRecyclerView.setAdapter(trackAdapter);
            trackRecyclerView.setNestedScrollingEnabled(false);
        });


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
        });

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
            String transitionName = "image_detail_" + imageUrls.get(currentPosition);
            ViewCompat.setTransitionName(currentImageView, transitionName);

            // 4. 전달할 데이터 준비 (전체 URL 리스트, 현재 위치)
            Bundle args = new Bundle();
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


}

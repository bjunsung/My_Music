package com.example.mymusic.ui.albumInfo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
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
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Explode;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.mymusic.R;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.databinding.FragmentAlbumInfoBinding;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.example.mymusic.util.EdgeSwipeBackGestureHelper;
import com.example.mymusic.util.ImageOverlayManager;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


public class AlbumInfoFragment extends Fragment {

    private Album album;

    private ArtistApiHelper apiHelper;
    private TrackAdapter trackAdapter;
    private FavoritesViewModel favoritesViewModel;
    private FavoriteArtistViewModel favoriteArtistViewModel;



    //View
    private ImageView albumImageView;
    private TextView albumNameTextView, artistNameTextView, releaseDateTextView, totalTracksTextView;
    private RecyclerView trackRecyclerView;
    private ImageOverlayManager imageOverlayManager;
    private ImageView enlargeButton;
    private FragmentAlbumInfoBinding binding;

    private boolean isImageReady = false;
    private boolean isListReady = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstatceState){
        super.onCreate(savedInstatceState);

        //앨범 이미지 트랜지션 설정
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setPathMotion(new MaterialArcMotion());
        setSharedElementEnterTransition(transform);
        setSharedElementReturnTransition(new MaterialContainerTransform());


        setEnterTransition(new Explode());
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState){
        binding = FragmentAlbumInfoBinding.inflate(inflater, container, false);
        new EdgeSwipeBackGestureHelper().attachToView(binding.gestureOverlay, this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        postponeEnterTransition();
        super.onViewCreated(view, savedInstanceState);
        binding.trackResultRecyclerView.setTransitionGroup(false);
        bindView(view);
        Glide.with(requireContext())
                .load(album.artworkUrl)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(albumImageView);

        LinearLayout metadata_container = view.findViewById(R.id.metadata_container);
        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom);
        metadata_container.startAnimation(anim);


        // 3. 트랙 목록을 비동기로 가져옵니다.
        apiHelper = new ArtistApiHelper(getContext(), requireActivity());
        apiHelper.searchTrackByAlbum(null, album,  trackList -> {
            // 4. 데이터를 받으면 어댑터를 설정합니다.
            trackAdapter = new TrackAdapter(trackList, getContext(), this::showTrackDetails, this::addFavoriteSong, this::onTrackClick);
            trackAdapter.setShowImage(false);
            trackAdapter.setShowPosition(true);
            trackRecyclerView.setAdapter(trackAdapter);

            // 5. ✅ RecyclerView가 화면에 그려질 준비가 끝났을 때 전환을 시작합니다.
            // 이 시점이 이미지와 리스트가 모두 준비된 가장 확실한 타이밍입니다.
            view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // 리스너를 여러 번 호출하지 않도록 바로 제거해줍니다.
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    // 연기했던 모든 전환을 시작합니다!
                    isListReady = true;
                    if (isImageReady)
                        startPostponedEnterTransition();
                    return true;
                }
            });
        });

        setView();

    }

    public void artistClickEvent(String artistId){
        Bundle bundle = new Bundle();
        favoriteArtistViewModel.loadFavoriteArtistByArtistId(artistId, loaded -> {
            if (loaded != null){
                bundle.putParcelable("favorite_artist", loaded);
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_albumInfoFragment_to_artist_infoFragment, bundle);
            }
            else{
                ArtistApiHelper apiHelper = new ArtistApiHelper(getContext(), requireActivity());
                apiHelper.getArtist(null, artistId, artist -> {
                    FavoriteArtist favoriteArtist = new FavoriteArtist(artist);
                    bundle.putParcelable("favorite_artist", favoriteArtist);
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.navigate(R.id.action_albumInfoFragment_to_artist_infoFragment, bundle);
                });
            }
        });
    }

    public void trackItemClickEvent(Favorite favorite){
        Bundle bundle = new Bundle();
        bundle.putParcelable("favorite", favorite);
        NavController navController = Navigation.findNavController((requireView()));
        navController.navigate(R.id.action_searchFragment_to_musicInfoFragment, bundle);
    }

    private void bindView(View view){
        albumNameTextView = view.findViewById(R.id.album_name);
        artistNameTextView = view.findViewById(R.id.artist_name);
        releaseDateTextView = view.findViewById(R.id.release_date);
        totalTracksTextView = view.findViewById(R.id.total_tracks);
        trackRecyclerView = view.findViewById(R.id.track_result_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        album = getArguments().getParcelable("album");
        albumImageView = view.findViewById(R.id.artwork_image);
        String received = getArguments().getString("transitionName");
        albumImageView.setTransitionName(received);

        imageOverlayManager = new ImageOverlayManager(requireActivity(), view);
        enlargeButton = view.findViewById(R.id.enlarge_button);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void setView(){
        Glide.with(requireContext())
                .load(album.artworkUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_image_not_found_foreground)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        albumImageView.setImageDrawable(resource);
                        isImageReady = true;
                        if (isListReady)
                            startPostponedEnterTransition(); // ✅ 성공 시 전환 시작
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 필요하면 placeholder 정리
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        albumImageView.setImageResource(R.drawable.ic_image_not_found_foreground);
                        startPostponedEnterTransition(); // ✅ 실패해도 전환 시작
                    }
                });

        albumNameTextView.setText(album.albumName);
        artistNameTextView.setText(album.artistName);
        releaseDateTextView.setText(album.releaseDate);
        totalTracksTextView.setText(String.valueOf(album.totalTracks));

        artistNameTextView.setOnClickListener(v -> this.artistClickEvent(album.artistId));


        // 1. 롱클릭을 감지할 GestureDetector 생성
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                // 롱클릭이 감지되었을 때 실행될 코드
                // event 객체에서 화면 절대 좌표를 가져옵니다.
                float touchX = event.getRawX();
                float touchY = event.getRawY();

                // 매니저를 호출하여 오버레이와 애니메이션을 표시합니다.
                imageOverlayManager.showOverlay(albumImageView, album.artworkUrl, touchX, touchY);
            }
        });

        // 2. ImageView에 OnTouchListener 설정
        albumImageView.setOnTouchListener((v, motionEvent) -> {
            // 모든 터치 이벤트를 GestureDetector에 전달합니다.
            gestureDetector.onTouchEvent(motionEvent);
            imageOverlayManager.setDownloadButtonLocation(- (int)(albumImageView.getWidth()/6.5f), albumImageView.getWidth()/14);

            // true를 반환하여 이 이벤트가 여기서 처리되었음을 시스템에 알립니다.
            return true;
        });


        enlargeButton.setOnClickListener(v -> {
            // 1. 전환할 뷰(albumImageView)에 고유한 transitionName 설정
            // 이 이름은 도착 프래그먼트의 이미지 뷰도 동일하게 사용하게 됩니다.
            String transitionName = "Transition_" + album.artworkUrl; // 앨범마다 고유한 이름으로 설정
            ViewCompat.setTransitionName(albumImageView, transitionName);

            // 2. ImageDetailFragment에 전달할 데이터 준비
            // ViewPager2가 아니므로, 이미지는 현재 앨범 아트 하나
            ArrayList<String> imageUrls = new ArrayList<>();
            imageUrls.add(album.artworkUrl);
            int startPosition = 0;

            Bundle args = new Bundle();
            args.putStringArrayList("image_urls", imageUrls);
            args.putInt("start_position", startPosition);
            // 참고: ImageDetailFragment는 이 key값들("image_urls", "start_position")로 데이터를 꺼내 씀

            // 3. 전환 애니메이션 정보(Extras) 생성
            // 어떤 뷰를(albumImageView), 어떤 이름으로(transitionName) 보낼지 지정
            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(albumImageView, transitionName)
                    .build();

            // 4. NavController로 데이터(args)와 애니메이션 정보(extras)를 함께 전달하며 이동
            NavController navController = NavHostFragment.findNavController(this);
            // action_albumInfoFragment_to_imageDetailFragment는 navigation graph에 정의된 action id입니다.
            navController.navigate(R.id.action_albumInfoFragment_to_imageDetailFragment, args, null, extras);
        });


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


    public void onTrackClick(Track track, ImageView sharedImageView){
        Bundle bundle = new Bundle();
        Favorite favorite = new Favorite(track);
        bundle.putParcelable("favorite", favorite);
        String transitionName = "Transition_album_to_music" + album.artworkUrl;

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(albumImageView, transitionName)
                .build();

        //sharedImageView.setTransitionName(transitionName);
        bundle.putString("transitionName", transitionName);
        NavController navController = NavHostFragment.findNavController(this);
        NavDestination currentDestination = navController.getCurrentDestination();
        assert currentDestination != null;
        if (currentDestination.getId() == R.id.album_info)
            navController.navigate(R.id.musicInfoFragment, bundle, null, extras);

    }




}

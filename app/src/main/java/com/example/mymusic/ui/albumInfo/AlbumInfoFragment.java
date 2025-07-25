package com.example.mymusic.ui.albumInfo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Explode;
import androidx.transition.Slide;

import com.bumptech.glide.Glide;
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
import com.example.mymusic.util.ImageOverlayManager;
import com.google.android.material.transition.MaterialArcMotion;
import com.google.android.material.transition.MaterialContainerTransform;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;


public class  AlbumInfoFragment extends Fragment {
    private final String TAG = "AlbumInfoFragment";
    public static final String ARGUMENTS_KEY = "album";
    public static final String TRANSITION_NAME_KEY = "transitionName";
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
    public static final String REQUEST_KEY = "album_info_fragment_request";
    public static final String BUNDLE_KEY_TRANSITION_END = "transition_album_artwork_ended";

    private AlbumInfoViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstatceState){
        super.onCreate(savedInstatceState);

        //앨범 이미지 트랜지션 설정
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setPathMotion(new MaterialArcMotion());
        setSharedElementEnterTransition(transform);
        setSharedElementReturnTransition(transform);


        setEnterTransition(new Slide(Gravity.TOP));

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

        viewModel = new ViewModelProvider(this).get(AlbumInfoViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
    }

    @Override
    public void onPause(){
        super.onPause();
        setEnterTransition(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState){
        binding = FragmentAlbumInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        postponeEnterTransition();

        if (viewModel.getInitialTransitionName() == null){
            String receivedTransitionName = getArguments().getString(TRANSITION_NAME_KEY);
            viewModel.setInitialTransitionName(receivedTransitionName);
            ViewCompat.setTransitionName(binding.artworkImage, receivedTransitionName);
            Log.d(TAG, "navigation 으로 '전달받은' 이름 설정, name=" + receivedTransitionName);
        }else{
            String currentTransitionName = viewModel.getCurrentTransitionName();
            ViewCompat.setTransitionName(binding.artworkImage, currentTransitionName);
            Log.d(TAG, "viewModel 에 '저장된' current 이름 복원, name=" + currentTransitionName);
        }


        binding.trackResultRecyclerView.setTransitionGroup(false);
        bindView(view);

        if (albumImageView != null && albumImageView.getTransitionName() != null){
            Log.d(TAG, "현재 album transition name: " + albumImageView.getTransitionName());
        }

        Glide.with(requireContext())
                .load(album.artworkUrl)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        albumImageView.setImageDrawable(resource);
                        Log.d(TAG, "Image resource Ready and start postponed enter transition");
                        startPostponedEnterTransition(); // ✅ 성공 시 전환 시작
                        albumImageView.post(() -> {
                            Log.d(TAG, "set transition name to initial transition name after album imageview posted");
                            ViewCompat.setTransitionName(binding.artworkImage, viewModel.getInitialTransitionName());
                        });

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });

        LinearLayout metadata_container = view.findViewById(R.id.metadata_container);
        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom);
        metadata_container.startAnimation(anim);



        // 3. 트랙 목록을 비동기로 가져옵니다.
        apiHelper = new ArtistApiHelper(getContext());
        apiHelper.searchTrackByAlbum(null, album,  trackList -> {
            // 4. 데이터를 받으면 어댑터를 설정합니다.
            trackAdapter = new TrackAdapter(
                    trackList,
                    getContext(),
                    this::showTrackDetails,
                    this::addFavoriteSong,
                    this::onTrackClick);
            trackAdapter.setShowImage(false);
            trackAdapter.setShowPosition(true);
            trackRecyclerView.setAdapter(trackAdapter);

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
                ArtistApiHelper apiHelper = new ArtistApiHelper(getContext());
                apiHelper.getArtist(null, artistId, artist -> {
                    FavoriteArtist favoriteArtist = new FavoriteArtist(artist);
                    bundle.putParcelable("favorite_artist", favoriteArtist);
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.navigate(R.id.action_albumInfoFragment_to_artist_infoFragment, bundle);
                });
            }
        });
    }

    private void bindView(View view){
        albumNameTextView = view.findViewById(R.id.album_name);
        artistNameTextView = view.findViewById(R.id.artist_name);
        releaseDateTextView = view.findViewById(R.id.release_date);
        totalTracksTextView = view.findViewById(R.id.total_tracks);
        trackRecyclerView = view.findViewById(R.id.track_result_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        album = getArguments().getParcelable(ARGUMENTS_KEY);
        albumImageView = view.findViewById(R.id.artwork_image);
        imageOverlayManager = new ImageOverlayManager(requireActivity(), view);
        enlargeButton = view.findViewById(R.id.enlarge_button);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void setView(){
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
            String transitionName = "Transition_album_to_image_detail_" + album.artworkUrl;

            ViewCompat.setTransitionName(albumImageView, transitionName);
            viewModel.setCurrentTransitionName(transitionName);

            ArrayList<String> imageUrls = new ArrayList<>();
            imageUrls.add(album.artworkUrl);
            int startPosition = 0;

            Bundle args = new Bundle();
            args.putString("transitionName", transitionName);
            args.putStringArrayList("image_urls", imageUrls);
            args.putInt("start_position", startPosition);

            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(albumImageView, transitionName)
                    .build();

            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_albumInfoFragment_to_imageDetailFragment, args, null, extras);
        });


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
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_custom_only_dismiss_button);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);


        TextView dismissButton = dialog.findViewById(R.id.dismiss_button);
        TextView titleTextView = dialog.findViewById(R.id.title);
        TextView subTextView = dialog.findViewById(R.id.subtext);

        titleTextView.setText("세부사항");
        subTextView.setText("제목: " + track.trackName +
                "\n아티스트: " + track.artistName +
                "\n앨범: " + track.albumName +
                "\n발매일: " + track.releaseDate.substring(0, 10));

        dismissButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    public void onTrackClick(TrackAdapter.TrackViewHolder holder, Track track, int position){
        Bundle bundle = new Bundle();
        Favorite favorite = new Favorite(track);
        bundle.putParcelable("favorite", favorite);
        String transitionName = "Transition_album_to_music_" + album.artworkUrl;
        viewModel.setCurrentTransitionName(transitionName);

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(albumImageView, transitionName)
                .build();

        ViewCompat.setTransitionName(binding.artworkImage, transitionName);
        bundle.putString("transitionName", transitionName);
        NavController navController = NavHostFragment.findNavController(this);
        NavDestination currentDestination = navController.getCurrentDestination();
        assert currentDestination != null;
        if (currentDestination.getId() == R.id.album_info)
            navController.navigate(R.id.musicInfoFragment, bundle, null, extras);

    }



}

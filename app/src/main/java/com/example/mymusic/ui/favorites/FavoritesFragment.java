

package com.example.mymusic.ui.favorites;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.adapter.FavoriteArtistAdapter;
import com.example.mymusic.adapter.FavoritesAdapter;
import com.example.mymusic.adapter.FavoritesWithCardViewAdapter;
import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.databinding.FragmentFavoritesBinding;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;
import com.example.mymusic.network.ArtistMetadataService;
import com.example.mymusic.network.ArtistVibeLinkService;
import com.example.mymusic.network.LyricsSearchService;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;
import com.example.mymusic.ui.favorites.bottomSheet.FilterBottomSheetFragment;
import com.example.mymusic.ui.musicInfo.MusicInfoFragment;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;
import com.example.mymusic.util.SortFilterUtil;
import com.example.mymusic.util.VerticalSpaceItemDecoration;
import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import android.widget.LinearLayout;


import jp.wasabeef.recyclerview.animators.FadeInLeftAnimator;
import jp.wasabeef.recyclerview.animators.LandingAnimator;
import jp.wasabeef.recyclerview.animators.OvershootInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class FavoritesFragment extends Fragment {
    private final String TAG = "FavoriteFragment";
    private RecyclerView trackRecyclerView, artistRecyclerView;
    private FavoritesViewModel favoritesViewModel;
    private FavoriteArtistViewModel favoriteArtistViewModel;
    FavoritesAdapter favoriteTrackAdapter;
    FavoriteArtistAdapter favoriteArtistAdapter;
    FavoritesWithCardViewAdapter artistsOtherMusicAdapter;
    TextView emptyFavoriteSongTextView, emptyFavoriteArtistTextView, favoritesLoadedCountTextView;
    public int favoriteOption = 0; // 기본값: track
    private TextView elementCountTextView;
    private Boolean isSelectionMode = false;
    private WebView webView, webView2;
    private FilterBottomSheetFragment bottomSheet;

    TextView lyricsTextView, onLyricsTitleTextView, getOnLyricsArtistTextView;
    ScrollView scrollAreaView;
    LinearLayout countLayout, onLyricsContainer;
    ImageButton lyricsModeCancelButton;
    ImageView focusedImageView;
    TextView focusedTitleTextView, focusedAlbumTextView, focusedArtistTextView, focusedDurationTextView, focusedReleaseDateTextView;
    MaterialCardView musicInfoContainer, simpleMusicInfoContainer, lyricsTextContainer, lyricsContainer;
    TextView cancelSelectionModeTextView, removeSelectedFavoritesTextView;
    SwitchCompat favoriteOptionSwitch;
    private FragmentFavoritesBinding binding;
    private ImageButton filterImageButton, dropDownImageButton, dropUpImageButton;
    private String currentCount = "";
    private RecyclerView artistsOtherMusicRecyclerView;
    private MaterialCardView artistsOtherMusicCardView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoritesViewModel = new ViewModelProvider(requireActivity()).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(requireActivity()).get(FavoriteArtistViewModel.class);

        getParentFragmentManager().setFragmentResultListener(MusicInfoFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (requestKey.equals(MusicInfoFragment.REQUEST_KEY)){
                boolean transitionEnded = bundle.getBoolean(MusicInfoFragment.BUNDLE_KEY_TRANSITION_END);
                if (transitionEnded){
                    setRecyclerViewAnimation();
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(ArtistInfoFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (requestKey.equals(ArtistInfoFragment.REQUEST_KEY)){
                boolean transitionEnded = bundle.getBoolean(ArtistInfoFragment.BUNDLE_KEY_TRANSITION_END);
                if (transitionEnded){
                    setArtistRecyclerViewAnimation();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        Log.d(TAG, "onViewCreated() 호출됨");
        super.onViewCreated(view, savedInstanceState);

        //가사 보이는 상태에서 뒤로가기 제어
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        postponeEnterTransition();



        //No adapter attached 오류 해결을 위해 먼저 빈 리스트 전달하고 나중에 데이터 받고 나서 업데이트해주기
        favoriteTrackAdapter = new FavoritesAdapter(
                new ArrayList<>(), // ⬅️ 빈 리스트 전달
                this::deleteFavoriteSong,
                this::onLyricsClick,
                this::onLyricLongClick,
                this::onItemLongClick,
                this::handleItemNavigation
        );

        favoriteArtistAdapter = new FavoriteArtistAdapter(
                new ArrayList<>(),
                this::deleteFavoriteArtist,
                this::addArtistMetadata,
                favoriteArtistViewModel,
                this::handleItemNavigationForArtist,
                this::addSelected,
                this::removeSelected);

        Context context = getContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            boolean favoritesTrackColorUnificationState = prefs.getBoolean("favorites_track_color_unification_state", false);

            artistsOtherMusicAdapter = new FavoritesWithCardViewAdapter(
                    context,
                    new ArrayList<>(),
                    this::onLyricsClick,
                    favoritesTrackColorUnificationState
            );
        }

        elementCountTextView = view.findViewById(R.id.element_count);
        elementCountTextView.setText("Songs");

        lyricsTextView = view.findViewById(R.id.metadata_lyrics);
        onLyricsTitleTextView = view.findViewById(R.id.on_lyrics_title);
        getOnLyricsArtistTextView = view.findViewById(R.id.on_lyrics_artist);
        scrollAreaView = view.findViewById(R.id.scroll_area);
        if (scrollAreaView != null)
            OverScrollDecoratorHelper.setUpOverScroll(scrollAreaView);


        trackRecyclerView = binding.trackRecyclerView;
        trackRecyclerView.setVisibility(View.VISIBLE);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        trackRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int position = layoutManager.findFirstVisibleItemPosition();
                    View firstItemView = layoutManager.findViewByPosition(position);
                    int offset = (firstItemView != null) ? firstItemView.getTop() : 0;

                    favoritesViewModel.setScrollPosition(position);
                    favoritesViewModel.setScrollOffset(offset);
                }
            }
        });

        setRecyclerViewAnimation();


        artistRecyclerView = binding.artistRecyclerView;
        artistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        artistRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
                if (layoutManager != null){
                    int position = layoutManager.findFirstVisibleItemPosition();
                    View firstItemView = layoutManager.findViewByPosition(position);
                    int offset = (firstItemView != null) ? firstItemView.getTop() : 0;

                    favoriteArtistViewModel.setScrollPosition(position);
                    favoriteArtistViewModel.setScrollOffset(offset);
                }
            }
        });

        setArtistRecyclerViewAnimation();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            artistsOtherMusicRecyclerView = binding.artistsOtherMusicRecycler;
            artistsOtherMusicRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            // dp 값을 px로 변환
            int spacingDp = -11;
            int spacingPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    spacingDp,
                    artistsOtherMusicRecyclerView.getResources().getDisplayMetrics()
            );

            // 데코레이션 추가
            artistsOtherMusicRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(spacingPx));
        }




        //switch toggle event
        favoriteOptionSwitch = view.findViewById(R.id.favorite_option_switch);
        favoriteOptionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                favoriteOption = 0; //track
                trackRecyclerView.setVisibility(View.VISIBLE);
                artistRecyclerView.setVisibility(View.GONE);
                onLyricsContainer.setVisibility(View.GONE);

            }
            else {
                favoriteOption = 1; //artist
                artistRecyclerView.setVisibility(View.VISIBLE);
                trackRecyclerView.setVisibility(View.GONE);
                onLyricsContainer.setVisibility(View.GONE);
            }
            loadFavoritesAndUpdateUI();
        });


        emptyFavoriteSongTextView = view.findViewById(R.id.empty_favorite_song);
        emptyFavoriteArtistTextView = view.findViewById(R.id.empty_favorite_artist);
        favoritesLoadedCountTextView = view.findViewById(R.id.favorites_loaded_count);
        webView = view.findViewById(R.id.hidden_web_view);
        webView2 = view.findViewById(R.id.hidden_web_view_2);
        lyricsContainer = view.findViewById(R.id.lyrics_container);
        lyricsModeCancelButton = view.findViewById(R.id.cancel_button);
        countLayout = view.findViewById(R.id.count_layout);
        onLyricsContainer = view.findViewById(R.id.on_lyrics_container);
        onLyricsContainer.setVisibility(View.GONE);
        musicInfoContainer = view.findViewById(R.id.music_info_container);
        musicInfoContainer.setClipToOutline(true);
        focusedImageView = view.findViewById(R.id.focused_image);
        focusedTitleTextView = view.findViewById(R.id.focused_title);
        focusedAlbumTextView = view.findViewById(R.id.focused_album_title);
        focusedArtistTextView = view.findViewById(R.id.focused_artist);
        focusedDurationTextView = view.findViewById(R.id.focused_duration);
        focusedReleaseDateTextView = view.findViewById(R.id.focused_release_date);
        simpleMusicInfoContainer = view.findViewById(R.id.simple_music_info);
        lyricsTextContainer = view.findViewById(R.id.lyrics_text_container);
        cancelSelectionModeTextView = view.findViewById(R.id.cancel_selection_mode);
        removeSelectedFavoritesTextView = view.findViewById(R.id.remove_selected_favorites);
        bottomSheet = new FilterBottomSheetFragment();
        filterImageButton = view.findViewById(R.id.filter_button);
        dropDownImageButton = view.findViewById(R.id.in_order_button_drop_down);
        dropUpImageButton = view.findViewById(R.id.in_order_button_drop_up);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            artistsOtherMusicRecyclerView = view.findViewById(R.id.artists_other_music_recycler);
            artistsOtherMusicCardView = view.findViewById(R.id.artists_other_music_card_view);
        }


        trackRecyclerView.setAdapter(favoriteTrackAdapter);
        artistRecyclerView.setAdapter(favoriteArtistAdapter);

        if(favoriteOption == 0) {//track
            Log.d(TAG, "OnViewCreatied-  option 0 (Favorites Tracks)");
            emptyFavoriteArtistTextView.setVisibility(View.GONE);
            loadFavoritesAndUpdateUI();
        }
        else if(favoriteOption == 1) {//artist
            Log.d(TAG, "OnViewCreatied-  option 1 (Favorites Artists)");
            emptyFavoriteSongTextView.setVisibility(View.GONE);
            loadFavoritesAndUpdateUI();
        }


        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
            boolean isDescending = prefs.getBoolean("isDescending", false);
            if (isDescending){
                dropUpImageButton.setVisibility(View.GONE);
                dropDownImageButton.setVisibility(View.VISIBLE);
            }else{
                dropDownImageButton.setVisibility(View.GONE);
                dropUpImageButton.setVisibility(View.VISIBLE);
            }
        }

        lyricsModeCancelButton.setOnClickListener(v -> {
            cancelLyricsMode();
        });


        //다중삭제 취소 버튼
        cancelSelectionModeTextView.setOnClickListener(v ->{
          handleCancelSelectionMode();
        });

        removeSelectedFavoritesTextView.setOnClickListener(v -> {
            if (favoriteOption == 0){
                List<Favorite> selectedList;
                selectedList = favoriteTrackAdapter.getSelectedList();
                if (selectedList.isEmpty()) return;
                StringBuilder selected = new StringBuilder();
                for (Favorite item : selectedList) {
                    if (item.metadata != null && item.metadata.title != null && !item.metadata.title.isEmpty()) {
                        selected.append(item.metadata.title + " - " + item.track.artistName + "\n");
                    } else {
                        selected.append(item.track.trackName + " - " + item.track.artistName + "\n");
                    }
                }
                String mentByCount = selectedList.size() == 1 ? "을(를) 삭제하시겠습니까?" : selectedList.size() + "개의 노래를 모두 삭제하시겠습니까?";
                new AlertDialog.Builder(getContext())
                        .setTitle("삭제")
                        .setMessage("정말\n" + selected + mentByCount)
                        .setPositiveButton("삭제", ((dialog, which) -> {
                            List<String> selectedIds = new ArrayList<>();
                            for (Favorite favorite : selectedList) {
                                selectedIds.add(favorite.track.trackId);
                            }
                            favoritesViewModel.deleteFavoritesByIds(selectedIds, result -> {
                                if (result > 0) {
                                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), result + "개의 노래가 삭제되었습니다.", Toast.LENGTH_SHORT));
                                    favoritesViewModel.loadAllFavorites(favorites -> {
                                        if (context != null) {
                                            List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(context, favorites, null);
                                            favoriteTrackAdapter.updateData(filtered);
                                            //count 업데이트
                                            setFavoritesCountText(filtered.size());
                                            Log.d(TAG, "sort and filter successfully and updated list size");

                                        }
                                    });


                                } else {
                                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), "삭제되지 않았습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT));
                                    favoritesViewModel.getFavoritesCount(count -> setFavoritesCountText(count));
                                }
                                favoriteTrackAdapter.setSelectionMode(false);
                                for (Favorite fv : selectedList) {
                                    fv.recyclerViewPosition = -1;
                                }
                                //selectedList = new ArrayList<>();
                                cancelSelectionModeTextView.setVisibility(View.GONE);
                                removeSelectedFavoritesTextView.setVisibility(View.GONE);
                                isSelectionMode = false;


                            });


                        }))
                        .setNegativeButton("취소", null)
                        .show();

            }
            else if (favoriteOption == 1){
                List<Artist> selectedList = favoriteArtistViewModel.selectedList;
                if (selectedList.isEmpty()) return;
                StringBuilder selectedStr = new StringBuilder();
                for (Artist item : selectedList) {
                    if (item.artistName != null && !item.artistName.isEmpty()) {
                        selectedStr.append(item.artistName + "\n");
                    }
                }
                String mentByCount = selectedList.size() == 1 ? "을(를) 삭제하시겠습니까?" : selectedList.size() + "팀의 아티스트를 모두 삭제하시겠습니까?";
                new AlertDialog.Builder(getContext())
                        .setTitle("삭제")
                        .setMessage("정말\n" + selectedStr + mentByCount)
                        .setPositiveButton("삭제", ((dialog, which) -> {
                            List<String> selectedIds = new ArrayList<>();
                            for (Artist artist : selectedList) {
                                selectedIds.add(artist.artistId);
                            }
                            favoriteArtistViewModel.deleteFavoritesArtistByIds(selectedIds, result -> {
                                if (result > 0) {
                                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), result + "팀의 아티스트가 삭제되었습니다.", Toast.LENGTH_SHORT));
                                    favoriteArtistViewModel.loadFavoritesOriginalForm(favorites -> {
                                        List<FavoriteArtist> copy = new ArrayList<>(favorites);
                                        Collections.reverse(copy);
                                        favoriteArtistAdapter.updateData(copy);
                                        //count 업데이트
                                        setFavoritesCountText(favorites.size());

                                    });
                                } else {
                                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), "삭제되지 않았습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT));
                                    favoriteArtistViewModel.getFavoriteArtistsCount(count -> setFavoritesCountText(count));
                                }
                                favoriteArtistAdapter.setSelectionMode(false);
                                favoriteArtistViewModel.selectedList = new ArrayList<>();
                                cancelSelectionModeTextView.setVisibility(View.GONE);
                                removeSelectedFavoritesTextView.setVisibility(View.GONE);
                                isSelectionMode = false;
                            });


                        }))
                        .setNegativeButton("취소", null)
                        .show();
            }
        });


        simpleMusicInfoContainer.setOnClickListener(v -> {
            Log.d(TAG, "simpleMusicInfoContainer click event arised");
            Favorite favorite = favoritesViewModel.getFocusedTrack().getValue();
            if (favorite == null) {
                Log.d(TAG, "can not navigate to music_info_fragment, focused favorite track is null");
                return;
            }

            String transitionName = "Transition_favorites_to_music_simple" + favorite.track.artworkUrl + "_" + favorite.track.trackId;
            focusedImageView.setTransitionName(transitionName);
            favoritesViewModel.setFocusedTransitionName(transitionName);
            favoritesViewModel.setTransitionPosition(-2);

            LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
            if (layoutManager != null) {
                int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                View firstItemView = layoutManager.findViewByPosition(firstVisiblePosition);
                int offset = (firstItemView != null) ? firstItemView.getTop() : 0;
                favoritesViewModel.setReenterScrollPosition(firstVisiblePosition);
                favoritesViewModel.setReenterScrollOffset(offset);
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable("favorite", favorite);
            bundle.putString("transitionName", transitionName);

            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(focusedImageView, transitionName)
                    .build();

            Navigation.findNavController(requireView()).navigate(
                    R.id.action_favoritesFragment_to_musicInfoFragment,
                    bundle,
                    null,
                    extras
            );
        });


        //is lyrics mode인 경우 (다른 fragment로 갔다가 돌아올 때)
        Favorite focused = favoritesViewModel.getFocusedTrack().getValue();
        Boolean isLyrics = favoritesViewModel.getLyricsMode().getValue();
        if (isLyrics != null && isLyrics && focused != null){
            Log.d(TAG, "lyrics on mode");
            LyricsOnMode(focused, favoritesViewModel.getFocusedPosition());
            loadFavoritesAndUpdateUI();
            if (favoritesViewModel.getFocusedTransitionName() != null){
                ViewCompat.setTransitionName(focusedImageView, favoritesViewModel.getFocusedTransitionName());
            }
        }


        bottomSheet.setApplyListener(new FilterBottomSheetFragment.OnApplyListener() {
            @Override
            public void onApply() {
                loadFavoritesAndUpdateUI();
            }
        });

        filterImageButton.setOnClickListener(v -> {
            if (!bottomSheet.isAdded() && !bottomSheet.isVisible()) {
                bottomSheet.show(getParentFragmentManager(), "FilterBottomSheet");
            }
        });


        dropDownImageButton.setOnClickListener(v -> {
            dropDownImageButton.setVisibility(View.GONE);
            dropUpImageButton.setVisibility(View.VISIBLE);
            if (context != null){
                SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isDescending", false);
                editor.apply();
                Log.d(TAG, "isDescending false preference stored");
                loadFavoritesAndUpdateUI();
            }else{
                Log.d(TAG, "fail to getContext(), can not update list");
            }
        });

        dropUpImageButton.setOnClickListener(v -> {
            dropUpImageButton.setVisibility(View.GONE);
            dropDownImageButton.setVisibility(View.VISIBLE);
            if (context != null){
                SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isDescending", true);
                editor.apply();
                Log.d(TAG, "isDescending true preference stored");
                loadFavoritesAndUpdateUI();
            }else{
                Log.d(TAG, "fail to getContext(), can not update list");
            }
        });


//onViewCreated
    }

    private void setArtistRecyclerViewAnimation(){
        OvershootInLeftAnimator animator = new OvershootInLeftAnimator();
        animator.setRemoveDuration(200); // 제거 애니메이션 시간
        animator.setAddDuration(200); // 추가 애니메이션 시간
        if (artistRecyclerView != null)
            artistRecyclerView.setItemAnimator(animator);
    }

    private void setRecyclerViewAnimation() {
        //SlideInUpAnimator animator = new SlideInUpAnimator();
        //SlideInLeftAnimator animator = new SlideInLeftAnimator();
        //LandingAnimator animator = new LandingAnimator();
        OvershootInLeftAnimator animator = new OvershootInLeftAnimator();
        animator.setRemoveDuration(200); // 제거 애니메이션 시간
        animator.setAddDuration(200); // 추가 애니메이션 시간
        if (trackRecyclerView != null)
            trackRecyclerView.setItemAnimator(animator);
    }

    // 화면 업데이트 함수
    private void loadFavoritesAndUpdateUI() {
        if (favoriteOption == 0) {
            artistRecyclerView.setVisibility(View.GONE);
            favoritesViewModel.loadAllFavorites(favoritesList -> {
                if (!favoritesList.isEmpty())
                    trackRecyclerView.setVisibility(View.VISIBLE);
                else
                    trackRecyclerView.setVisibility(View.GONE);

                Context context = getContext();
                if (context != null) {
                    List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(context, favoritesList, null);
                    favoriteTrackAdapter.updateData(filtered);
                    //count 업데이트
                    int size = filtered.size();
                    if (size == 0){
                        emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                    }else{
                        emptyFavoriteSongTextView.setVisibility(View.GONE);
                    }
                    setFavoritesCountText(size);
                    updateEmptyState(filtered.isEmpty());
                    Log.d(TAG, "sort and filter successfully and updated list size");
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
                if (layoutManager != null){
                    layoutManager.scrollToPositionWithOffset(favoritesViewModel.getScrollPosition(), favoritesViewModel.getScrollOffset());
                    Log.d(TAG, "loadFavoritesAndUpdateUI called, set scrollPositoin: "
                            + favoritesViewModel.getScrollPosition() + " , scrollOffset: " + favoritesViewModel.getScrollOffset());
                }


                handleReenterTransition();
            });
        } else {
            trackRecyclerView.setVisibility(View.GONE);
            favoriteArtistViewModel.loadFavoritesOriginalForm(favoriteArtistList -> {
                if (!favoriteArtistList.isEmpty())
                    artistRecyclerView.setVisibility(View.VISIBLE);
                else {
                    Log.d(TAG, "Artist List is empty");
                    artistRecyclerView.setVisibility(View.GONE);
                }
                List<FavoriteArtist> reversed = new ArrayList<>(favoriteArtistList);
                Collections.reverse(reversed);
                updateEmptyState(reversed.isEmpty());
                favoriteArtistAdapter.updateData(reversed);

                LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
                if (layoutManager != null){
                    layoutManager.scrollToPositionWithOffset(favoriteArtistViewModel.getScrollPosition(), favoriteArtistViewModel.getScrollOffset());
                }

                setFavoritesCountText(reversed.size());
                handleReenterTransition();
            });
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        // 먼저 모두 GONE 처리해서 겹침 방지
        emptyFavoriteSongTextView.setVisibility(View.GONE);
        emptyFavoriteArtistTextView.setVisibility(View.GONE);

        if (favoriteOption == 0){
            artistRecyclerView.setVisibility(View.GONE);
            if (isEmpty)
                emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
            else if(Boolean.TRUE.equals(favoritesViewModel.getLyricsMode().getValue())){
                trackRecyclerView.setVisibility(View.GONE);
            } else{
                trackRecyclerView.setVisibility(View.VISIBLE);
            }

        }
        else if (favoriteOption == 1){
            trackRecyclerView.setVisibility(View.GONE);
            if (isEmpty)
                emptyFavoriteArtistTextView.setVisibility(View.VISIBLE);
            else{
                artistRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }




    void deleteFavoriteSong(Favorite favorite){
        Track track = favorite.track;
        String trackName = track.trackName;
        if (favorite.metadata != null && favorite.metadata.title != null){
            trackName = favorite.metadata.title;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("삭제")
                .setMessage("정말 " + trackName + " - " + track.artistName + " 을(를) 삭제하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        List<String> ids = new ArrayList<>();
                        ids.add(track.trackId);
                        favoritesViewModel.deleteFavoritesByIds(ids, result -> {
                            if (result > 0){
                                //Toast.makeText(getContext(),track.trackName + " - " + track.artistName + " 이(가) Favorites List 에서 삭제되었습니다.",Toast.LENGTH_SHORT).show();
                                favoritesViewModel.loadAllFavorites(updatedList -> {
                                    if (updatedList.isEmpty()) {
                                        emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        emptyFavoriteSongTextView.setVisibility(View.GONE);
                                    }
                                    Context context = getContext();
                                    if (context != null){
                                        List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(context, updatedList, null);
                                        favoriteTrackAdapter.updateData(filtered);
                                        updateEmptyState(filtered.isEmpty());
                                        setFavoritesCountText(filtered.size());
                                        Log.d(TAG, "sort and filter successfully and updated list size");
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
    }


    public void deleteFavoriteArtist(Artist artist) {
        // --- AlertDialog에 삽입할 커스텀 뷰를 Java 코드로 직접 생성 시작 ---
        // 최상위 레이아웃 (수직 방향)
        LinearLayout customLayout = new LinearLayout(getContext());
        customLayout.setOrientation(LinearLayout.VERTICAL);

        // 패딩 설정 (dp 값을 픽셀로 변환)
        float density = getContext().getResources().getDisplayMetrics().density;
        int paddingHorizontalPx = (int) (24 * density); // 좌우 24dp
        int paddingVerticalPx = (int) (12 * density);   // 상하 12dp
        customLayout.setPadding(paddingHorizontalPx, paddingVerticalPx, paddingHorizontalPx, paddingVerticalPx);

        // 메시지 TextView 생성
        TextView messageTextView = new TextView(getContext());
        messageTextView.setText("정말 " + artist.artistName + " 을(를) 삭제하시겠습니까?");
        // 텍스트 색상 설정 (예시: 기본 시스템 텍스트 색상)
        messageTextView.setTextColor(getContext().getResources().getColor(android.R.color.tab_indicator_text, getContext().getTheme()));
        messageTextView.setTextSize(16); // 텍스트 크기 16sp
        // 메시지 TextView의 하단 패딩 (16dp)
        int messageBottomPaddingPx = (int) (16 * density);
        messageTextView.setPadding(0, 0, 0, messageBottomPaddingPx);

        // 체크박스 생성
        CheckBox deleteMetadataCheckBox = new CheckBox(getContext());
        deleteMetadataCheckBox.setText("metadata와 함께 삭제");

        // 체크박스를 오른쪽에 정렬하기 위한 LayoutParams 설정
        LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        checkBoxParams.gravity = android.view.Gravity.END; // 오른쪽 정렬
        deleteMetadataCheckBox.setLayoutParams(checkBoxParams);

        // 생성된 뷰들을 레이아웃에 추가
        customLayout.addView(messageTextView);
        customLayout.addView(deleteMetadataCheckBox);
        // --- AlertDialog에 삽입할 커스텀 뷰 생성 끝 ---


        // AlertDialog 빌더를 사용하여 다이얼로그 생성
        new AlertDialog.Builder(getContext())
                .setTitle("삭제")
                .setView(customLayout) // Java 코드로 만든 커스텀 뷰를 다이얼로그에 설정
                .setNegativeButton("취소", null) // 취소 버튼
                .setPositiveButton("확인", (dialog, which) -> {
                    // 사용자가 체크박스를 체크했는지 확인
                    boolean shouldDeleteMetadata = deleteMetadataCheckBox.isChecked();

                    if (shouldDeleteMetadata) {
                        Log.d(TAG, "delete artist with metadata");
                        // "metadata와 함께 삭제"가 체크된 경우의 로직
                        // ViewModel에 해당 아티스트의 메타데이터도 함께 삭제하는 메서드를 호출 (이 메서드는 ViewModel에 새로 구현해야 함)
                        favoriteArtistViewModel.deleteArtistMetadataBySpotifyId(artist.artistId, message -> {
                            if (message.contains("Success")){
                                Log.d(TAG, "delete metadata Success");
                            }
                            else{
                                Log.d(TAG, "delete metadata Fail");
                                Toast.makeText(getContext(),
                                        artist.artistName + "의 메타데이터 삭제 실패",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                    // 체크되지 않은 경우 (기존 로직: 즐겨찾기 목록에서만 삭제)
                    favoriteArtistViewModel.deleteFavoriteArtist(artist.artistId, result -> {
                        if (result > 0){
                            // 삭제 후 즐겨찾기 목록 새로고침 (공통 로직)
                            favoriteArtistViewModel.loadFavoritesOriginalForm(updatedList -> {
                                List<FavoriteArtist> reversed = new ArrayList<>(updatedList);
                                Collections.reverse(reversed);

                                // 목록 상태에 따라 UI 업데이트 (empty/visible)
                                if (updatedList.isEmpty()) {
                                    emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                                } else {
                                    emptyFavoriteSongTextView.setVisibility(View.GONE);
                                }
                                favoriteArtistAdapter.updateData(reversed); // RecyclerView 데이터 업데이트
                                // updateEmptyState(updatedList.isEmpty()); // 이 메서드가 별도로 있다면 호출
                                setFavoritesCountText(reversed.size());
                            });
                        }
                    });
                })
                .show(); // 다이얼로그 표시
    }



    private void onLyricLongClick(String trackIdDb, String trackName){
        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null){
                addLyric(trackIdDb, trackName, true);
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("가사정보 없음")
                        .setMessage("링크를 입력해 가사를 추가하세요")
                        .setPositiveButton("닫기", null)
                        .show();
            }
        });

    }
    private void onLyricsClick(String trackIdDb, String trackName, int recyclerViewPosition){

        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null && !favorite.metadata.lyrics.isEmpty()){
                LyricsOnMode(favorite, recyclerViewPosition);
            } else {
                addLyric(trackIdDb, trackName, false);
            }
        });
    }

    private void LyricsOnMode(Favorite favorite, int recyclerViewPosition){
        if (getView() == null || lyricsModeCancelButton == null) {
            Log.w(TAG, "View not ready yet, skipping LyricsOnMode()");
            return;
        }
        toggleLyricsVisibility(true);
        favoritesViewModel.setLyricsMode(true);
        favoritesViewModel.setFocusedTrack(favorite);
        favoritesViewModel.setFocusedPosition(recyclerViewPosition);

        if (favorite != null) {
            Log.d(TAG, "focused favorite element saved");
        }
        else{
            Log.d(TAG, "null point saved in focused favorite");
        }
        Track track = favorite.track;
        lyricsModeCancelButton.setVisibility(View.VISIBLE);
        countLayout.setVisibility(View.GONE);
        trackRecyclerView.setVisibility(View.GONE);
        favoriteOptionSwitch.setVisibility(View.GONE);
        onLyricsContainer.setVisibility(View.VISIBLE);

        int[] currentPrimaryColor = {0};
        ImageColorAnalyzer.analyzePrimaryColor(requireContext(), track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
            @Override
            public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                currentPrimaryColor[0] = primaryColor;

                int darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
                int adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor);
                simpleMusicInfoContainer.setCardBackgroundColor(adjustedForWhiteText);
                lyricsTextContainer.setCardBackgroundColor(adjustedForWhiteText);
                musicInfoContainer.setCardBackgroundColor(primaryColor);
                lyricsContainer.setCardBackgroundColor(primaryColor);
                lyricsTextView.setTextColor(MyColorUtils.adjustForWhiteText(MyColorUtils.getSoftWhiteTextColor(adjustedForWhiteText)));
                //lyricsTextView.setShadowLayer(0.25f, 0.25f, 0.25f, Color.BLACK);
                artistsOtherMusicAdapter.setTextColor(MyColorUtils.adjustForWhiteText(MyColorUtils.getSoftWhiteTextColor(adjustedForWhiteText)));
                artistsOtherMusicAdapter.setPrimaryBackgroundColor(adjustedForWhiteText);


                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && artistsOtherMusicRecyclerView != null && artistsOtherMusicCardView != null) {
                    artistsOtherMusicCardView.setCardBackgroundColor(adjustedForWhiteText);
                    artistsOtherMusicRecyclerView.setAdapter(artistsOtherMusicAdapter);



                    favoritesViewModel.loadAllFavorites(favoriteList -> {
                        if (!favoriteList.isEmpty()){
                            boolean isDescending = true;
                            List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(getContext(), favoriteList, "ARTIST", track, "RELEASE_DATE", isDescending);
                            artistsOtherMusicAdapter.updateData(filtered);
                            if (filtered.isEmpty()){
                                artistsOtherMusicCardView.setVisibility(View.GONE);
                            } else{
                                artistsOtherMusicCardView.setVisibility(View.VISIBLE);
                            }

                        }
                    });


                }
            }

            @Override
            public void onFailure() {
                Log.d(TAG, "Fail to Analyze Primary Color of Album image");
            }
        });



        Picasso.get()
                .load(track.artworkUrl)
                .error(R.drawable.ic_image_not_found_foreground)
                .into(focusedImageView);

        if (favorite.metadata != null && favorite.metadata.title != null && !favorite.metadata.title.isEmpty()){
            focusedTitleTextView.setText(favorite.metadata.title);
        }
        else {
            focusedTitleTextView.setText(track.trackName);
        }
        focusedAlbumTextView.setText(track.albumName);
        focusedArtistTextView.setText(track.artistName);
        focusedDurationTextView.setText(track.durationToString());
        focusedReleaseDateTextView.setText(track.releaseDate);

        lyricsTextView.setText(favorite.metadata.lyrics);
        if (favorite.metadata != null && favorite.metadata.title != null && !favorite.metadata.title.isEmpty()) {
            onLyricsTitleTextView.setText(favorite.metadata.title);
        } else {
            onLyricsTitleTextView.setText(favorite.track.trackName);
        }
        getOnLyricsArtistTextView.setText(favorite.track.artistName);
    }



    private void addArtistMetadata(String artistId){
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);


        final EditText input = new EditText(getContext());
        input.setHint("https://vibe.naver.com/artist/# (#에 해당하는 숫자만 입력해도 괜찮습니다)");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        int originalInputType = input.getInputType();

        //NumberPad 세팅값 load
        SettingRepository settingRepository = new SettingRepository(requireContext());
        new Thread(() -> {
            boolean numericPadMode = settingRepository.getNumericPreference();
            if (numericPadMode){
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            else{
                input.setInputType(originalInputType);
            }

        }).start();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);

        layout.addView(input);

        AlertDialog dialog1 = new AlertDialog.Builder(getContext())
                .setTitle("Artist Metadata 추가")
                .setView(layout)
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", ((dialog, which) -> {
                    ArtistMetadataService.fetchMetadata(webView, input.getText().toString().trim() , new ArtistMetadataService.MetadataCallback() {
                        @Override
                        public void onSuccess(ArtistMetadata metadata) {
                            if (getActivity() != null){
                                getActivity().runOnUiThread(() -> {
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("아래 정보를 저장하시겠습니까?")
                                            .setMessage(metadata.toString())
                                            .setPositiveButton("저장", (dialog2, which2) -> {
                                                //ArtistMetadata 수정
                                                metadata.spotifyArtistId = artistId;
                                                metadata.vibeArtistId = input.getText().toString().trim();

                                                if (metadata.images != null && !metadata.images.isEmpty()) {
                                                    List<String> cleanedImages = new ArrayList<>();
                                                    for (String url : metadata.images) {
                                                        if (url == null) {
                                                            cleanedImages.add(null); // null 방어
                                                            continue;
                                                        }
                                                        int idx = url.indexOf("?type=");
                                                        if (idx != -1) {
                                                            cleanedImages.add(url.substring(0, idx)); // 자름
                                                        } else {
                                                            cleanedImages.add(url); // 그대로 유지
                                                        }
                                                    }
                                                    metadata.images = cleanedImages; // 원래 리스트 교체
                                                }

                                                if (metadata.members != null && !metadata.members.isEmpty()) {
                                                    for (int i = 0 ; i < metadata.members.size(); i++){
                                                        List<String> pair = metadata.members.get(i);
                                                        if (pair.get(1) == null || pair.get(1).isEmpty())
                                                            continue;
                                                        int id = Integer.parseInt(pair.get(1));
                                                        String padded = String.format("%06d", id); // 최소 6자리 패딩
                                                        String key = String.format("%06d", id / 1000);
                                                        String folder1 = key.substring(0, 3);    // 앞 3자리
                                                        String folder2 = key.substring(3, 6);    // 뒤 3자리
                                                        String link = "https://musicmeta-phinf.pstatic.net/artist/" + folder1 + "/" + folder2 + "/" + id + ".jpg";
                                                        metadata.members.get(i).set(2, link);
                                                    }
                                                }

                                                Log.d("FavoritesFragment", "수정된 metadata : " + metadata.toString());
                                                favoriteArtistViewModel.addArtistMetadata(metadata, new FavoriteArtistViewModel.addMetadataCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
                                                    }

                                                    @Override
                                                    public void onFailure(ArtistMetadata metadata, String reason) {
                                                        if (reason.contains("Spotify")) {
                                                            requireActivity().runOnUiThread(() -> {
                                                                new AlertDialog.Builder(getContext())
                                                                        .setTitle("Artist 중복")
                                                                        .setMessage("Metadata 정보가 이미 있습니다.\n삭제 후 다시 시도하세요.")
                                                                        .setNegativeButton("확인", null)
                                                                        .show();

                                                            });
                                                        }
                                                        else if (reason.contains("Vibe")){
                                                            favoriteArtistViewModel.getArtistNameById(metadata.spotifyArtistId, artistName -> {
                                                                requireActivity().runOnUiThread(() -> {
                                                                    new AlertDialog.Builder(getContext())
                                                                            .setTitle("Metadata 중복")
                                                                            .setMessage("Metadata 정보가 " + artistName +  " 에 연결되어있습니다.\n삭제 후 다시 시도하세요.")
                                                                            .setNegativeButton("확인", null)
                                                                            .show();
                                                                });
                                                            });
                                                        }
                                                    }
                                                });

                                            })
                                            .setNegativeButton("닫기", null)
                                            .show();
                                });
                            }
                        }

                        @Override
                        public void onFailure(String reason) {

                        }
                    });
                }))
                .create();




        dialog1.show();

    }

    private void addLyric(String trackIdDb, String trackName, boolean editMode){
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);


        final EditText input = new EditText(getContext());
        input.setHint("https://vibe.naver.com/track/# (#에 해당하는 숫자만 입력해도 괜찮습니다)");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final EditText input_lyrics = new EditText(getContext());
        input_lyrics.setHint("가사 입력");
        input_lyrics.setInputType(InputType.TYPE_CLASS_TEXT);
        //input_lyrics.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final EditText input_titleKr = new EditText(getContext());
        input_titleKr.setHint("한국어 제목을 입력하세요.");
        input_lyrics.setInputType(InputType.TYPE_CLASS_TEXT);

        int originalInputType = input.getInputType();


        //NumberPad 세팅값 load
        SettingRepository settingRepository = new SettingRepository(requireContext());
        new Thread(() -> {
            boolean numericPadMode = settingRepository.getNumericPreference();
            if (numericPadMode){
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            else{
                input.setInputType(originalInputType);
            }

        }).start();


        TextView message = new TextView(getContext());
        message.setText("또는 '편집' 버튼을 눌러 직접 수정");
        message.setPadding(6, 30, 0, 0);
        message.setTextSize(16);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);
        input_lyrics.setLayoutParams(params);


        layout.addView(input);
        layout.addView(message);


        LinearLayout layout_directly = new LinearLayout(getContext());
        layout_directly.setOrientation(LinearLayout.VERTICAL);
        layout_directly.setPadding(50, 40, 50, 10);

        layout_directly.addView(input_lyrics);
        layout_directly.addView(input_titleKr);

        String dialogTitle;
        if (editMode){
            dialogTitle = "가사 편집";
        }
        else{
            dialogTitle = "가사 정보 등록";
        }


        AlertDialog dialog1 = new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(layout)
                // AlertDialog의 setPositiveButton 부분을 아래 코드로 완전히 교체하세요.

                .setPositiveButton("확인", (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {
                        Toast.makeText(getContext(), "VIBE 트랙 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long startFetchLyricsTime = System.currentTimeMillis();

                    String userInput = input.getText().toString().trim();
                    String trackIdNaverVibe = extractTrackId(userInput);
// 사용자 입력을 받을 첫 번째 다이얼로그
// 로딩 다이얼로그를 담을 배열 (final로 만들어 콜백에서 접근 가능하게 함)
                    final AlertDialog[] loadingDialog = new AlertDialog[1];
                    loadingDialog[0] = new AlertDialog.Builder(getContext())
                            .setTitle("전체 정보 로딩 중...")
                            .setMessage("노래 정보와 아티스트 링크를 모두 가져오고 있습니다.")
                            .setCancelable(false)
                            .create();
                    loadingDialog[0].show();

                    // --- 1단계: 노래 정보 가져오기 시작 ---
                    LyricsSearchService.fetchMetadata(webView, trackIdNaverVibe, new LyricsSearchService.MetadataCallback() {
                        @Override
                        public void onSuccess(TrackMetadata metadata) {
                            if (metadata.lyrics == null || metadata.getLyrics().trim().isEmpty()) {
                                Log.d("FavoritesFragment", "Fail to fetch lyrics");
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    if(loadingDialog[0] != null) loadingDialog[0].dismiss();
                                    Toast.makeText(getContext(), "가사 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }
                            Log.d("FavoritesFragment", "Success to fetch lyrics");
                            metadata.vibeTrackId = trackIdNaverVibe;

                            // --- 2단계: 아티스트 링크 가져오기 시작 ---
                            if (getActivity() == null) return;
                            if (metadata.artistLink != null && !metadata.artistLink.isEmpty() && metadata.vocalists != null && !metadata.vocalists.isEmpty()) {
                                getActivity().runOnUiThread(() -> {

                                    // artistLink가 전체 주소일 경우와 상대 경로일 경우를 모두 처리
                                    String finalArtistPageUrl;
                                    if (metadata.artistLink != null && metadata.artistLink.startsWith("http")) {
                                        finalArtistPageUrl = metadata.artistLink;
                                    } else {
                                        finalArtistPageUrl = "https://vibe.naver.com" + metadata.artistLink;
                                    }

                                    long startTime = System.currentTimeMillis();
                                    // [수정] 디버깅 코드를 삭제하고, 원래 로직의 주석을 해제합니다.
                                    Log.d("FavoritesFragment", "start to fetch members ids");
                                    ArtistVibeLinkService.fetchAllLinks(
                                            webView2, // 아티스트 링크 탐색에 사용할 두 번째 WebView
                                            metadata.vocalists,
                                            finalArtistPageUrl, // 방금 만든 올바른 URL 사용
                                            new ArtistVibeLinkService.ArtistLinksCallback() {
                                                @Override
                                                public void onComplete(List<List<String>> updatedVocalistList) {
                                                    long linkFetchedTime = System.currentTimeMillis();
                                                    // --- 최종 성공: 모든 정보가 준비됨 ---
                                                    if (getActivity() == null) return;
                                                    getActivity().runOnUiThread(() -> {
                                                        if (loadingDialog[0] != null)
                                                            loadingDialog[0].dismiss();
                                                        metadata.setVocalists(updatedVocalistList);

                                                        long updatedTime = System.currentTimeMillis();
                                                        Log.d("FavoritesFragment", "Lyrics: " + (startTime - startFetchLyricsTime) + "ms 소요됨");
                                                        Log.d("FavoritesFragment", "link: " + (linkFetchedTime - startTime) + "ms 소요됨");
                                                        Log.d("FavoritesFragment", "update metadata: " + (updatedTime - linkFetchedTime) + "ms 소요됨");

                                                        new AlertDialog.Builder(requireContext())
                                                                .setTitle("아래 정보를 저장하시겠습니까?")
                                                                .setMessage(metadata.toString())
                                                                .setPositiveButton("저장", (dialog2, which2) -> {
                                                                    if (metadata.title != null && metadata.title.equals(trackName)) {
                                                                        metadata.title = null;
                                                                    }
                                                                    favoritesViewModel.updateMetadata(trackIdDb, metadata, updated -> {
                                                                        if (updated > 0) {
                                                                            requireActivity().runOnUiThread(() -> {
                                                                                Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();
                                                                                loadFavoritesAndUpdateUI();
                                                                            });

                                                                        } else {
                                                                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                                        }
                                                                    });
                                                                })
                                                                .setNegativeButton("닫기", null)
                                                                .show();
                                                    });
                                                }

                                                @Override
                                                public void onError(String reason) {
                                                    // 2단계 실패 시 처리
                                                    if (getActivity() == null) return;
                                                    getActivity().runOnUiThread(() -> {
                                                        if (loadingDialog[0] != null)
                                                            loadingDialog[0].dismiss();
                                                        Toast.makeText(getContext(), "아티스트 링크 업데이트 실패: " + reason, Toast.LENGTH_SHORT).show();
                                                    });
                                                }
                                            }
                                    );
                                });
                            }
                            else{ // vocalist 정보 없는 경우
                                if (loadingDialog[0] != null)
                                    loadingDialog[0].dismiss();
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("아래 정보를 저장하시겠습니까?")
                                        .setMessage(metadata.toString())
                                        .setPositiveButton("저장", (dialog, which) -> {
                                            if (metadata.title != null && metadata.title.equals(trackName)) {
                                                metadata.title = null;
                                            }
                                            favoritesViewModel.updateMetadata(trackIdDb, metadata, updated -> {
                                                if (updated > 0) {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
                                                    loadFavoritesAndUpdateUI();
                                                } else {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                }
                                            });
                                        })
                                        .setNegativeButton("닫기", null)
                                        .show();
                            }
                        }

                        @Override
                        public void onFailure(String reason) {
                            // 1단계 실패 시 처리
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if(loadingDialog[0] != null) loadingDialog[0].dismiss();
                                if(reason.contains("JavaScript 오류")){
                                    Toast.makeText(getContext(), "JavaScript 오류: 유효하지 않은 주소입니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "ERROR: " + reason, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
// .setNeutralButton... 등 나머지 체인은 그대로 이어집니다.
                .setNeutralButton("편집", null)
                .setNegativeButton("취소", (dialog, which)  ->{})
                .create();


        dialog1.setOnShowListener(d -> {
            Button neutralBtn = dialog1.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralBtn.setOnClickListener(v -> {
                dialog1.dismiss();

                new AlertDialog.Builder(getContext())
                        .setTitle("Metadata 입력")
                        .setView(layout_directly)
                        .setPositiveButton("확인", (dialog, which) -> {
                            if (!(input_lyrics.getText().toString().isEmpty() && input_titleKr.getText().toString().isEmpty())){

                                TrackMetadata newMetadata = new TrackMetadata();
                                favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
                                            if (favorite != null) {
                                                if (!(favorite.metadata.title == null && favorite.metadata.lyrics == null && favorite.metadata.composers == null && favorite.metadata.lyricists == null)) {
                                                    Log.d("metadata is not null", favorite.metadata.title + favorite.metadata.lyrics + favorite.metadata.composers + favorite.metadata.lyricists);
                                                    newMetadata.vibeTrackId = favorite.metadata.vibeTrackId;
                                                    newMetadata.title = favorite.metadata.title;
                                                    newMetadata.lyrics = favorite.metadata.lyrics;
                                                    newMetadata.lyricists = favorite.metadata.lyricists;
                                                    newMetadata.composers = favorite.metadata.composers;
                                                }

                                                //사용자가 가사 직접 입력
                                                if (!input_lyrics.getText().toString().isEmpty()) {
                                                    newMetadata.lyrics = input_lyrics.getText().toString();
                                                }

                                                // 사용자가 노래 제목(kr) 직접 입력
                                                if(!input_titleKr.getText().toString().isEmpty()) {
                                                    newMetadata.title = input_titleKr.getText().toString();

                                                }

                                                favoritesViewModel.updateMetadata(trackIdDb, newMetadata, updated -> {
                                                    if (updated > 0) {
                                                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());

                                                    } else {
                                                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                    }
                                                });
                                            }
                                        }
                                );
                            }
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        });


        dialog1.show();


    }




    private String extractTrackId(String input) {
        if (input.startsWith("http") && input.contains("/track/")) {
            return input.substring(input.lastIndexOf("/") + 1);
        } else {
            return input;
        }
    }


    private void toggleLyricsVisibility(boolean show) {
        try {
            View metadataLayout = requireView().findViewById(R.id.metadata_layout);

            if (show) {
                Animation slideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_bottom);
                metadataLayout.startAnimation(slideIn);
                onLyricsContainer.startAnimation(slideIn);
                scrollAreaView.startAnimation(slideIn);
                Animation slideOutSlow = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_bottom_slow);
                trackRecyclerView.startAnimation(slideOutSlow);
                slideOutSlow.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        metadataLayout.setVisibility(View.VISIBLE);
                        scrollAreaView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        trackRecyclerView.setVisibility(View.GONE);
                    }
                });
            } else {
                Animation slideOut = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_bottom);
                trackRecyclerView.setVisibility(View.VISIBLE);
                metadataLayout.startAnimation(slideOut);
                onLyricsContainer.startAnimation(slideOut);
                scrollAreaView.startAnimation(slideOut);
                Animation slideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_bottom);
                Animation SlideTopIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_top);
                trackRecyclerView.startAnimation(slideIn);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //metadataLayout.setVisibility(View.GONE);
                        //scrollAreaView.setVisibility(View.GONE);
                    }
                });

                onLyricsContainer.setVisibility(View.GONE);

            }
        }catch (NullPointerException e){}
    }


    public void onItemLongClick(){
        if (!isSelectionMode){
            currentCount = (String) favoritesLoadedCountTextView.getText();
            Log.d(TAG, "current count save: " + currentCount);
            isSelectionMode = true;
            cancelSelectionModeTextView.setVisibility(View.VISIBLE);
            removeSelectedFavoritesTextView.setVisibility(View.VISIBLE);
        }


        int selectedCount = favoriteTrackAdapter.getSelectedSize();
        setFavoritesCountText(selectedCount);


    }
    public void removeSelected(Artist artist){
        if (!favoriteArtistViewModel.selectedList.contains(artist)) return;
        favoriteArtistViewModel.selectedList.remove(artist);

        int selectedCount = favoriteArtistViewModel.selectedList.size();
        setFavoritesCountText(selectedCount);
    }

    public void addSelected(Artist artist){
        if (!isSelectionMode){
            currentCount = (String) favoritesLoadedCountTextView.getText();
            Log.d(TAG, "current count save: " + currentCount);
            isSelectionMode = true;
            cancelSelectionModeTextView.setVisibility(View.VISIBLE);
            removeSelectedFavoritesTextView.setVisibility(View.VISIBLE);
        }

        if (favoriteArtistViewModel.selectedList.contains(artist)) return;
        favoriteArtistViewModel.selectedList.add(artist);

        int selectedCount = favoriteArtistViewModel.selectedList.size();
        setFavoritesCountText(selectedCount);


    }

    /**
     * 뒤로 가기로 돌아온 상태를 처리하는 메서드
     */
    private void handleReenterTransition() {
        Log.d(TAG, "handleReenterTransition() 호출됨");
        String transitionName = favoritesViewModel.getFocusedTransitionName();
        if (transitionName != null) {
            Log.d(TAG, "favorite track reenterState Exist");
            int position = favoritesViewModel.getTransitionPosition();
            if (position == -2){
                ViewCompat.setTransitionName(binding.focusedImage, transitionName);
                focusedImageView.post(() -> {
                    Log.d(TAG, "transitionName" + ViewCompat.getTransitionName(focusedImageView));
                    startPostponedEnterTransition();
                    //취소 또는 뒤로가기 버튼을 누르고 나서 null 처리
                    //reenterState = null;
                });
            }
            else {
                binding.trackRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        binding.trackRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
                        layoutManager.scrollToPositionWithOffset(favoritesViewModel.getReenterScrollPosition(), favoritesViewModel.getReenterScrollOffset());
                        trackRecyclerView.setItemAnimator(null);
                        startPostponedEnterTransition();
                        favoritesViewModel.setFocusedTransitionName(null);
                        favoritesViewModel.setTransitionPosition(0);
                        return true;
                    }
                });
            }
        } else if(favoriteArtistViewModel.reenterState != null){
            Log.d(TAG, "favorite artist reenterState Exist");
            Bundle reenterState = favoriteArtistViewModel.reenterState;
            int position = reenterState.getInt("position");
            binding.trackRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    binding.artistRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
                    layoutManager.scrollToPositionWithOffset(favoriteArtistViewModel.getReenterScrollPosition(), favoriteArtistViewModel.getReenterScrollOffset());
                    artistRecyclerView.setItemAnimator(null);
                    startPostponedEnterTransition();
                    favoriteArtistViewModel.reenterState = null;
                    return true;
                }
            });

        }else {
            Log.d(TAG, "reenterState does not Exist");
            // 뒤로 온 경우가 아니면, 연기했던 전환을 그냥 시작(취소)
            startPostponedEnterTransition();
        }
    }
    /**
     * 어댑터의 아이템 클릭을 받아 내비게이션을 실행하는 메서드
     */
    private void handleItemNavigation(Favorite favorite, ImageView sharedImageView, int position) {
        Log.d(TAG, "handleItemNavigation() 호출됨");
        String transitionName = ViewCompat.getTransitionName(sharedImageView);
        favoritesViewModel.setFocusedTransitionName(transitionName);
        favoritesViewModel.setTransitionPosition(position);
        LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            View firstItemView = layoutManager.findViewByPosition(firstVisiblePosition);
            int offset = (firstItemView != null) ? firstItemView.getTop() : 0;
            favoritesViewModel.setReenterScrollPosition(firstVisiblePosition);
            favoritesViewModel.setReenterScrollOffset(offset);
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("favorite", favorite);
        bundle.putString("transitionName", transitionName);

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();

        Navigation.findNavController(requireView()).navigate(
                R.id.action_favoritesFragment_to_musicInfoFragment,
                bundle,
                null,
                extras
        );
    }


    /**
     * 어댑터의 아이템 클릭을 받아 내비게이션을 실행하는 메서드
     */
    private void handleItemNavigationForArtist(FavoriteArtist favorite, ImageView sharedImageView, String transitionNameForm, int position) {
        Log.d(TAG, "handleItemNavigationForArtist() 호출됨");

        Bundle reenterState = new Bundle();
        reenterState.putInt("position", position);
        String transitionName = ViewCompat.getTransitionName(sharedImageView);
        Log.d(TAG, "Prepare Transition, transitionName is " + transitionName);
        reenterState.putString("transitionName", transitionName);

        LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
        if (layoutManager != null){
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            View firstItemView = layoutManager.findViewByPosition(firstVisiblePosition);
            int offset = (firstItemView != null) ? firstItemView.getTop() : 0;
            favoriteArtistViewModel.setReenterScrollPosition(firstVisiblePosition);
            favoriteArtistViewModel.setReenterScrollOffset(offset);
        }

        favoriteArtistViewModel.reenterState = reenterState;

        Bundle bundle = new Bundle();
        bundle.putParcelable("favorite_artist", favorite);
        bundle.putString("transitionName", transitionName);
        bundle.putString("transitionNameForm", transitionNameForm);
        bundle.putInt("position", position);

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();

        Navigation.findNavController(requireView()).navigate(
                R.id.action_favoritesFragment_to_artistInfoFragment,
                bundle,
                null,
                extras
        );
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isSelectionMode){
                handleCancelSelectionMode();
            }
            else if (lyricsContainer.getVisibility() == View.GONE){
                requireActivity().onBackPressed();
            }else{
                cancelLyricsMode();
            }
        }
    };

    private void handleCancelSelectionMode(){
        favoritesLoadedCountTextView.setText(currentCount);
        Log.d(TAG, "reText saved current count: " + currentCount);
        if (favoriteOption == 0){
            List<Favorite> selectedList = favoriteTrackAdapter.getSelectedList();
            favoriteTrackAdapter.setSelectionMode(false);
            for (Favorite selected : selectedList){
                selected.isSelected = false;
                selected.recyclerViewPosition = -1;
            }
            cancelSelectionModeTextView.setVisibility(View.GONE);
            removeSelectedFavoritesTextView.setVisibility(View.GONE);
            isSelectionMode = false;
            favoritesViewModel.loadAllFavorites(favorites -> {
                Context context = getContext();
                if (context != null){
                    int count = SortFilterUtil.sortAndFilterFavoritesList(getContext(), favorites, null).size();
                    setFavoritesCountText(count);
                }
            });
        }
        else{
            favoriteArtistAdapter.setSelectionMode(false);
            favoriteArtistViewModel.selectedList = new ArrayList<>();
            cancelSelectionModeTextView.setVisibility(View.GONE);
            removeSelectedFavoritesTextView.setVisibility(View.GONE);
            isSelectionMode = false;
            favoriteArtistViewModel.getFavoriteArtistsCount(count -> setFavoritesCountText(count));
        }
    }

    private void cancelLyricsMode(){
        favoritesViewModel.setLyricsMode(false);
        if (favoritesViewModel.getTransitionPosition() == -2 ){ // position : -2 means reenter to lyrics on mode from other fragments
            //reenterState 이면 recycler view 위치 재조정할 시간을 위해 연기
            postponeEnterTransition();
            favoritesViewModel.setFocusedTransitionName(null);
            favoritesViewModel.setTransitionPosition(0);
            favoritesViewModel.loadAllFavorites(favoritesList -> {
                if (!favoritesList.isEmpty())
                    trackRecyclerView.setVisibility(View.VISIBLE);
                else
                    trackRecyclerView.setVisibility(View.GONE);
                Collections.reverse(favoritesList);
                updateEmptyState(favoritesList.isEmpty());
                favoriteTrackAdapter.updateData(favoritesList);

                setFavoritesCountText(favoritesList.size());
                binding.trackRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        binding.trackRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.trackRecyclerView.getLayoutManager();
                        if(layoutManager != null) {
                            layoutManager.scrollToPositionWithOffset(favoritesViewModel.getReenterScrollPosition(), favoritesViewModel.getReenterScrollOffset());
                            Log.d(TAG, "recyclerView 위치 복원, position: " + favoritesViewModel.getReenterScrollPosition() + " ,offset: " + favoritesViewModel.getReenterScrollOffset());
                        }
                        startPostponedEnterTransition();
                        return true;
                    }
                });
            });

        }
        lyricsModeCancelButton.setVisibility(View.GONE);
        favoriteOptionSwitch.setVisibility(View.VISIBLE);
        elementCountTextView.setVisibility(View.VISIBLE);
        countLayout.setVisibility(View.VISIBLE);
        toggleLyricsVisibility(false);
    }

    private void setFavoritesCountText(int count){
        String option = "";
        if (favoriteOption == 0){
            option = "Songs";
        } else{
            option = "Artists";
        }

        if (count == 0){
            favoritesLoadedCountTextView.setText("No");
            elementCountTextView.setText(option);
        } else if (count == 1){
            favoritesLoadedCountTextView.setText("1");
            elementCountTextView.setText(option.substring(0, option.length() - 1));
        } else {
            favoritesLoadedCountTextView.setText(String.valueOf(count));
            elementCountTextView.setText(option);
        }

    }



}

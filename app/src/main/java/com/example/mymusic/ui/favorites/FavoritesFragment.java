

package com.example.mymusic.ui.favorites;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
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
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mymusic.MainActivityViewModel;
import com.example.mymusic.R;
import com.example.mymusic.adapter.FavoriteArtistAdapter;
import com.example.mymusic.adapter.FavoritesAdapter;
import com.example.mymusic.adapter.FavoritesWithCardViewAdapter;
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter;
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
import com.example.mymusic.simpleArtistInfo.SimpleArtistDialogHelper;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;
import com.example.mymusic.ui.favorites.bottomSheet.ArtistFilterBottomSheetFragment;
import com.example.mymusic.ui.favorites.bottomSheet.FilterBottomSheetFragment;
import com.example.mymusic.ui.musicInfo.MusicInfoFragment;
import com.example.mymusic.ui.webView.artistMetadata.ArtistMetadataWebView;
import com.example.mymusic.ui.webView.lyricsSearch.LyricsSearchAutoFragment;
import com.example.mymusic.util.DarkModeUtils;
import com.example.mymusic.util.FavoritesSearchUtils;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;
import com.example.mymusic.util.SortFilterArtistUtil;
import com.example.mymusic.util.SortFilterUtil;
import com.example.mymusic.util.VerticalSpaceItemDecoration;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.widget.LinearLayout;


import jp.wasabeef.recyclerview.animators.LandingAnimator;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;


@UnstableApi
public class FavoritesFragment extends Fragment {
    public static int FAVORITE_ARTIST_REPRESENTATIVE_ARTWORK_SIZE = 160;
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
    private ArtistFilterBottomSheetFragment bottomSheetArtist;
    private FrameLayout lyricsFrameLayout;

    TextView lyricsTextView;
    ScrollView scrollAreaView;
    LinearLayout countLayout, onLyricsContainer;
   // ImageButton lyricsModeCancelButton;
    ImageView focusedImageView;
    TextView focusedTitleTextView, focusedAlbumTextView, focusedArtistTextView, focusedDurationTextView, focusedReleaseDateTextView;
    MaterialCardView simpleMusicInfoContainer, lyricsTextContainer, lyricsContainer;
    TextView cancelSelectionModeTextView, removeSelectedFavoritesTextView;
    SwitchCompat favoriteOptionSwitch;
    private FragmentFavoritesBinding binding;
    private ImageButton filterImageButton, dropDownImageButton, dropUpImageButton;
    private String currentCount = "";
    private RecyclerView artistsOtherMusicRecyclerView;
    private MaterialCardView artistsOtherMusicCardView;
    private Context viewGroupContext;
    private SimpleArtistDialogHelper artistDialogHelper;
    private EditText searchKeywordEditText;
    private ImageButton previousKeywordButton, nextKeywordButton;
    private TextView keywordSearchedCountTextView;
    private MainActivityViewModel mainActivityViewModel;

    @Override
    public void onResume() {
        super.onResume();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation != favoritesViewModel.getScreenOrientation()){
            removeRecyclerViewAnimation();
            Log.d(TAG, "orientation changed");

        }
        favoritesViewModel.setScreenOrientation(orientation);
        if (favoriteArtistViewModel.isOnSimpleDialog()){
            int lastPosition = favoriteArtistViewModel.getSimpleArtistDialogPosition();
            List<FavoriteArtist> favoriteArtistList = favoriteArtistViewModel.getFavoriteArtistList();
            GradientDrawable lastGradient = favoriteArtistViewModel.getLastGradient();
            safeShowDialog(getActivity(), lastPosition, favoriteArtistViewModel.getDetailVisibleStateOnDialog(), favoriteArtistList, lastGradient);
        }
    }


    private void safeShowDialog(Activity activity, int lastPosition, int detailsVisibleState, List<FavoriteArtist> favoriteArtistList , GradientDrawable lastGradient) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable tryShow = new Runnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    if (detailsVisibleState == SimpleArtistDialogHelper.OFF_DETAILS) {
                        artistDialogHelper.showArtistDialog(lastPosition, favoriteArtistList, lastGradient);
                    } else{
                        artistDialogHelper.showArtistDialogWithExpand(lastPosition, favoriteArtistList, lastGradient);
                    }
                } else if (attempts < 5) {
                    attempts++;
                    handler.postDelayed(this, 100); // 100ms 간격으로 최대 5번 재시도
                } else {
                    Log.w(TAG, "Activity not ready. Dialog not shown.");
                }
            }
        };

        handler.post(tryShow);
    }

    public void onIconDoubleTapped() {
        if (Boolean.TRUE.equals(favoritesViewModel.getLyricsMode().getValue())) {
            cancelLyricsMode();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoritesViewModel = new ViewModelProvider(requireActivity()).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(requireActivity()).get(FavoriteArtistViewModel.class);
        mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);

        getParentFragmentManager().setFragmentResultListener(MusicInfoFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (requestKey.equals(MusicInfoFragment.REQUEST_KEY)){
                boolean transitionEnded = bundle.getBoolean(MusicInfoFragment.BUNDLE_KEY_TRANSITION_END);
                if (transitionEnded){
                    Log.d("AnimationDebug", "transition ended");
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

        //Auto Lyrics Search
        getParentFragmentManager().setFragmentResultListener(LyricsSearchAutoFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (requestKey.equals(LyricsSearchAutoFragment.REQUEST_KEY)){
                TrackMetadata metadata = bundle.getParcelable(LyricsSearchAutoFragment.BUNDLE_KEY);
                String trackId = favoritesViewModel.getLyricsSearchTrackId();
                favoritesViewModel.updateMetadata(trackId, metadata, result -> {
                    if (result > 0) new Handler(Looper.getMainLooper()).post(() -> {
                        int position = favoritesViewModel.getScrollPosition();
                        onLyricsClick(trackId, null, null, null, position);
                    });
                    else new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), "가사 저장 실패, 다시 시도해주세요.", Toast.LENGTH_SHORT).show());
                    //int position = favoritesViewModel.getFocusedPosition();
                    if (favoriteTrackAdapter != null) {
                        favoritesViewModel.loadAllFavorites(favorites -> {
                            List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(getContext(), favorites);
                            new Handler(Looper.getMainLooper()).post(() -> favoriteTrackAdapter.updateData(filtered));
                        });
                    }
                });
            }
        });

        getParentFragmentManager().setFragmentResultListener(ArtistMetadataWebView.REQUEST_KEY, this, (requestKey, bundle) -> {
            Log.d(TAG + "Debug", "bundle received");
            if (requestKey.equals(ArtistMetadataWebView.REQUEST_KEY)){
                ArtistMetadata metadata = bundle.getParcelable(ArtistMetadataWebView.BUNDLE_KEY);
                favoriteArtistViewModel.addArtistMetadata(metadata, new FavoriteArtistViewModel.addMetadataCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG + "Debug", "add artist metadata success");
                        artistRecyclerView.post(() -> setArtistRecyclerViewAnimation());
                        loadFavoritesAndUpdateUI();
                    }

                    @Override
                    public void onFailure(ArtistMetadata metadata, String reason) {
                        Log.d(TAG + "Debug", "add artist metadata Fail: " + reason);
                        Log.d(TAG + "Debug", "metadata: " + metadata.toString());
                    }
                });
            }
        });



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        viewGroupContext = container.getContext();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        Log.d(TAG, "onViewCreated() 호출됨");
        super.onViewCreated(view, savedInstanceState);

        //가사 보이는 상태에서 뒤로가기 제어
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        postponeEnterTransition();

        artistDialogHelper = new SimpleArtistDialogHelper(getContext(), new ArrayList<>());
        artistDialogHelper.setPositionChangedListener(new SimpleArtistDialogHelper.OnPositionChangedListener() {
            @Override
            public void positionChanged(int position, int detail_visible_state, GradientDrawable currentGradient) {
                favoriteArtistViewModel.setSimpleArtistDialogPosition(position);
                favoriteArtistViewModel.setDetailVisibleStateOnDialog(detail_visible_state);
                favoriteArtistViewModel.setLastGradient(currentGradient);
            }

        });
        artistDialogHelper.setDismissListener(new SimpleArtistDialogHelper.OnDialogDismissListener() {
            @Override
            public void dialogDismissed() {
                favoriteArtistViewModel.setOnSimpleDialog(false);
            }
        });

        //No adapter attached 오류 해결을 위해 먼저 빈 리스트 전달하고 나중에 데이터 받고 나서 업데이트해주기
        favoriteTrackAdapter = new FavoritesAdapter(
                new ArrayList<>(), // ⬅️ 빈 리스트 전달
                this::showCustomPopup,
                this::onLyricsClick,
                this::onLyricLongClick,
                this::onItemLongClick,
                this::handleItemNavigation,
                this::trackPlay
        );

        favoriteArtistAdapter = new FavoriteArtistAdapter(
                new ArrayList<>(),
                this::deleteFavoriteArtist,
                this::addArtistMetadataAuto,
                this::addArtistMetadata,
                favoriteArtistViewModel,
                this::handleItemNavigationForArtist,
                this::addSelected,
                this::removeSelected,
                this::artistDetailButtonClick);

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



        emptyFavoriteSongTextView = view.findViewById(R.id.empty_favorite_song);
        emptyFavoriteArtistTextView = view.findViewById(R.id.empty_favorite_artist);
        favoritesLoadedCountTextView = view.findViewById(R.id.favorites_loaded_count);
        webView = view.findViewById(R.id.hidden_web_view);
        webView2 = view.findViewById(R.id.hidden_web_view_2);
        lyricsContainer = view.findViewById(R.id.lyrics_container);
      //  lyricsModeCancelButton = view.findViewById(R.id.cancel_button);
        countLayout = view.findViewById(R.id.count_layout);
        onLyricsContainer = view.findViewById(R.id.on_lyrics_container);
        onLyricsContainer.setVisibility(View.GONE);
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
        bottomSheetArtist = new ArtistFilterBottomSheetFragment();
        filterImageButton = view.findViewById(R.id.filter_button);
        dropDownImageButton = view.findViewById(R.id.in_order_button_drop_down);
        dropUpImageButton = view.findViewById(R.id.in_order_button_drop_up);
        searchKeywordEditText = view.findViewById(R.id.search_keyword);
        previousKeywordButton = view.findViewById(R.id.previous_keyword);
        nextKeywordButton = view.findViewById(R.id.next_keyword);
        keywordSearchedCountTextView = view.findViewById(R.id.keyword_searched_count);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            artistsOtherMusicRecyclerView = view.findViewById(R.id.artists_other_music_recycler);
            artistsOtherMusicCardView = view.findViewById(R.id.artists_other_music_card_view);
        }


        //switch toggle event
        favoriteOptionSwitch = view.findViewById(R.id.favorite_option_switch);


        trackRecyclerView.setAdapter(favoriteTrackAdapter);
        artistRecyclerView.setAdapter(favoriteArtistAdapter);

        lyricsFrameLayout = view.findViewById(R.id.lyrics_frame);


        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
            if (favoriteOption == 1){
                prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
                updateArtistSortOption(prefs);
            }
            boolean isDescending = prefs.getBoolean("isDescending", false);
            if (isDescending) {
                dropUpImageButton.setVisibility(View.INVISIBLE);
                dropDownImageButton.setVisibility(View.VISIBLE);
            } else {
                dropDownImageButton.setVisibility(View.GONE);
                dropUpImageButton.setVisibility(View.VISIBLE);
            }
        }


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

        favoriteOptionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSelectionMode){
                handleCancelSelectionMode();
            }
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

        /*
        lyricsModeCancelButton.setOnClickListener(v -> {
            cancelLyricsMode();
        });

         */


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
                    if (mainActivityViewModel.getCurrentTrack().getValue() != null && mainActivityViewModel.getCurrentTrack().getValue().track.trackId.equals(item.track.trackId)) {
                        Dialog dialog = new Dialog(getContext());
                        dialog.setContentView(R.layout.dialog_custom_only_dismiss_button);
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                        dialog.setCancelable(true);
                        TextView dismissButton = dialog.findViewById(R.id.dismiss_button);
                        TextView titleTextView = dialog.findViewById(R.id.title);
                        TextView subTextView = dialog.findViewById(R.id.subtext);
                        titleTextView.setText("Error");
                        subTextView.setText("재생중인 노래(" + item.getTitle() + ") 를 삭제할 수 없습니다.");
                        dismissButton.setOnClickListener(v2 -> dialog.dismiss());
                        dialog.show();
                        return;
                    }

                    if (item.metadata != null && item.metadata.title != null && !item.metadata.title.isEmpty()) {
                        selected.append(item.metadata.title + " - " + item.track.artistName + "\n");
                    } else {
                        selected.append(item.track.trackName + " - " + item.track.artistName + "\n");
                    }
                }
                String mentByCount = selectedList.size() == 1 ? "을(를) 삭제하시겠습니까?" : selectedList.size() + "개의 노래를 모두 삭제하시겠습니까?";

                Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.dialog_custom);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.setCancelable(true);

                TextView cancelButton = dialog.findViewById(R.id.cancel_button);
                TextView confirmButton = dialog.findViewById(R.id.confirm_button);
                TextView titleTextView = dialog.findViewById(R.id.title);
                TextView subTextView = dialog.findViewById(R.id.subtext);

                titleTextView.setText("삭제");
                subTextView.setText(selected + "\n" + mentByCount);

                cancelButton.setOnClickListener(v2 -> dialog.dismiss());
                confirmButton.setOnClickListener(v2 -> {
                    dialog.dismiss();
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
                                    favoritesViewModel.setFavoriteList(filtered);
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
                });

                dialog.show();

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

                Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.dialog_custom);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.setCancelable(true);

                TextView cancelButton = dialog.findViewById(R.id.cancel_button);
                TextView confirmButton = dialog.findViewById(R.id.confirm_button);
                TextView titleTextView = dialog.findViewById(R.id.title);
                TextView subTextView = dialog.findViewById(R.id.subtext);

                titleTextView.setText("삭제");
                subTextView.setText(selectedStr + "\n" + mentByCount);

                cancelButton.setOnClickListener(v2 -> dialog.dismiss());
                confirmButton.setOnClickListener(v2 -> {
                    dialog.dismiss();
                    List<String> selectedIds = new ArrayList<>();
                    List<String> selectedImageUrls = new ArrayList<>();
                    for (Artist artist : selectedList) {
                        selectedIds.add(artist.artistId);
                        selectedImageUrls.add(artist.artworkUrl);
                        favoriteArtistViewModel.deleteArtistMetadataBySpotifyId(artist.artistId , message -> {
                            if (message.contains("Success")){
                                Log.d(TAG, "delete metadata Success");
                            }
                            else{
                                Log.d(TAG, "delete metadata Fail");
                            }
                        });
                    }
                    favoriteArtistViewModel.deleteFavoritesArtistByIds(selectedIds, result -> {
                        if (result > 0) {
                            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), result + "팀의 아티스트가 삭제되었습니다.", Toast.LENGTH_SHORT));
                            favoriteArtistViewModel.loadFavoritesOriginalForm(favorites -> {
                                List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(getContext(), favorites);

                                favoriteArtistAdapter.updateData(filtered);
                                //count 업데이트
                                setFavoritesCountText(filtered.size());

                                artistDialogHelper.updateList(filtered);

                            });
                            CustomFavoriteArtistImageWriter.removeUrls(viewGroupContext, selectedImageUrls);
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
                });

                dialog.show();
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

            NavHostFragment.findNavController(this).navigate(R.id.musicInfoFragment, bundle, null, extras);

            /*
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_favoritesFragment_to_musicInfoFragment,
                    bundle,
                    null,
                    extras
            );

             */
        });


        //is lyrics mode인 경우 (다른 fragment로 갔다가 돌아올 때)
        Favorite focused = favoritesViewModel.getFocusedTrack().getValue();
        Boolean isLyrics = favoritesViewModel.getLyricsMode().getValue();
        if (isLyrics != null && isLyrics && focused != null){
            Log.d(TAG, "lyrics on mode");
            LyricsOnMode(focused, favoritesViewModel.getFocusedPosition(), false);
            loadFavoritesAndUpdateUI();
            if (favoritesViewModel.getFocusedTransitionName() != null){
                ViewCompat.setTransitionName(focusedImageView, favoritesViewModel.getFocusedTransitionName());
            }
        }


        bottomSheet.setApplyListener(new FilterBottomSheetFragment.OnApplyListener() {
            @Override
            public void onApply() { loadFavoritesAndUpdateUI(); }
        });

        bottomSheetArtist.setApplyListener(new ArtistFilterBottomSheetFragment.OnApplyListener() {
            @Override
            public void onApply() {
                loadFavoritesAndUpdateUI();
            }
        });




        filterImageButton.setOnClickListener(v -> {
            if (favoriteOption == 0) {
                if (!bottomSheet.isAdded() && !bottomSheet.isVisible()) {
                    bottomSheet.show(getParentFragmentManager(), "FilterBottomSheet");
                }
            }
            else if (favoriteOption == 1){
                if (!bottomSheetArtist.isAdded() && !bottomSheetArtist.isVisible()) {
                    bottomSheetArtist.show(getParentFragmentManager(), "ArtistFilterBottomSheet");
                }
            }
        });


        dropDownImageButton.setOnClickListener(v -> {
            dropDownImageButton.setVisibility(View.GONE);
            dropUpImageButton.setVisibility(View.VISIBLE);
            if (context != null){
                SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
                if (favoriteOption == 1){
                    prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
                }
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
            dropUpImageButton.setVisibility(View.INVISIBLE);
            dropDownImageButton.setVisibility(View.VISIBLE);
            if (context != null){
                if (favoriteOption == 0) {
                    SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isDescending", true);
                    editor.apply();
                }else{
                    SharedPreferences prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isDescending", true);
                    editor.apply();
                }
                Log.d(TAG, "isDescending true preference stored");
                loadFavoritesAndUpdateUI();
            }else{
                Log.d(TAG, "fail to getContext(), can not update list");
            }
        });


        searchKeywordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (favoriteOption == 0){
                    favoritesViewModel.setKeyword(s.toString());
                    updateHighlightedPositionList();
                }

                else{
                    favoriteArtistViewModel.setKeyword(s.toString());
                    updateHighlightedPositionList();
                }

            }
        });

        searchKeywordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null){
                        imm.hideSoftInputFromWindow(searchKeywordEditText.getWindowToken(), 0);
                    }
                    searchKeywordEditText.clearFocus();

                    String currentKeyword = searchKeywordEditText.getText().toString();
                    if (favoriteOption == 0){
                        String keywordSavedInViewModel = favoritesViewModel.getKeyword();
                        if (keywordSavedInViewModel == null || keywordSavedInViewModel.isEmpty() || keywordSavedInViewModel.equals(currentKeyword)) return true;
                        favoritesViewModel.setKeyword(currentKeyword);
                        updateHighlightedPositionList();
                    }else if (favoriteOption == 1) {
                        String keywordSavedInViewModel = favoriteArtistViewModel.getKeyword();
                        if (keywordSavedInViewModel == null || keywordSavedInViewModel.isEmpty() || keywordSavedInViewModel.equals(currentKeyword)) return true;
                        favoriteArtistViewModel.setKeyword(currentKeyword);
                        updateHighlightedPositionList();
                    }
                    return true;
                }
                return false;
            }
        });

        previousKeywordButton.setOnClickListener(v -> {
            if (keywordSearchedCountTextView.getVisibility() == View.VISIBLE) {
                scrollToPreviousSearched();
            }
            else{
                if (favoriteOption == 0){
                    trackRecyclerView.smoothScrollToPosition(0);
                } else{
                    artistRecyclerView.smoothScrollToPosition(0);
                }
            }
        });

        nextKeywordButton.setOnClickListener(v -> {
            if (keywordSearchedCountTextView.getVisibility() == View.VISIBLE) {
                scrollToNextSearched();
            }
            else{
                if (favoriteOption == 0){
                    trackRecyclerView.smoothScrollToPosition(favoritesViewModel.getFavoriteList().size() - 1);
                } else{
                    artistRecyclerView.smoothScrollToPosition(favoriteArtistViewModel.getFavoriteArtistList().size() - 1);
                }
            }
        });


//onViewCreated
    }

    private void showCustomPopup(Favorite fav, View anchorView, int position) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_preview_menu, null);
        int popupWidth = 360;
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(16f);
        popupWindow.setFocusable(false);


        // **뷰가 완전히 그려진 후 좌표 계산**
        anchorView.post(() -> {
            // 팝업 크기 먼저 측정
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = popupView.getMeasuredHeight();
            Log.d(TAG, "width: " + popupWidth + " height: " + popupHeight);

            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);
            int anchorX = location[0];
            int anchorY = location[1];
            Log.d(TAG, "anchor x: " + anchorX + " anchor Y: " + anchorY);

            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            int x = screenWidth - (7 * anchorX + popupWidth);
            Log.d(TAG, "screen width: " + screenWidth + " calculated x: " + x);
            if (x < 0) x = 0;
            if (x + popupWidth > screenWidth) x = screenWidth - popupWidth;
            int y = anchorY + (anchorView.getHeight() / 2) - (popupHeight / 2);

            // **rootView를 넣어야 좌표계가 전체화면 기준**
            popupWindow.showAtLocation(anchorView.getRootView(), Gravity.NO_GRAVITY, x, y);
        });

        //bind
        TextView deleteButton = popupView.findViewById(R.id.delete_button);
        addMp3Button = popupView.findViewById(R.id.add_mp3_button);
        editMp3Button = popupView.findViewById(R.id.edit_mp3_button);
        if (fav.audioUri != null) {
            editMp3Button.setVisibility(View.VISIBLE);
            addMp3Button.setVisibility(View.GONE);
        }

        TextView editButton = popupView.findViewById(R.id.edit_button);
        deleteButton.setOnClickListener(v -> {
            deleteFavoriteSong(fav);
            popupWindow.dismiss();
        });

        addMp3Button.setOnClickListener(v -> {
            popupWindow.dismiss();
            addMp3File(fav, position);
        });

        editMp3Button.setOnClickListener(v -> {
            popupWindow.dismiss();
            addMp3File(fav, position);
        });


    }
    LinearLayout addMp3Button;
    LinearLayout editMp3Button;


    private static final int REQUEST_CODE_PICK_AUDIO = 1001;

    private void addMp3File(Favorite fav, int position) {
        favoritesViewModel.selectedFavoriteForMp3 = fav;
        pickAudioFile();
    }


    private void pickAudioFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/mpeg"); // MP3만
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();

            // 퍼미션 영구 유지
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );



            // 복사 없이 원본 URI 그대로 저장
            favoritesViewModel.selectedFavoriteForMp3.audioUri = uri.toString();

            favoritesViewModel.updateFavoriteSong(favoritesViewModel.selectedFavoriteForMp3, new FavoritesViewModel.OnFavoriteViewModelCallback() {
                @Override
                public void onSuccess() {
                    new Handler(Looper.getMainLooper()).post(() ->
                            {
                                List<Favorite> list = new ArrayList<>();
                                list.add(favoritesViewModel.selectedFavoriteForMp3);
                                FavoritesFragment.this.mainActivityViewModel.setPlaylist(list, 0);
                            }
                    );
                    loadFavoritesAndUpdateUI();
                }

                @Override
                public void onFailure() {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(requireContext(), "mp3 불러오기 실패", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }


    private File copyAudioToAppStorage(Uri uri) throws IOException {
        // 내부 저장소 경로 (앱 전용)
        Activity activity = requireActivity();
        File targetFile = new File( activity.getFilesDir(), "track_" + System.currentTimeMillis() + ".mp3");

        try (InputStream in = activity.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return targetFile;  // 복사한 파일 반환
    }





    private void updateHighlightedPositionList(){
        if (favoriteOption == 0){

            String keyword = favoritesViewModel.getKeyword();
            favoriteTrackAdapter.setKeyword(keyword);
            favoritesViewModel.clearHighlightedPositions();
            List<Integer> highlightedPositions = FavoritesSearchUtils.getContainPositions(keyword, favoritesViewModel.getFavoriteList(), favorite -> {
                if (favorite.metadata != null && favorite.metadata.title != null)
                    return favorite.metadata.title;
                else if (favorite.track != null && favorite.track.trackName != null)
                    return favorite.track.trackName;
                return null;
            });



            int visibility = Boolean.TRUE.equals(favoritesViewModel.getLyricsMode().getValue()) ? View.GONE : View.VISIBLE;


            if (highlightedPositions != null && !highlightedPositions.isEmpty()){
                previousKeywordButton.setVisibility(visibility);
                nextKeywordButton.setVisibility(visibility);
                keywordSearchedCountTextView.setVisibility(visibility);
                favoritesViewModel.setFocusedHighlightedPosition(highlightedPositions.get(0));
                String count = "1/" + highlightedPositions.size();
                keywordSearchedCountTextView.setText(count);
                trackRecyclerView.smoothScrollToPosition(highlightedPositions.get(0));
            } else{
                keywordSearchedCountTextView.setText("");
                keywordSearchedCountTextView.setVisibility(View.INVISIBLE);
            }

            favoritesViewModel.setHighlightedPositions(highlightedPositions);


            Log.d(TAG, "highlighted positions: " + favoritesViewModel.getHighlightedPositions());
        }

        else {
            String keyword = favoriteArtistViewModel.getKeyword();
            favoriteArtistAdapter.setKeyword(keyword);
            favoriteArtistViewModel.clearHighlightedPositions();

            List<Integer> highlightedPositions = FavoritesSearchUtils.getContainPositions(keyword, favoriteArtistViewModel.getFavoriteArtistList(), favorite -> {
                if (favorite.metadata != null && favorite.metadata.artistNameKr != null)
                    return favorite.metadata.artistNameKr;
                else if (favorite.artist != null && favorite.artist.artistName != null)
                    return favorite.artist.artistName;
                return null;
            });

            if (highlightedPositions != null && !highlightedPositions.isEmpty()){
                previousKeywordButton.setVisibility(View.VISIBLE);
                nextKeywordButton.setVisibility(View.VISIBLE);
                keywordSearchedCountTextView.setVisibility(View.VISIBLE);
                favoriteArtistViewModel.setFocusedHighlightedPosition(highlightedPositions.get(0));
                String count = "1/" + highlightedPositions.size();
                keywordSearchedCountTextView.setText(count);
                artistRecyclerView.smoothScrollToPosition(highlightedPositions.get(0));
            } else{
                keywordSearchedCountTextView.setText("");
                keywordSearchedCountTextView.setVisibility(View.INVISIBLE);
            }

            favoriteArtistViewModel.setHighlightedPositions(highlightedPositions);
            Log.d(TAG, "highlighted positions: " + favoriteArtistViewModel.getHighlightedPositions());
        }

    }

    private void scrollToPreviousSearched(){
        if (favoriteOption == 0) {
            List<Integer> highlightedPositions = favoritesViewModel.getHighlightedPositions();
            if (highlightedPositions != null && !highlightedPositions.isEmpty()) {
                int currentPosition = favoritesViewModel.getFocusedHighlightedPosition();
                int indexOfCurrentPosition = highlightedPositions.indexOf(currentPosition);
                int index = (indexOfCurrentPosition + highlightedPositions.size() - 1) % highlightedPositions.size();
                int previousPosition = highlightedPositions.get(index);
                favoritesViewModel.setFocusedHighlightedPosition(previousPosition);

                LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(previousPosition, 0);
                } else{
                    trackRecyclerView.smoothScrollToPosition(previousPosition);
                }
                String count = (index + 1) + "/" + highlightedPositions.size();
                keywordSearchedCountTextView.setText(count);
            }
        }

        else{
            List<Integer> highlightedPositions = favoriteArtistViewModel.getHighlightedPositions();
            if (highlightedPositions != null && !highlightedPositions.isEmpty()) {
                int currentPosition = favoriteArtistViewModel.getFocusedHighlightedPosition();
                int indexOfCurrentPosition = highlightedPositions.indexOf(currentPosition);
                int index = (indexOfCurrentPosition + highlightedPositions.size() - 1) % highlightedPositions.size();
                int previousPosition = highlightedPositions.get(index);
                favoriteArtistViewModel.setFocusedHighlightedPosition(previousPosition);

                LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(previousPosition, 0);
                } else{
                    artistRecyclerView.smoothScrollToPosition(previousPosition);
                }
                String count = (index + 1) + "/" + highlightedPositions.size();
                keywordSearchedCountTextView.setText(count);
            }
        }
    }
    private void scrollToNextSearched(){
        if (favoriteOption == 0) {
            List<Integer> highlightedPositions = favoritesViewModel.getHighlightedPositions();
            if (highlightedPositions != null && !highlightedPositions.isEmpty()) {
                int currentPosition = favoritesViewModel.getFocusedHighlightedPosition();
                int indexOfCurrentPosition = highlightedPositions.indexOf(currentPosition);
                int index = (indexOfCurrentPosition + highlightedPositions.size() + 1) % highlightedPositions.size();
                int nextPosition = highlightedPositions.get(index);
                favoritesViewModel.setFocusedHighlightedPosition(nextPosition);

                LinearLayoutManager layoutManager = (LinearLayoutManager) trackRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(nextPosition, 0);
                } else{
                    trackRecyclerView.smoothScrollToPosition(nextPosition);
                }

                String count = (index + 1) + "/" + highlightedPositions.size();
                keywordSearchedCountTextView.setText(count);
            }
        }

        else{
            List<Integer> highlightedPositions = favoriteArtistViewModel.getHighlightedPositions();
            if (highlightedPositions != null && !highlightedPositions.isEmpty()) {
                int currentPosition = favoriteArtistViewModel.getFocusedHighlightedPosition();
                int indexOfCurrentPosition = highlightedPositions.indexOf(currentPosition);
                int index = (indexOfCurrentPosition + highlightedPositions.size() + 1) % highlightedPositions.size();
                int nextPosition = highlightedPositions.get(index);
                favoriteArtistViewModel.setFocusedHighlightedPosition(nextPosition);

                LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(nextPosition, 0);
                } else{
                    artistRecyclerView.smoothScrollToPosition(nextPosition);
                }

                String count = (index + 1) + "/" + highlightedPositions.size();
                keywordSearchedCountTextView.setText(count);
            }
        }
    }

    private void updateArtistSortOption(SharedPreferences prefs){
        String sortOpt = prefs.getString("sort_option", "ADDED_DATE");
        favoriteArtistViewModel.setSortOption(sortOpt);
    }

    private void setArtistRecyclerViewAnimation(){
        LandingAnimator artistAnimator = new LandingAnimator();
        artistAnimator.setRemoveDuration(200); // 제거 애니메이션 시간
        artistAnimator.setAddDuration(200); // 추가 애니메이션 시간
        if (artistRecyclerView != null)
            artistRecyclerView.setItemAnimator(artistAnimator);
    }

    private void setRecyclerViewAnimation() {
        //SlideInUpAnimator animator = new SlideInUpAnimator();
        //SlideInLeftAnimator animator = new SlideInLeftAnimator();
        LandingAnimator animator = new LandingAnimator();
        //OvershootInLeftAnimator animator = new OvershootInLeftAnimator();
        animator.setRemoveDuration(200); // 제거 애니메이션 시간
        animator.setAddDuration(200); // 추가 애니메이션 시간
        if (trackRecyclerView != null)
            trackRecyclerView.setItemAnimator(animator);
    }

    private void removeRecyclerViewAnimation(){
        Log.d(TAG, "remove animation");
        if (favoriteOption == 0) {
            trackRecyclerView.setLayoutAnimation(null);
            trackRecyclerView.postDelayed(this::setRecyclerViewAnimation, 200);
        }
        else{
            artistRecyclerView.setLayoutAnimation(null);
            artistRecyclerView.postDelayed(this::setArtistRecyclerViewAnimation, 200);
        }

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
                    favoritesViewModel.setFavoriteList(filtered);
                    favoriteTrackAdapter.updateData(filtered);
                    updateHighlightedPositionList();

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
                    if (favoritesViewModel.getScrollPosition() > 7 ){
                        trackRecyclerView.setItemAnimator(null);
                        trackRecyclerView.post(this::setRecyclerViewAnimation);
                    }
                }


                handleReenterTransition();
            });
        } else {  //Favorite Artist
            SharedPreferences prefs = getContext().getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
            int oldSortOpt = favoriteArtistViewModel.getSortOptionTypeInt();
            updateArtistSortOption(prefs);
            int newSortOpt = favoriteArtistViewModel.getSortOptionTypeInt();
            Log.d(TAG, "old sort option: " +oldSortOpt + " new sort opt: " + newSortOpt);

            if (oldSortOpt == -1){
                oldSortOpt = newSortOpt;
            }


            trackRecyclerView.setVisibility(View.GONE);
            int finalOldSortOpt = oldSortOpt;
            favoriteArtistViewModel.loadFavoritesOriginalForm(favoriteArtistList -> {
                if (favoriteArtistList != null) {
                    if (!favoriteArtistList.isEmpty())
                        artistRecyclerView.setVisibility(View.VISIBLE);
                    else {
                        Log.d(TAG, "Artist List is empty");
                        artistRecyclerView.setVisibility(View.GONE);
                    }
                    List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(getContext(), favoriteArtistList);

                    artistDialogHelper.updateList(filtered);
                    favoriteArtistViewModel.setFavoriteArtistList(filtered);
                    updateHighlightedPositionList();

                    updateEmptyState(filtered.isEmpty());
                    favoriteArtistAdapter.updateData(filtered, finalOldSortOpt, newSortOpt);

                    //Cache save when enter to FavoritesFragment (Favorite option : 1)
                    artistRecyclerView.post(() -> {
                        CustomFavoriteArtistImageWriter.saveRepresentativeImagesByFavoriteArtistList(viewGroupContext, filtered, ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE);
                    });


                    LinearLayoutManager layoutManager = (LinearLayoutManager) artistRecyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPositionWithOffset(favoriteArtistViewModel.getScrollPosition(), favoriteArtistViewModel.getScrollOffset());
                        Log.d(TAG, "loadFavoritesAndUpdateUI called, set scrollPositoin: "
                                + favoriteArtistViewModel.getScrollPosition() + " , scrollOffset: " + favoriteArtistViewModel.getScrollOffset());
                        if (favoriteArtistViewModel.getScrollPosition() > 7){
                            artistRecyclerView.setItemAnimator(null);
                            artistRecyclerView.post(this::setArtistRecyclerViewAnimation);
                        }
                    }

                    setFavoritesCountText(filtered.size());
                    handleReenterTransition();
                }else{
                    Log.d(TAG, "Artist List is empty");
                    artistRecyclerView.setVisibility(View.GONE);
                    updateEmptyState(true);
                    setFavoritesCountText(0);
                }
            });
        }

    }



    public void artistDetailButtonClick(int position){
        artistDialogHelper.showArtistDialogFirstTime(position);
        favoriteArtistViewModel.setOnSimpleDialog(true);
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

        if (mainActivityViewModel.getCurrentTrack().getValue() != null && mainActivityViewModel.getCurrentTrack().getValue().track.trackId.equals(favorite.track.trackId)) {
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_custom_only_dismiss_button);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);
            TextView dismissButton = dialog.findViewById(R.id.dismiss_button);
            TextView titleTextView = dialog.findViewById(R.id.title);
            TextView subTextView = dialog.findViewById(R.id.subtext);
            titleTextView.setText("Error");
            subTextView.setText("재생중인 노래를 삭제할 수 없습니다.");
            dismissButton.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
            return;
        }

        Track track = favorite.track;
        String trackName = track.trackName;
        if (favorite.metadata != null && favorite.metadata.title != null){
            trackName = favorite.metadata.title;
        }

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_custom);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        TextView cancelButton = dialog.findViewById(R.id.cancel_button);
        TextView confirmButton = dialog.findViewById(R.id.confirm_button);
        TextView titleTextView = dialog.findViewById(R.id.title);
        TextView subTextView = dialog.findViewById(R.id.subtext);

        titleTextView.setText("삭제");
        subTextView.setText("정말 " + trackName + " - " + track.artistName + " 을(를) 삭제하시겠습니까?");
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(v -> {
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
                            favoritesViewModel.setFavoriteList(filtered);
                            favoriteTrackAdapter.updateData(filtered);
                            updateEmptyState(filtered.isEmpty());
                            setFavoritesCountText(filtered.size());
                            Log.d(TAG, "sort and filter successfully and updated list size");
                        }
                    });
                }
            });
            dialog.dismiss();
        });

        dialog.show();
    }






    public void deleteFavoriteArtist(Artist artist) {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_custom_with_option);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        TextView cancelButton = dialog.findViewById(R.id.cancel_button);
        TextView confirmButton = dialog.findViewById(R.id.confirm_button);
        TextView titleTextView = dialog.findViewById(R.id.title);
        TextView subTextView = dialog.findViewById(R.id.subtext);
        CheckBox deleteMetadataCheckBox = dialog.findViewById(R.id.delete_metadata_checkbox);

        titleTextView.setText("삭제");
        subTextView.setText("정말 " + artist.artistName + " 을(를) 삭제하시겠습니까?");

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            boolean optionDeleteMetadata = deleteMetadataCheckBox.isChecked();

            Log.d(TAG, "delete artist with metadata");
            favoriteArtistViewModel.deleteArtistMetadataBySpotifyId(artist.artistId, message -> {
                if (message.contains("Success")){
                    Log.d(TAG, "delete metadata Success");
                    if (optionDeleteMetadata){
                        loadFavoriteArtistsAndUpdateRecyclerView();
                    }
                }
                else if (optionDeleteMetadata){
                    Log.d(TAG, "delete metadata Fail");
                    Toast.makeText(getContext(),
                            artist.artistName + "의 메타데이터 삭제 실패",
                            Toast.LENGTH_SHORT).show();
                }
            });

            if (!optionDeleteMetadata) {
                String artworkUrl = artist.artworkUrl;
                // 체크되지 않은 경우 (기존 로직: 즐겨찾기 목록에서만 삭제)
                favoriteArtistViewModel.deleteFavoriteArtist(artist.artistId, result -> {
                    if (result > 0) {
                        // 삭제 후 즐겨찾기 목록 새로고침 (공통 로직)
                        loadFavoriteArtistsAndUpdateRecyclerView();
                        CustomFavoriteArtistImageWriter.removeUrl(viewGroupContext, artworkUrl);
                    }
                });
            }
        });

        dialog.show();
    }


    private void loadFavoriteArtistsAndUpdateRecyclerView(){
        favoriteArtistViewModel.loadFavoritesOriginalForm(updatedList -> {

            List<FavoriteArtist> filterd = SortFilterArtistUtil.sortAndFilterFavoritesList(getContext(), updatedList);

            // 목록 상태에 따라 UI 업데이트 (empty/visible)
            if (filterd.isEmpty()) {
                emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
            } else {
                emptyFavoriteSongTextView.setVisibility(View.GONE);
            }
            favoriteArtistAdapter.updateData(filterd); // RecyclerView 데이터 업데이트
            // updateEmptyState(updatedList.isEmpty()); // 이 메서드가 별도로 있다면 호출
            setFavoritesCountText(filterd.size());


        });
    }

    private void onLyricLongClick(String trackIdDb, String trackName){
        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null){
                addLyricManually(trackIdDb, trackName, true);
            } else {
                addLyricManually(trackIdDb, trackName, false);
            }
        });

    }
    private void onLyricsClick(String trackIdDb, String trackName, String albumName, String artistName, int recyclerViewPosition){

        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null && !favorite.metadata.lyrics.isEmpty()){
                LyricsOnMode(favorite, recyclerViewPosition, true);
            } else {
                favoritesViewModel.setLyricsSearchTrackId(trackIdDb);
                favoritesViewModel.setFocusedPosition(recyclerViewPosition);

                Bundle bundle = new Bundle();
                bundle.putString("track_name", trackName);
                bundle.putString("album_name", albumName);
                bundle.putString("artist_name", artistName);

                Navigation.findNavController(requireView()).navigate(R.id.action_favoritesFragment_to_autoLyricsSearchFragment, bundle);

            }
        });
    }

    private void setBackgroundColor(Favorite favorite) {
        int primaryColor = favorite.track.primaryColor;
        Context context = getContext();
        int darkenColor;
        if (context != null && DarkModeUtils.isDarkMode(context)){
            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.3f);
            int adjustedPrimaryColorForDarkMode = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.7f);
            lyricsContainer.setCardBackgroundColor(adjustedPrimaryColorForDarkMode);
        }else{
            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
            lyricsContainer.setCardBackgroundColor(primaryColor);
        }

        /**
         * 배경 그라디언트로 수정
         */
        if (context != null){
            int[] colorPair = MyColorUtils.generateBoundedContrastColors(MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.7f), 0.85f, 0.15f, 0.2f, 0.45f, 0.29f,0.3f);
            gradiantToFrame(lyricsFrameLayout, colorPair[0], colorPair[1]);
        }

        int adjustedForWhiteText = MyColorUtils.adjustForWhiteText(darkenColor);
        simpleMusicInfoContainer.setCardBackgroundColor(adjustedForWhiteText);
        lyricsTextContainer.setCardBackgroundColor(adjustedForWhiteText);

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
                    List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(getContext(), favoriteList, "ARTIST", favorite.track, "RELEASE_DATE", isDescending);
                    artistsOtherMusicAdapter.updateData(filtered, primaryColor);
                    if (filtered.isEmpty()){
                        artistsOtherMusicCardView.setVisibility(View.GONE);
                    } else{
                        artistsOtherMusicCardView.setVisibility(View.VISIBLE);
                    }

                }
            });


        }
    }

    private void LyricsOnMode(Favorite favorite, int recyclerViewPosition, boolean showSlideAnimation){

        if (getView() == null) {
            Log.w(TAG, "View not ready yet, skipping LyricsOnMode()");
            return;
        }

        if (showSlideAnimation && !Boolean.TRUE.equals(favoritesViewModel.getLyricsMode().getValue())) {
            toggleLyricsVisibility(true);
        }
        favoritesViewModel.setLyricsMode(true);
        favoritesViewModel.setFocusedTrack(favorite);
        favoritesViewModel.setFocusedPosition(recyclerViewPosition);

        if (favorite != null) {
            Log.d(TAG, "focused favorite element saved");
        }
        else{
            Log.d(TAG, "null point saved in focused favorite");
            return;
        }
        Track track = favorite.track;
        //lyricsModeCancelButton.setVisibility(View.VISIBLE);
        countLayout.setVisibility(View.GONE);
        trackRecyclerView.setVisibility(View.GONE);
        favoriteOptionSwitch.setVisibility(View.GONE);
        filterImageButton.setVisibility(View.INVISIBLE);
        dropUpImageButton.setVisibility(View.GONE);
        dropDownImageButton.setVisibility(View.GONE);
        onLyricsContainer.setVisibility(View.VISIBLE);
        keywordSearchedCountTextView.setVisibility(View.GONE);
        searchKeywordEditText.setVisibility(View.GONE);
        previousKeywordButton.setVisibility(View.GONE);
        nextKeywordButton.setVisibility(View.GONE);


        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && artistsOtherMusicRecyclerView != null && artistsOtherMusicCardView != null) {
            artistsOtherMusicRecyclerView.postDelayed(()-> artistsOtherMusicRecyclerView.smoothScrollToPosition(recyclerViewPosition), 200);

        }
       if (favorite.track.primaryColor != null) {
           setBackgroundColor(favorite);
       } else{
           ImageColorAnalyzer.analyzePrimaryColor(requireContext(), track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
               @Override
               public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                   favorite.track.primaryColor = primaryColor;
                   setBackgroundColor(favorite);
                   favoritesViewModel.updateFavoriteSong(favorite, new FavoritesViewModel.OnFavoriteViewModelCallback() {

                       @Override
                       public void onSuccess() {
                           new Handler(Looper.getMainLooper()).post(() -> Log.d(TAG, "primary color stored in db"));
                       }

                       @Override
                       public void onFailure() {
                           new Handler(Looper.getMainLooper()).post(() -> Log.d(TAG, "primary color storing failed"));
                       }
                   });
               }

               @Override
               public void onFailure() {
                   Log.d(TAG, "Fail to Analyze Primary Color of Album image");
               }
           });
       }




        Glide.with(getContext())
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

        StringBuilder lyrics = new StringBuilder(favorite.metadata.lyrics);
        lyrics.append("\n\n\n\n\n");
        lyricsTextView.setText(lyrics);
    }

    private void gradiantToFrame(FrameLayout frameLayout, int brightenColor, int darkenColor){
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{darkenColor, brightenColor, darkenColor}
        );
        gradient.setCornerRadius(0f); // 필요 시 곡률
        frameLayout.setBackground(gradient);
    }
    private void addArtistMetadataAuto(String artistId, String artistName) {
        Bundle bundle = new Bundle();
        bundle.putString("artist_id", artistId);
        bundle.putString("artist_name", artistName);
        Navigation.findNavController(requireView()).navigate(R.id.action_favoritesFragment_to_ArtistMetadataWebView, bundle);
    }

    private void addArtistMetadata(String artistId, String artistName){
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_custom_with_edittext);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);
        TextView cancelButton = dialog.findViewById(R.id.cancel_button);
        TextView confirmButton = dialog.findViewById(R.id.confirm_button);
        TextView titleTextView1= dialog.findViewById(R.id.title);
        EditText editText1 = dialog.findViewById(R.id.edit_text);
        editText1.setHint("https://vibe.naver.com/artist/#");
        editText1.setText("");
        EditText editText2 = dialog.findViewById(R.id.edit_text2);
        editText2.setVisibility(View.GONE);
        titleTextView1.setText("Artist Metadata 추가");
        confirmButton.setText("확인");
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v-> {
            dialog.dismiss();
            ArtistMetadataService.fetchMetadata(webView, editText1.getText().toString().trim() , new ArtistMetadataService.MetadataCallback() {
                @Override
                public void onSuccess(ArtistMetadata metadata) {
                    if (getActivity() != null){
                        getActivity().runOnUiThread(() -> {
                            Dialog scrollDialog = new Dialog(getContext());
                            scrollDialog.setContentView(R.layout.dialog_custom_with_scroll);
                            scrollDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                            scrollDialog.setCancelable(true);
                            TextView cancelButton = scrollDialog.findViewById(R.id.cancel_button);
                            TextView confirmButton = scrollDialog.findViewById(R.id.confirm_button);
                            TextView titleTextView = scrollDialog.findViewById(R.id.title);
                            TextView subTextView = scrollDialog.findViewById(R.id.subtext);
                            titleTextView.setText("아래 정보를 저장하시겠습니까?");
                            subTextView.setText(metadata.toString() + "\n\n\n");
                            confirmButton.setText("저장");
                            cancelButton.setOnClickListener(v -> scrollDialog.dismiss());
                            confirmButton.setOnClickListener(v -> {
                                //ArtistMetadata 수정
                                metadata.spotifyArtistId = artistId;
                                metadata.vibeArtistId = editText1.getText().toString().trim();

                                if (metadata.artistNameKr != null && metadata.artistNameKr.equals(artistName))
                                    metadata.artistNameKr = null;

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
                                        loadFavoritesAndUpdateUI();
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
                                scrollDialog.dismiss();
                            });

                            scrollDialog.show();
                        });
                    }
                }

                @Override
                public void onFailure(String reason) {}
            });
        });


        dialog.show();

    }

    private void addLyricManually(String trackIdDb, String trackName, boolean editMode){
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_custom_with_edittext_and_neutral_button);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        TextView cancelButton = dialog.findViewById(R.id.cancel_button);
        TextView confirmButton = dialog.findViewById(R.id.confirm_button);
        TextView titleTextView = dialog.findViewById(R.id.title);
        TextView subTextView = dialog.findViewById(R.id.subtext);
        EditText editText = dialog.findViewById(R.id.edit_text);
        TextView editButton = dialog.findViewById(R.id.edit_button);

        editText.setHint("https://vibe.naver.com/track/#");
        titleTextView.setText("가사 편집");
        subTextView.setText("또는 [편집]을 눌러 직접 수정");
        editText.setText("");
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        //NumberPad 세팅값 load
        SettingRepository settingRepository = new SettingRepository(requireContext());
        int originalInputType = editText.getInputType();
        new Thread(() -> {
            boolean numericPadMode = settingRepository.getNumericPreference();
            if (numericPadMode){
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            else{
                editText.setInputType(originalInputType);
            }

        }).start();
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(v -> {
            if (editText.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "VIBE 트랙 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            long startFetchLyricsTime = System.currentTimeMillis();

            String userInput = editText.getText().toString().trim();
            String trackIdNaverVibe = extractTrackId(userInput);
// 사용자 입력을 받을 첫 번째 다이얼로그
// 로딩 다이얼로그를 담을 배열 (final로 만들어 콜백에서 접근 가능하게 함)
            Dialog loadingDialog = new Dialog(getContext());
            loadingDialog = new Dialog(getContext());
            loadingDialog.setContentView(R.layout.dialog_custom);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            loadingDialog.setCancelable(false);
            TextView loadingDialog_cancelButton = loadingDialog.findViewById(R.id.cancel_button);
            TextView loadingDialog_confirmButton = loadingDialog.findViewById(R.id.confirm_button);
            TextView loadingDialog_titleTextView = loadingDialog.findViewById(R.id.title);
            TextView loadingDialog_subTextView = loadingDialog.findViewById(R.id.subtext);
            MaterialCardView cancelCardView = loadingDialog.findViewById(R.id.cancel_button_card);
            MaterialCardView confirmCardView = loadingDialog.findViewById(R.id.confirm_button_card);
            cancelCardView.setVisibility(View.GONE);
            confirmCardView.setVisibility(View.GONE);
            loadingDialog_cancelButton.setVisibility(View.GONE);
            loadingDialog_confirmButton.setVisibility(View.GONE);
            loadingDialog_titleTextView.setText("전체 정보 로딩 중...");
            loadingDialog_subTextView.setText("노래 정보와 아티스트 링크를 모두 가져오고 있습니다.");

            loadingDialog.show();

            // --- 1단계: 노래 정보 가져오기 시작 ---
            Dialog finalLoadingDialog = loadingDialog;
            LyricsSearchService.fetchMetadata(webView, trackIdNaverVibe, new LyricsSearchService.MetadataCallback() {
                @Override
                public void onSuccess(TrackMetadata metadata) {
                    if (metadata.lyrics == null || metadata.getLyrics().trim().isEmpty()) {
                        Log.d("FavoritesFragment", "Fail to fetch lyrics");
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if(finalLoadingDialog != null) finalLoadingDialog.dismiss();
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
                                                if (finalLoadingDialog != null)
                                                    finalLoadingDialog.dismiss();
                                                metadata.setVocalists(updatedVocalistList);

                                                long updatedTime = System.currentTimeMillis();
                                                Log.d("FavoritesFragment", "Lyrics: " + (startTime - startFetchLyricsTime) + "ms 소요됨");
                                                Log.d("FavoritesFragment", "link: " + (linkFetchedTime - startTime) + "ms 소요됨");
                                                Log.d("FavoritesFragment", "update metadata: " + (updatedTime - linkFetchedTime) + "ms 소요됨");

                                                Dialog dialog1 = new Dialog(requireContext());
                                                dialog1.setContentView(R.layout.dialog_custom_with_scroll);
                                                dialog1.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                                                dialog1.setCancelable(true);
                                                TextView cancelButton = dialog1.findViewById(R.id.cancel_button);
                                                TextView confirmButton = dialog1.findViewById(R.id.confirm_button);
                                                TextView titleTextView = dialog1.findViewById(R.id.title);
                                                TextView subTextView = dialog1.findViewById(R.id.subtext);
                                                titleTextView.setText("아래 정보를 저장하시겠습니까?");
                                                subTextView.setText(metadata.toString() + "\n\n\n");
                                                confirmButton.setText("저장");
                                                Dialog finalDialog = dialog1;

                                                confirmButton.setOnClickListener(v -> {
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
                                                    finalDialog.dismiss();
                                                });

                                                cancelButton.setOnClickListener(v -> finalDialog.dismiss());

                                                finalDialog.show();
                                            });
                                        }
                                        @Override
                                        public void onError(String reason) {
                                            // 2단계 실패 시 처리
                                            if (getActivity() == null) return;
                                            getActivity().runOnUiThread(() -> {
                                                if (finalLoadingDialog != null)
                                                    finalLoadingDialog.dismiss();
                                                Toast.makeText(getContext(), "아티스트 링크 업데이트 실패: " + reason, Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    }
                            );
                        });
                    }
                    else{ // vocalist 정보 없는 경우
                        if (finalLoadingDialog != null)
                            finalLoadingDialog.dismiss();
                        Dialog dialog1 = new Dialog(requireContext());
                        dialog1.setContentView(R.layout.dialog_custom_with_scroll);
                        dialog1.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                        dialog1.setCancelable(true);
                        TextView cancelButton = dialog1.findViewById(R.id.cancel_button);
                        TextView confirmButton = dialog1.findViewById(R.id.confirm_button);
                        TextView titleTextView = dialog1.findViewById(R.id.title);
                        TextView subTextView = dialog1.findViewById(R.id.subtext);
                        titleTextView.setText("아래 정보를 저장하시겠습니까?");
                        subTextView.setText(metadata.toString() + "\n\n\n");
                        confirmButton.setText("저장");

                        cancelButton.setOnClickListener(v->dialog1.dismiss());
                        confirmButton.setOnClickListener(v -> {
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
                            dialog1.dismiss();
                        });
                        dialog1.show();
                    }
                }

                @Override
                public void onFailure(String reason) {
                    // 1단계 실패 시 처리
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if(finalLoadingDialog != null) finalLoadingDialog.dismiss();
                        if(reason.contains("JavaScript 오류")){
                            Toast.makeText(getContext(), "JavaScript 오류: 유효하지 않은 주소입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "ERROR: " + reason, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            dialog.dismiss();
        });


        editButton.setOnClickListener(v -> {
            dialog.dismiss();
            Dialog dialog1 = new Dialog(getContext());
            dialog1.setContentView(R.layout.dialog_custom_with_edittext);
            dialog1.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog1.setCancelable(true);
            TextView cancelButton1 = dialog1.findViewById(R.id.cancel_button);
            TextView confirmButton1 = dialog1.findViewById(R.id.confirm_button);
            TextView titleTextView1 = dialog1.findViewById(R.id.title);
            EditText editText1 = dialog1.findViewById(R.id.edit_text);
            editText1.setHint("가사 입력");
            editText1.setText("");
            EditText editText2 = dialog1.findViewById(R.id.edit_text2);
            editText2.setHint("한국어 제목 입력");
            editText2.setText("");
            titleTextView1.setText("가사/타이틀 직접 편집");
            confirmButton1.setText("저장");
            cancelButton1.setOnClickListener(v1 -> dialog1.dismiss());

            confirmButton1.setOnClickListener(v1 -> {
                if (!(editText1.getText().toString().isEmpty() && editText2.getText().toString().isEmpty())){

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
                                    if (!editText1.getText().toString().isEmpty()) {
                                        newMetadata.lyrics = editText1.getText().toString();
                                    }

                                    // 사용자가 노래 제목(kr) 직접 입력
                                    if(!editText2.getText().toString().isEmpty()) {
                                        newMetadata.title = editText2.getText().toString();

                                    }

                                    favoritesViewModel.updateMetadata(trackIdDb, newMetadata, updated -> {
                                        if (updated > 0) {
                                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
                                            loadFavoritesAndUpdateUI();
                                        } else {
                                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                        }
                                    });
                                }
                            }
                    );
                }
                dialog1.dismiss();
            });

            dialog1.show();
        });

        dialog.show();

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
            if (position == -2){ //onlyrics mode에서 출발한 경우
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
                        //
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


        NavHostFragment.findNavController(this).navigate(R.id.artist_info, bundle, null, extras);


    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isSelectionMode){
                handleCancelSelectionMode();
            }
            else if (Boolean.TRUE.equals(favoritesViewModel.getLyricsMode().getValue())){
                cancelLyricsMode();
            }else{
                NavHostFragment.findNavController(FavoritesFragment.this).popBackStack();
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
            List<FavoriteArtist> selectedList = favoriteArtistAdapter.getSelectedList();
            for (FavoriteArtist item : selectedList){
                item.isSelected = false;
            }

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

                List<Favorite> filtered = SortFilterUtil.sortAndFilterFavoritesList(getContext(), favoritesList, null);

                if (!filtered.isEmpty())
                    trackRecyclerView.setVisibility(View.VISIBLE);
                else
                    trackRecyclerView.setVisibility(View.GONE);

                updateEmptyState(filtered.isEmpty());
                favoritesViewModel.setFavoriteList(filtered);
                favoriteTrackAdapter.updateData(filtered);


                setFavoritesCountText(filtered.size());
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
       // lyricsModeCancelButton.setVisibility(View.GONE);
        favoriteOptionSwitch.setVisibility(View.VISIBLE);
        filterImageButton.setVisibility(View.VISIBLE);
        dropUpImageButton.setVisibility(View.VISIBLE);
        loadSortDirectionAndUpdateUi();
        countLayout.setVisibility(View.VISIBLE);
        keywordSearchedCountTextView.setVisibility(View.VISIBLE);
        searchKeywordEditText.setVisibility(View.VISIBLE);
        previousKeywordButton.setVisibility(View.VISIBLE);
        nextKeywordButton.setVisibility(View.VISIBLE);
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

    private void loadSortDirectionAndUpdateUi() {
        Context context = getContext();
        if (context != null){
            SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
            if (favoriteOption == 1){
                prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
            }

            boolean isDescending = prefs.getBoolean("isDescending", false);

            if (isDescending) {
                dropDownImageButton.setVisibility(View.VISIBLE);
                dropUpImageButton.setVisibility(View.GONE);
            }
            else{
                dropUpImageButton.setVisibility(View.VISIBLE);
                dropDownImageButton.setVisibility(View.GONE);
            }
        }else{
            Log.d(TAG, "fail to getContext(), can not update list");
        }
    }

    private void trackPlay(Favorite favorite, int position) {
        List<Favorite> list = FavoritesFragment.this.favoritesViewModel.getFavoriteList();
        List<Favorite> adjusted = new ArrayList<>();
        for (int i = position; i < list.size(); ++i){
            Favorite track = list.get(i);
            if (track.audioUri != null) {
                adjusted.add(list.get(i));
            }
        }
        for (int i = 0 ; i < position; ++i) {
            Favorite track = list.get(i);
            if (track.audioUri != null) {
                adjusted.add(list.get(i));
            }
        }

        for (Favorite item: adjusted) {
            Log.d(TAG, "title: " + item.getTitle());
        }
        FavoritesFragment.this.mainActivityViewModel.setPlaylist(adjusted, 0);
    }


}

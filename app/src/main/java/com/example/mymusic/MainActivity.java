package com.example.mymusic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowMetrics;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.mymusic.data.repository.FavoriteSongRepository;
import com.example.mymusic.main.MusicPlayingBottomSheet;

import com.example.mymusic.main.NotificationUtils;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.SessionKind;
import com.example.mymusic.ui.favorites.FavoritesFragment;
import com.example.mymusic.ui.setting.SettingFragment;
import com.example.mymusic.util.DarkModeUtils;
import com.example.mymusic.util.ImageColorAnalyzer;
import com.example.mymusic.util.MyColorUtils;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mymusic.databinding.ActivityMainBinding;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@UnstableApi
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;

    //더블클릭시 해당 Fragment로 강제이동
    private int lastSelectedItemId = -1;
    //더블클릭시 해당 Fragment로 강제이동
    private long lastClickTime = 0;
    //더블클릭시 해당 Fragment로 강제이동
    private static final long DOUBLE_CLICK_THRESHOLD = 500; // 0.5초
    public Size screenSize;
    MainActivityViewModel viewModel;
    private ExoPlayer exoPlayer;
    private ImageButton audioPlayButton;
    private ImageButton audioPauseButton;

    private ImageButton repeatOffButton, repeatOnButton, repeatOneButton, shuffleButton, shuffleOnStateButton;
    private MusicPlayingBottomSheet musicPlayingBottomSheet;


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        SettingFragment.applyDarkModeSensitiveCustomStyling(this);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        WebView.setWebContentsDebuggingEnabled(true);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /**
         * playlist toggle test
         */

        viewModel.loadFavorite();

        FrameLayout musicPlayingBar = binding.musicPlayingBar;
        musicPlayingBar.setOnClickListener(v-> {
            viewModel.requestBottomSheet(true);
        });

        viewModel.getShowBottomSheet().observe(this, visible -> {
                    if (visible) {
                        MusicPlayingBottomSheet musicPlayingBottomSheet = new MusicPlayingBottomSheet().newInstance(viewModel.getCurrentTrack().getValue());
                        musicPlayingBottomSheet.show(getSupportFragmentManager(), MusicPlayingBottomSheet.TAG);
                    }
                });



        TextView titleTextView = binding.title;
        TextView artistNameTextView = binding.artistName;
        ImageView artworkImage = binding.artworkImage;
        audioPlayButton = binding.audioPlayButton;
        audioPauseButton = binding.audioPauseButton;

        audioPauseButton.setOnClickListener(v -> {
            viewModel.togglePlayPause();
        });

        audioPlayButton.setOnClickListener(v-> viewModel.togglePlayPause());

        ImageButton audioSkipPreviousButton = binding.skipPrevious;
        ImageButton audioSkipNextButton = binding.skipNext;

        audioSkipPreviousButton.setOnClickListener(v-> viewModel.playPrevious());
        audioSkipNextButton.setOnClickListener(v -> viewModel.playNext());

        repeatOffButton = binding.repeatOff;
        repeatOnButton = binding.repeatOn;
        repeatOneButton = binding.repeatOne;
        shuffleButton = binding.shuffle;
        shuffleOnStateButton = binding.shuffleOnState;

        repeatOffButton.setOnClickListener(v->  viewModel.toggleRepeatMode());
        repeatOnButton.setOnClickListener(v->  viewModel.toggleRepeatMode());
        repeatOneButton.setOnClickListener(v->  viewModel.toggleRepeatMode());

        viewModel.getRepeatMode().observe(this, this::setVisibilityByRepeatMode);

        viewModel.isPlaying().observe(this, aBoolean -> {
            if (!aBoolean.booleanValue()) {
                audioPauseButton.setVisibility(View.INVISIBLE);
                audioPlayButton.setVisibility(View.VISIBLE);
            }
            else {
                audioPlayButton.setVisibility(View.INVISIBLE);
                audioPauseButton.setVisibility(View.VISIBLE);
            }
        });
        /**
         * currentTrack Listener
         */
        viewModel.getCurrentTrack().observe(this, favorite -> {
            if (favorite == null) {
                musicPlayingBar.setVisibility(View.GONE);
                return;
            }


            musicPlayingBar.setVisibility(View.VISIBLE);
            titleTextView.setText(favorite.getTitle());
            artistNameTextView.setText(favorite.getArtistName());
            Glide.with(this)
                    .load(favorite.track.artworkUrl)
                    .override(60, 60)
                    .into(artworkImage);
            if (favorite.track.primaryColor != null) {
                int primaryColor = favorite.track.primaryColor;

                int darkenColor;
                if (DarkModeUtils.isDarkMode(this)){
                    darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.55f);
                    int adjustedPrimaryColorForDarkMode = MyColorUtils.darkenHslColor(darkenColor, 0.66f);
                    musicPlayingBar.setBackgroundColor(adjustedPrimaryColorForDarkMode);
                    titleTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(adjustedPrimaryColorForDarkMode));
                    artistNameTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(adjustedPrimaryColorForDarkMode));
                }else{
                    darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
                    musicPlayingBar.setBackgroundColor(darkenColor);
                    titleTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(darkenColor));
                    artistNameTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(darkenColor));
                }

            } else {
                ImageColorAnalyzer.analyzePrimaryColor(this, favorite.track.artworkUrl, new ImageColorAnalyzer.OnPrimaryColorAnalyzedListener() {
                    @Override
                    public void onSuccess(int dominantColor, int primaryColor, int selectedColor, int unselectedColor) {
                        favorite.track.primaryColor = primaryColor;
                        new Thread(() -> viewModel.getFavoriteSongRepository().updateFavoriteSongExceptPlayCount(favorite, new FavoriteSongRepository.FavoriteDbCallback() {
                            @Override
                            public void onSuccess() {}

                            @Override
                            public void onFailure() {}
                        })).start();

                        int darkenColor;
                        if (DarkModeUtils.isDarkMode(MainActivity.this)){
                            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.55f);
                            int adjustedPrimaryColorForDarkMode = MyColorUtils.darkenHslColor(darkenColor, 0.f);
                            musicPlayingBar.setBackgroundColor(adjustedPrimaryColorForDarkMode);
                            titleTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(adjustedPrimaryColorForDarkMode));
                            artistNameTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(adjustedPrimaryColorForDarkMode));
                        }else{
                            darkenColor = MyColorUtils.darkenHslColor(MyColorUtils.ensureContrastWithWhite(primaryColor), 0.9f);
                            musicPlayingBar.setBackgroundColor(darkenColor);
                            titleTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(darkenColor));
                            artistNameTextView.setTextColor(MyColorUtils.getSoftWhiteTextColor(darkenColor));
                        }
                    }

                    @Override
                    public void onFailure() {}
                });
            }
        });


        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_favorites, R.id.fragment_my_calendar, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.navView.setOnItemSelectedListener(item -> {
            // 항상 클릭한 탭만 강조
            Menu menu = binding.navView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                if (menuItem.getItemId() == item.getItemId()){
                    menuItem.setChecked(true);
                }
            }

            // 원래의 자동 네비게이션은 그대로 유지
            return NavigationUI.onNavDestinationSelected(item, navController);
        });


        //더블클릭시 해당 Fragment로 강제이동
        binding.navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            long currentTime = System.currentTimeMillis();

            if (itemId == lastSelectedItemId && (currentTime - lastClickTime) < DOUBLE_CLICK_THRESHOLD) {
                // 더블 클릭: 루트로 이동
                navController.popBackStack(itemId, false);

                // favorites fragment 에서 더블 클릭 시 가사 on -> off
                if (itemId == R.id.navigation_favorites) {
                    NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment_activity_main); // NavHost의 ID
                    Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);

                    if (currentFragment instanceof FavoritesFragment) {
                        ((FavoritesFragment) currentFragment).onIconDoubleTapped();
                    }

                }
            } else {
                // 일반 클릭: 원래 로직대로 작동
                NavigationUI.onNavDestinationSelected(item, navController);
            }

            lastSelectedItemId = itemId;
            lastClickTime = currentTime;
            return true;
        });


        setColor();

        // ✅ 목적지가 바뀔 때마다 호출되는 리스너 추가
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // imageDetailFragment로 이동할 때는 하단 바를 숨김
            if (destination.getId() == R.id.fragment_image_detail
                    || destination.getId() == R.id.fragment_release_date_chart) {
                navView.setVisibility(View.GONE);
                musicPlayingBar.setVisibility(View.GONE);
            }
            // 다른 주요 프래그먼트들로 돌아올 때는 다시 보이게 함
            else {
                navView.setVisibility(View.VISIBLE);
                if (viewModel.getCurrentTrack().getValue() != null) {
                    musicPlayingBar.setVisibility(View.VISIBLE);
                }
            }

            if (destination.getId() == R.id.fragment_image_detail) {
                navView.setVisibility(View.GONE);
                musicPlayingBar.setVisibility(View.GONE);
            }
            // 다른 주요 프래그먼트들로 돌아올 때는 다시 보이게 함
            else if (destination.getId() == R.id.fragment_release_date_chart || destination.getId() == R.id.fragment_play_count_chart) {
                navView.setVisibility(View.GONE);
                if (viewModel.getCurrentTrack().getValue() != null) {
                    musicPlayingBar.setVisibility(View.VISIBLE);
                }
            }
            else {
                navView.setVisibility(View.VISIBLE);
                if (viewModel.getCurrentTrack().getValue() != null) {
                    musicPlayingBar.setVisibility(View.VISIBLE);
                }
            }


            if (destination.getId() == R.id.navigation_favorites || destination.getId()  == R.id.navigation_settings || destination.getId() == R.id.fragment_my_calendar  || destination.getId() == R.id.navigation_home){
                setColor();
            }

        });

        getScreenSize(this);

        printCacheInfo(MainActivity.this);

        printExternalCacheInfo(MainActivity.this);

        Log.d("SizeDebug", "files: " + getFolderSize(this.getFilesDir()) / (1024 * 1024) + " MB");
        Log.d("SizeDebug", "webview: " + getFolderSize(this.getDir("app_webview", Context.MODE_PRIVATE)) / (1024 * 1024) + " MB");
        Log.d("SizeDebug", "code_cache: " + getFolderSize(this.getCodeCacheDir()) / (1024 * 1024) + " MB");


        shuffleButton.setOnClickListener(v-> {
            viewModel.shufflePlayList();
            shuffleOnStateButton.setVisibility(View.VISIBLE);
            shuffleButton.setVisibility(View.INVISIBLE);
        });
        shuffleOnStateButton.setOnClickListener(v->{
            shuffleOnStateButton.setVisibility(View.INVISIBLE);
            shuffleButton.setVisibility(View.VISIBLE);
        });


        viewModel.getTrackDuration().observe(this, integer -> Log.d("MainActivityViewModel", "play times changed: " + integer));


        viewModel.getShuffleMode().observe(this, aBoolean -> {
            if (aBoolean) {
                shuffleOnStateButton.setVisibility(View.VISIBLE);
                shuffleButton.setVisibility(View.INVISIBLE);
            }
            else {
                shuffleOnStateButton.setVisibility(View.INVISIBLE);
                shuffleButton.setVisibility(View.VISIBLE);
            }
        });


        SharedPreferences prefs = this.getSharedPreferences("filter_prefs_in_search_playlist_track", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("sort_option", "ADDED_DATE")
                .putString("filter_option", "ALL")
                .apply();
        /**
         * end of onCreate
         */
    }

    private void setVisibilityByRepeatMode(int repeatMode) {
        if (repeatOffButton == null) return;
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            repeatOffButton.setVisibility(View.VISIBLE);
            repeatOnButton.setVisibility(View.INVISIBLE);
            repeatOneButton.setVisibility(View.INVISIBLE);
        }
        else if (repeatMode == Player.REPEAT_MODE_ALL) {
            repeatOffButton.setVisibility(View.INVISIBLE);
            repeatOnButton.setVisibility(View.VISIBLE);
            repeatOneButton.setVisibility(View.INVISIBLE);
        }
        else {
            repeatOffButton.setVisibility(View.INVISIBLE);
            repeatOnButton.setVisibility(View.INVISIBLE);
            repeatOneButton.setVisibility(View.VISIBLE);
        }
    }

    private void hideSystemBar() {
        // 시스템 바 숨기기
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(
                this.getWindow(),
                this.getWindow().getDecorView()
        );

        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    public void getScreenSize(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
            screenSize = new Size(windowMetrics.getBounds().width(), windowMetrics.getBounds().height());
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            screenSize = new Size(metrics.widthPixels, metrics.heightPixels);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void setColor(){
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        int selectedColor = prefs.getInt("selected_color", Color.GRAY); // 기본값 회색

        if (prefs.getBoolean("basic_color", true)){
            selectedColor = ContextCompat.getColor(this, R.color.textPrimary);
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

        BottomNavigationView bottomNav = findViewById(R.id.nav_view);
        bottomNav.setItemIconTintList(colorStateList);
        bottomNav.setItemTextColor(colorStateList);
        bottomNav.setBackgroundColor(getResources().getColor(R.color.navBarBasic));
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    public void printCacheInfo(Context context) {
        File cacheDir = context.getCacheDir(); // /data/data/your.package.name/cache

        if (cacheDir != null && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            long totalSize = 0;

            if (files != null) {
                for (File file : files) {
                    long size = file.length();
                    totalSize += size;

                    Log.d("CacheInfo", "File: " + file.getName() + ", Size: " + size / 1024 + " KB");
                }
            }

            Log.d("CacheInfo", "Total Cache Size: " + totalSize / (1024 * 1024) + " MB");
        } else {
            Log.d("CacheInfo", "Cache directory not found or not accessible.");
        }
    }


    public void printExternalCacheInfo(Context context) {
        File extCacheDir = context.getExternalCacheDir(); // /storage/emulated/0/Android/data/...
        if (extCacheDir != null && extCacheDir.isDirectory()) {
            File[] files = extCacheDir.listFiles();
            long totalSize = 0;
            if (files != null) {
                for (File file : files) {
                    totalSize += file.length();
                    Log.d("ExtCache", "File: " + file.getName() + ", Size: " + file.length() / 1024 + " KB");
                }
            }
            Log.d("ExtCache", "External Cache Size: " + totalSize / (1024 * 1024) + " MB");
        }
    }

    public long getFolderSize(File dir) {
        long size = 0;
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getFolderSize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }



}




/**
 * db update uri 초기화
 */

        /*
        FavoriteSongRepository favoriteSongRepository = new FavoriteSongRepository(this);


        new Thread(()->{
            long startMs = System.currentTimeMillis();
            List<Favorite> favs =  favoriteSongRepository.getAllFavoriteTracksWithPlayCount();
            long fetchedMs = System.currentTimeMillis();
            Log.d(TAG, "favorites songs with play count by day fetched time: " + (fetchedMs - startMs) + " ms");
            for (Favorite item: favs) {
                Map<LocalDate, Integer> map = item.playCountByDay;
                if (map.isEmpty()) continue;
                LocalDate ld = null;
                for (Map.Entry<LocalDate, Integer> entry:  map.entrySet()){
                    LocalDate key = entry.getKey();
                    if (ld == null) ld = key;
                    else if (key.isAfter(ld)) ld = key;
                }
                item.lastPlayedDate = ld;
                //Log.d(TAG, item.toString());
                favoriteSongRepository.updateFavoriteSongWithPlayCount(item, new FavoriteSongRepository.FavoriteDbCallback() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure() {}
                });
            }
        }).start();
        */


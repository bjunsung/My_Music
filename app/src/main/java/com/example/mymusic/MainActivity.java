package com.example.mymusic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowMetrics;
import android.webkit.WebView;
import android.widget.ImageButton;

import com.example.mymusic.cache.ImagePreloader;
import com.example.mymusic.ui.favorites.FavoritesFragment;
import com.example.mymusic.ui.setting.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mymusic.databinding.ActivityMainBinding;

import java.io.File;

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        SettingFragment.applyDarkModeSensitiveCustomStyling(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_favorites, R.id.fragment_my_calendar, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
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
            if (destination.getId() == R.id.fragment_image_detail) {
                navView.setVisibility(View.GONE);
            }
            // 다른 주요 프래그먼트들로 돌아올 때는 다시 보이게 함
            else {
                navView.setVisibility(View.VISIBLE);
            }

            if (destination.getId() == R.id.navigation_favorites || destination.getId()  == R.id.navigation_settings || destination.getId() == R.id.navigation_searches  || destination.getId() == R.id.navigation_home){
                setColor();
            }

        });

        getScreenSize(this);

        printCacheInfo(MainActivity.this);

        printExternalCacheInfo(MainActivity.this);

        Log.d("SizeDebug", "files: " + getFolderSize(this.getFilesDir()) / (1024 * 1024) + " MB");
        Log.d("SizeDebug", "webview: " + getFolderSize(this.getDir("app_webview", Context.MODE_PRIVATE)) / (1024 * 1024) + " MB");
        Log.d("SizeDebug", "code_cache: " + getFolderSize(this.getCodeCacheDir()) / (1024 * 1024) + " MB");

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

    public long scanEverything(Context context) {
        long total = 0;
        File appRoot = new File("/data/data/" + context.getPackageName());
        total += getFolderSize(appRoot);
        Log.d("TotalAppStorage", "ALL app storage: " + total / (1024 * 1024) + " MB");
        return total;
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
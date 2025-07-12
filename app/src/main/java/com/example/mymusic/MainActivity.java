package com.example.mymusic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowMetrics;
import android.webkit.WebView;
import android.widget.ImageButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mymusic.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;

    //더블클릭시 해당 Fragment로 강제이동
    private int lastSelectedItemId = -1;
    //더블클릭시 해당 Fragment로 강제이동
    private long lastClickTime = 0;
    //더블클릭시 해당 Fragment로 강제이동
    private static final long DOUBLE_CLICK_THRESHOLD = 500; // 0.5초
    public Size screenSize;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);
        /*
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
         */

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_favorites, R.id.navigation_searches, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


        ImageButton backButton = binding.backButton;
        backButton.setOnClickListener(view -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

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


        ImageButton backButton = binding.backButton;
        backButton.setBackgroundColor(getResources().getColor(R.color.navBarBasic));
        backButton.setColorFilter(Color.DKGRAY);
        ImageButton emptySpace = binding.emptySpace;
        emptySpace.setBackgroundColor(getResources().getColor(R.color.navBarBasic));

    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }



}
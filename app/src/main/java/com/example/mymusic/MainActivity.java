package com.example.mymusic;

import static com.spotify.sdk.android.auth.AccountsQueryParameters.CLIENT_ID;
import static com.spotify.sdk.android.auth.AccountsQueryParameters.REDIRECT_URI;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.ui.favorites.FavoritesFragment;
import com.example.mymusic.ui.musicInfo.MusicInfoFragment;
import com.example.mymusic.ui.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mymusic.databinding.ActivityMainBinding;

import kotlin._Assertions;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;

    //더블클릭시 해당 Fragment로 강제이동
    private int lastSelectedItemId = -1;
    //더블클릭시 해당 Fragment로 강제이동
    private long lastClickTime = 0;
    //더블클릭시 해당 Fragment로 강제이동
    private static final long DOUBLE_CLICK_THRESHOLD = 500; // 0.5초

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*  FULL SCREEN
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        */

/*
        setContentView(R.layout.activity_main);
        // ️중복 추가 방지: 처음 실행일 때만 추가
        if (savedInstanceState == null){
            // 1. 뒤에 깔릴 Fragment (예: FavoritesFragment)
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.background_fragment_container_main, new FavoritesFragment())
                    .commit();

            // 2. 현재 화면 Fragment (예: MusicInfoFragment)
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.foreground_fragment_container_main, new MusicInfoFragment())
                    .commit();

        }


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




    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }



}
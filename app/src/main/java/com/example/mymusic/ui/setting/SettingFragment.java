package com.example.mymusic.ui.setting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mymusic.R;
import com.example.mymusic.cache.CacheUtil;
import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.util.DatabaseBackupHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class SettingFragment extends Fragment {
    private EditText maxSearchedTracksEditText, maxSearchedArtistEditText, maxSearchedAlbumnsByArtistEditText, perSonalColorEditText;
    private TextView cancelButton, confirmButton;
    private SettingRepository settingRepository;
    private int originalTracksLimit, originalArtistsLimit, originalAlbumsLimit;
    private SwitchCompat numericPadStateSwitch, favoritesColorStateSwitch;
    private boolean numericPadState, favoritesTrackColorUnificationState;
    private ImageView colorCircleStrawberryPink, colorCircleManchesterCity, colorCircleBasic, colorCirclePantone;
    private ImageButton clearCacheImageButton;
    private SharedPreferences prefs;
    private ImageButton backupButton, restoreButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        settingRepository = new SettingRepository(requireContext());
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        viewBind(view);
        loadUserSettingValues();
        setViewInitialState();
        setOnClickEvent(view);
    }


    private void viewBind(View view){
        maxSearchedTracksEditText = view.findViewById(R.id.max_search_tracks);
        maxSearchedArtistEditText = view.findViewById(R.id.max_searched_artists);
        maxSearchedAlbumnsByArtistEditText = view.findViewById(R.id.max_searched_albums_by_artist);
        cancelButton = view.findViewById(R.id.cancel_button);
        confirmButton = view.findViewById(R.id.confirm_button);
        numericPadStateSwitch = view.findViewById(R.id.number_pad_state_switch);
        colorCircleStrawberryPink = view.findViewById(R.id.color_circle_strawberry_pink);
        colorCircleManchesterCity  = view.findViewById(R.id.color_circle_manhester_city);
        colorCirclePantone = view.findViewById(R.id.color_circle_color_pantone_712c);
        colorCircleBasic = view.findViewById(R.id.color_circle_navy_blue);
        perSonalColorEditText = view.findViewById(R.id.personal_color);
        favoritesColorStateSwitch = view.findViewById(R.id.favorites_color_state_switch);
        clearCacheImageButton = view.findViewById(R.id.clear_cache_button);
        backupButton = view.findViewById(R.id.backup_button);
        restoreButton = view.findViewById(R.id.restore_button);
    }

    private void setViewInitialState(){
        confirmButton.setVisibility(View.INVISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
    }

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 여기서 초기화 (생명주기 안전)
        backupLauncher = DatabaseBackupHelper.initBackupLauncher(this);
        restoreLauncher = DatabaseBackupHelper.initRestoreLauncher(this);
    }

    @SuppressLint("ResourceAsColor")
    private void setOnClickEvent(View view){
        confirmButton.setOnClickListener(v -> {
            this.saveIfValid();
           // loadInitialValues();
            confirmButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.INVISIBLE);
            // 키보드 닫기
            if(view != null){
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        cancelButton.setOnClickListener(v -> {
            perSonalColorEditText.setText("");
            perSonalColorEditText.setSelection(perSonalColorEditText.getText().length());
            this.restoreOriginalValues();
            confirmButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.INVISIBLE);
        });

        maxSearchedTracksEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (!maxSearchedTracksEditText.getText().toString().isEmpty()) {
                confirmButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
            }
            return true;
        });

        maxSearchedArtistEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (!maxSearchedArtistEditText.getText().toString().isEmpty()) {
                confirmButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
            }
            return true;
        });

        maxSearchedAlbumnsByArtistEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (!maxSearchedAlbumnsByArtistEditText.getText().toString().isEmpty()) {
                confirmButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
            }
            return true;
        });

        numericPadStateSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (isChecked) {
                new Thread(() -> {
                    settingRepository.setNumericPreference(true);
                }).start();
            }
            else {
                new Thread(() -> {
                    settingRepository.setNumericPreference(false);
                }).start();
            }
        }));

        favoritesColorStateSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (prefs == null) {
                prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            }
            if (isChecked){
                prefs.edit().putBoolean("favorites_track_color_unification_state", true).apply();
            }
            else{
                prefs.edit().putBoolean("favorites_track_color_unification_state", false).apply();
            }
        }));

        colorSetting();

        perSonalColorEditText.setOnEditorActionListener((v, actionId, event) -> {
           if (actionId == EditorInfo.IME_ACTION_DONE){


               String input = perSonalColorEditText.getText().toString().trim();
               int color = parseColorCode(input);
               change_color(color);

               InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
               imm.hideSoftInputFromWindow(perSonalColorEditText.getWindowToken(), 0);
               return true;
           }
           return false;
        });


        perSonalColorEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && perSonalColorEditText.getText().toString().isEmpty()) {
                perSonalColorEditText.setText("#");
                perSonalColorEditText.setSelection(perSonalColorEditText.getText().length());
            }
        });


        Context context = getContext();
        clearCacheImageButton.setOnClickListener(v -> {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_custom_with_radio_group);

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);

            RadioGroup radioGroup = dialog.findViewById(R.id.radio_group_container);
            TextView cancelButton = dialog.findViewById(R.id.cancel_button);
            TextView confirmButton = dialog.findViewById(R.id.confirm_button);
            confirmButton.setText("삭제");

            TextView title = dialog.findViewById(R.id.title);
            title.setText("캐시 삭제");
            RadioButton radioOption1 = dialog.findViewById(R.id.radio_option1);
            RadioButton radioOption2 = dialog.findViewById(R.id.radio_option2);
            RadioButton radioOption3 = dialog.findViewById(R.id.radio_option3);
            radioOption1.setText("Memory Cache");
            radioOption2.setText("Disk Cache");
            radioOption3.setText("모든 캐시 삭제");

            cancelButton.setOnClickListener(v1 -> dialog.dismiss());

            confirmButton.setOnClickListener(v2 -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selected = dialog.findViewById(selectedId);
                    String selectedText = selected.getText().toString();

                    switch (selectedText) {
                        case "Memory Cache":
                            CacheUtil.clearMemoryCache(context);
                            Toast.makeText(context, "Memory Cache를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                            break;
                        case "Disk Cache":
                            CacheUtil.clearDiskCache(context);
                            Toast.makeText(context, "Disk Cache를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                            break;
                        case "모든 캐시 삭제":
                            CacheUtil.celarAllCaches(context);
                            Toast.makeText(context, "모든 Cache를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "삭제할 캐시를 선택하세요.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        });







        backupButton.setOnClickListener(v-> {

            DatabaseBackupHelper.startBackup((AppCompatActivity) requireActivity(), backupLauncher);
        });

        restoreButton.setOnClickListener(v-> {

            DatabaseBackupHelper.startRestore((AppCompatActivity) requireActivity(), restoreLauncher);
        });


    }



    private void loadUserSettingValues() {
        // 백그라운드 쓰레드에서 Room 접근
        new Thread(() -> {
            originalTracksLimit = settingRepository.getMaxSearchedTracks();
            originalArtistsLimit = settingRepository.getMaxSearchedArtists();
            originalAlbumsLimit = settingRepository.getMaxSearchedAlbumsByArtist();
            numericPadState = settingRepository.getNumericPreference();
            // 여기서 호출해야 값이 정상적으로 표시됨
            requireActivity().runOnUiThread(this::setTextSync);
        }).start();

        if (prefs == null) {
            prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        }
        favoritesTrackColorUnificationState = prefs.getBoolean("favorites_track_color_unification_state", false);

    }

    private void setTextSync(){

        maxSearchedTracksEditText.setHint(String.valueOf(originalTracksLimit));
        maxSearchedArtistEditText.setHint(String.valueOf(originalArtistsLimit));
        maxSearchedAlbumnsByArtistEditText.setHint(String.valueOf(originalAlbumsLimit));

        maxSearchedTracksEditText.setText("");
        maxSearchedAlbumnsByArtistEditText.setText("");
        maxSearchedArtistEditText.setText("");

        numericPadStateSwitch.setChecked(numericPadState);
        favoritesColorStateSwitch.setChecked(favoritesTrackColorUnificationState);
    }


    private void restoreOriginalValues() {
        View view = getActivity().getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        maxSearchedTracksEditText.setText("");
        maxSearchedArtistEditText.setText("");
        maxSearchedAlbumnsByArtistEditText.setText("");
        maxSearchedTracksEditText.setHint(String.valueOf(originalTracksLimit));
        maxSearchedArtistEditText.setHint(String.valueOf(originalArtistsLimit));
        maxSearchedAlbumnsByArtistEditText.setHint(String.valueOf(originalAlbumsLimit));

        Window window = requireActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.machester_city_official_color));


    }

    private void saveIfValid() {
        String strTracks = maxSearchedTracksEditText.getText().toString().trim();
        String strArtists = maxSearchedArtistEditText.getText().toString().trim();
        String strAlbums = maxSearchedAlbumnsByArtistEditText.getText().toString().trim();
        if (!(isValid(strTracks) && isValid(strArtists) && isValid(strAlbums) )) {
            Toast.makeText(getContext(), "5이상 50이하의 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
            loadUserSettingValues();
        }

        try {
            boolean maxTracksUpdate = true, maxArtistsUpdate = true, maxAlbumsUpdate = true;
            int valTracks, valAlbums, valArtists;
            try {
                valTracks = Integer.parseInt(strTracks);
            } catch (NumberFormatException e){
                valTracks = -1;
            }
            try {
                valAlbums = Integer.parseInt(strAlbums);
            } catch (NumberFormatException e){
                valAlbums = -1;
            }
            try {
                valArtists = Integer.parseInt(strArtists);
            } catch (NumberFormatException e){
                valArtists = -1;
            }

            if (strTracks.isEmpty() || originalTracksLimit == Integer.parseInt(strTracks))
                maxTracksUpdate = false;
            if (strAlbums.isEmpty() || originalAlbumsLimit == Integer.parseInt(strAlbums))
                maxAlbumsUpdate = false;
            if (strArtists.isEmpty() || originalArtistsLimit == Integer.parseInt(strArtists))
                maxArtistsUpdate = false;
            final boolean finalMaxTrackUpdate = maxTracksUpdate;
            final boolean finalMaxAlbumUpdate = maxAlbumsUpdate;
            final boolean finalMaxArtistUpdate = maxArtistsUpdate;
            final int finalTracks = valTracks;
            final int finalAlbums = valAlbums;
            final int finalArtist = valArtists;

            new Thread(() -> {
                boolean storeSuccess = true;
                if (finalMaxTrackUpdate){
                    boolean success = settingRepository.setMaxSearchedTracks(finalTracks);
                    storeSuccess &= success;
                    requireActivity().runOnUiThread(() -> {
                        maxSearchedTracksEditText.setText("");
                        maxSearchedTracksEditText.setHint(String.valueOf(finalTracks));
                    });
                }
                if (finalMaxAlbumUpdate) {
                    boolean success = settingRepository.setMaxSearchedAlbumsByArtist(finalAlbums);
                    storeSuccess &= success;
                    requireActivity().runOnUiThread(() -> {
                        maxSearchedAlbumnsByArtistEditText.setText("");
                        maxSearchedAlbumnsByArtistEditText.setHint(String.valueOf(finalAlbums));
                    });
                }
                if (finalMaxArtistUpdate) {
                    boolean success = settingRepository.setMaxSearchedArtists(finalArtist);
                    storeSuccess &= success;
                    requireActivity().runOnUiThread(() -> {
                        maxSearchedArtistEditText.setText("");
                        maxSearchedArtistEditText.setHint(String.valueOf(finalArtist));
                    });
                }

                if((finalMaxTrackUpdate || finalMaxAlbumUpdate || finalMaxArtistUpdate) && storeSuccess){
                    requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();
                            loadUserSettingValues();
                    });
                }
            }).start();


        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "숫자를 입력하세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValid(String str) {
        try {
            int value = Integer.parseInt(str);
            return value >= 5 && value <= 50;
        } catch(NumberFormatException e){
            return true;
        }
    }


    private void colorSetting() {
        colorCircleStrawberryPink.setOnClickListener(v -> change_color(ContextCompat.getColor(requireContext(), R.color.apink_official_color)));
        colorCircleBasic.setOnClickListener(v -> {
            change_color(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            if (prefs == null) {
                prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            }
            prefs.edit().putBoolean("basic_color", true).apply();
        });
        colorCirclePantone.setOnClickListener(v -> change_color(ContextCompat.getColor(requireContext(), R.color.pantone)));
        colorCircleManchesterCity.setOnClickListener(v -> change_color(ContextCompat.getColor(requireContext(), R.color.machester_city_official_color)));
    }




    private int parseColorCode(String hexCode) {
        try {
            return Color.parseColor(hexCode);
        } catch (IllegalArgumentException e) {
            // 기본값: 회색 반환
            return Color.GRAY;
        }
    }

    public static void applyDarkModeSensitiveCustomStyling(Activity activity){
        // get color value
        SharedPreferences prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("basic_color", false)){
            change_color(activity, ContextCompat.getColor(activity, R.color.textPrimary));
        }
    }

    private static void change_color(Activity activity, int color) {
       BottomNavigationView bottomNav = activity.findViewById(R.id.nav_view);
        if (bottomNav != null) {
            int unselectedColor = Color.GRAY;

            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_checked },
                    new int[] { -android.R.attr.state_checked }
            };

            int[] colors = new int[] {
                    color,
                    unselectedColor
            };

            ColorStateList colorStateList = new ColorStateList(states, colors);

            bottomNav.setItemIconTintList(colorStateList);
            bottomNav.setItemTextColor(colorStateList);
        }
    }
    private void change_color(int color) {
        BottomNavigationView bottomNav = getActivity().findViewById(R.id.nav_view);
        if (bottomNav != null) {
            int unselectedColor = Color.GRAY;

            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_checked },
                    new int[] { -android.R.attr.state_checked }
            };

            int[] colors = new int[] {
                    color,
                    unselectedColor
            };

            ColorStateList colorStateList = new ColorStateList(states, colors);

            bottomNav.setItemIconTintList(colorStateList);
            bottomNav.setItemTextColor(colorStateList);

            // save color value
            if (prefs == null) {
                prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            }
            prefs.edit().putInt("selected_color", color).apply();
            prefs.edit().putBoolean("basic_color", false).apply();
        }
    }


}


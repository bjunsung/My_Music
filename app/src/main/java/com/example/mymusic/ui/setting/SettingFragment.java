package com.example.mymusic.ui.setting;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mymusic.R;
import com.example.mymusic.data.repository.SettingRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class SettingFragment extends Fragment {
    private EditText maxSearchedTracksEditText, maxSearchedArtistEditText, maxSearchedAlbumnsByArtistEditText, perSonalColorEditText;
    private TextView cancelButton, confirmButton;
    private SettingRepository settingRepository;
    private int originalTracksLimit, originalArtistsLimit, originalAlbumsLimit;
    private SwitchCompat numericPadStateSwitch;
    private boolean numericPadState;
    private ImageView colorCircleStrawberryPink, colorCircleManchesterCity, colorCircleNavy, colorCirclePantone;


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
        loadInitialValues();
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
        colorCircleNavy = view.findViewById(R.id.color_circle_navy_blue);
        perSonalColorEditText = view.findViewById(R.id.personal_color);
    }

    private void setViewInitialState(){
        confirmButton.setVisibility(View.INVISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
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

    }



    private void loadInitialValues() {
        // 백그라운드 쓰레드에서 Room 접근
        new Thread(() -> {
            originalTracksLimit = settingRepository.getMaxSearchedTracks();
            originalArtistsLimit = settingRepository.getMaxSearchedArtists();
            originalAlbumsLimit = settingRepository.getMaxSearchedAlbumsByArtist();
            numericPadState = settingRepository.getNumericPreference();
            // 여기서 호출해야 값이 정상적으로 표시됨
            requireActivity().runOnUiThread(this::setTextSync);
        }).start();
    }

    private void setTextSync(){

        maxSearchedTracksEditText.setHint(String.valueOf(originalTracksLimit));
        maxSearchedArtistEditText.setHint(String.valueOf(originalArtistsLimit));
        maxSearchedAlbumnsByArtistEditText.setHint(String.valueOf(originalAlbumsLimit));

        maxSearchedTracksEditText.setText("");
        maxSearchedAlbumnsByArtistEditText.setText("");
        maxSearchedArtistEditText.setText("");

        numericPadStateSwitch.setChecked(numericPadState);
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
            loadInitialValues();
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
                            loadInitialValues();
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
        colorCircleNavy.setOnClickListener(v -> change_color(ContextCompat.getColor(requireContext(), R.color.navy_blue)));
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
            SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            prefs.edit().putInt("selected_color", color).apply();
        }
    }


}


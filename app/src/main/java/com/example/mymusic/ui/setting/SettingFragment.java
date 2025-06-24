package com.example.mymusic.ui.setting;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mymusic.R;
import com.example.mymusic.data.repository.SettingRepository;

public class SettingFragment extends Fragment {
    private EditText maxSearchedTracksEditText;
    private EditText maxSearchedArtistEditText;
    private EditText maxSearchedAlbumnsByArtistEditText;
    private TextView cancelButton, confirmButton;
    private SettingRepository repository;
    private int originalTracksLimit, originalArtistsLimit, originalAlbumsLimit;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        repository = new SettingRepository(requireContext());
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        maxSearchedTracksEditText = view.findViewById(R.id.max_search_tracks);
        maxSearchedArtistEditText = view.findViewById(R.id.max_searched_artists);
        maxSearchedAlbumnsByArtistEditText = view.findViewById(R.id.max_searched_albums_by_artist);
        cancelButton = view.findViewById(R.id.cancel_button);
        confirmButton = view.findViewById(R.id.confirm_button);

        loadInitialValues();

        confirmButton.setOnClickListener(v -> {
            this.saveIfValid();
            loadInitialValues();
        });
        cancelButton.setOnClickListener(v -> this.restoreOriginalValues());

    }

    private void loadInitialValues() {
        // 백그라운드 쓰레드에서 Room 접근
        new Thread(() -> {
            originalTracksLimit = repository.getMaxSearchedTracks();
            originalArtistsLimit = repository.getMaxSearchedArtists();
            originalAlbumsLimit = repository.getMaxSearchedAlbumsByArtist();
            // 여기서 호출해야 값이 정상적으로 표시됨
            requireActivity().runOnUiThread(this::setTextSync);
        }).start();
    }

    private void setTextSync(){
        maxSearchedTracksEditText.setText("");
        maxSearchedAlbumnsByArtistEditText.setText("");
        maxSearchedArtistEditText.setText("");
        maxSearchedTracksEditText.setHint(String.valueOf(originalTracksLimit));
        maxSearchedArtistEditText.setHint(String.valueOf(originalArtistsLimit));
        maxSearchedAlbumnsByArtistEditText.setHint(String.valueOf(originalAlbumsLimit));
    }


    private void restoreOriginalValues() {
        View view = getActivity().getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        maxSearchedTracksEditText.setText("");
        maxSearchedTracksEditText.setHint(String.valueOf(originalTracksLimit));
        maxSearchedArtistEditText.setText("");
        maxSearchedArtistEditText.setHint(String.valueOf(originalArtistsLimit));
        maxSearchedAlbumnsByArtistEditText.setText("");
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
        if (!(isValid(strTracks) && isValid(strArtists) && isValid(strAlbums) ))
            Toast.makeText(getContext(), "5이상 50이하의 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();

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
                    boolean success = repository.setMaxSearchedTracks(finalTracks);
                    storeSuccess &= success;
                    requireActivity().runOnUiThread(() -> {
                        maxSearchedTracksEditText.setText("");
                        maxSearchedTracksEditText.setHint(String.valueOf(finalTracks));
                    });
                }
                if (finalMaxAlbumUpdate) {
                    boolean success = repository.setMaxSearchedAlbumsByArtist(finalAlbums);
                    storeSuccess &= success;
                    requireActivity().runOnUiThread(() -> {
                        maxSearchedAlbumnsByArtistEditText.setText("");
                        maxSearchedAlbumnsByArtistEditText.setHint(String.valueOf(finalAlbums));
                    });
                }
                if (finalMaxArtistUpdate) {
                    boolean success = repository.setMaxSearchedArtists(finalArtist);
                    storeSuccess &= success;
                    requireActivity().runOnUiThread(() -> {
                        maxSearchedArtistEditText.setText("");
                        maxSearchedArtistEditText.setHint(String.valueOf(finalArtist));
                    });
                }

                if((finalMaxTrackUpdate || finalMaxAlbumUpdate || finalMaxArtistUpdate) && storeSuccess){
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
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


}


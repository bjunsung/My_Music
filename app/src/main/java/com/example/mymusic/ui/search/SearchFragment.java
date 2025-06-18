package com.example.mymusic.ui.search;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.SongAdapter;
import com.example.mymusic.model.SongItem;
import com.example.mymusic.network.TokenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY = 500;
    private String accessToken = null;
    private EditText searchEditText;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.searchEditText);
        recyclerView = view.findViewById(R.id.resultRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        getTokenAndEnableSearch();
    }

    private void getTokenAndEnableSearch() {
        TokenHelper.getAccessToken(new TokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                accessToken = token;

                // 메인 스레드에서 리스너 설정
                requireActivity().runOnUiThread(() -> {
                    searchEditText.addTextChangedListener(new TextWatcher() {
                        private Handler searchHandler = new Handler();
                        private Runnable searchRunnable;
                        private static final long SEARCH_DELAY = 500;

                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                            searchRunnable = () -> {
                                if (accessToken != null && s.length() >= 2) {
                                    searchSongs(s.toString(), accessToken);
                                }
                            };
                            searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                        }

                        @Override public void afterTextChanged(Editable s) {}
                    });
                });
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("토큰 오류")
                            .setMessage(error)
                            .setPositiveButton("닫기", null)
                            .show();
                });
            }
        });
    }

    private void searchSongs(String keyword, String accessToken) {
        if (keyword.trim().isEmpty()) return;

        new Thread(() -> {
            try {
                String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
                String urlStr = "https://api.spotify.com/v1/search?q=" + encodedKeyword + "&type=track&market=KR&limit=20";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray items = json.getJSONObject("tracks").getJSONArray("items");

                List<SongItem> songs = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject obj = items.getJSONObject(i);
                    JSONObject album = obj.getJSONObject("album");
                    JSONArray images = album.getJSONArray("images");

                    String imageUrl = images.length() > 0 ? images.getJSONObject(0).getString("url") : "";

                    songs.add(new SongItem(
                            obj.getString("name"),
                            obj.getJSONArray("artists").getJSONObject(0).getString("name"),
                            imageUrl,
                            album.optString("name", "정보없음"),
                            album.optString("release_date", ""),
                            obj.optString("preview_url", "")
                    ));
                }

                requireActivity().runOnUiThread(() -> {
                    SongAdapter adapter = new SongAdapter(songs, getContext(), song -> showDetails(song));
                    RecyclerView recyclerView = requireView().findViewById(R.id.resultRecyclerView);
                    recyclerView.setAdapter(adapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        new AlertDialog.Builder(getContext())
                                .setTitle("검색 오류")
                                .setMessage("노래 검색 중 오류 발생:\n" + e.getMessage())
                                .setPositiveButton("닫기", null)
                                .show()
                );
            }
        }).start();
    }

    private void showDetails(SongItem song) {
        new AlertDialog.Builder(getContext())
                .setMessage("title: " + song.trackName + "\nartist: " + song.artistName +
                        "\nalbun: " + song.albumName +
                        "\n발매일: " + song.releaseDate.substring(0, 10))
                .setPositiveButton("닫기", null)
                .show();
    }
}

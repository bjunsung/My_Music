package com.example.mymusic.ui.search;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.model.SearchViewModel;
import com.example.mymusic.model.TrackAdapter;
import com.example.mymusic.model.Track;
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
    private boolean searchLocked = false;
    private String finalKeyword = null;
    private SearchViewModel viewModel;
    private TokenRepository tokenRepository;


    //ViewModel 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        tokenRepository = new TokenRepository(requireContext().getApplicationContext());
    }

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

        if(!viewModel.searchResults.isEmpty()){
            TrackAdapter adapter = new TrackAdapter(viewModel.searchResults, getContext(), accessToken, track -> showDetails(track));
            recyclerView.setAdapter(adapter);
        }

        getTokenAndEnableSearch();
    }

    private void getTokenAndEnableSearch() {
        new Thread(() -> {
            Token token = tokenRepository.getAccessTokenSync();

            if (token != null && token.getAccessToken() != null) {
                accessToken = token.getAccessToken();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "load token in db: " + accessToken, Toast.LENGTH_SHORT).show();
                });

                requireActivity().runOnUiThread(this::setupSearchUI);
            } else {
                // 토큰이 없으면 새로 발급
                TokenHelper.getAccessToken(requireContext(), new TokenHelper.TokenCallback() {
                    @Override
                    public void onSuccess(String token) {
                        accessToken = token;
                        // ✅ Room DB에 저장하는 코드 추가 필요
                        TokenRepository repo = new TokenRepository(requireContext().getApplicationContext());
                        repo.setAccessToken(token);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "store token to db: " + accessToken, Toast.LENGTH_SHORT).show();
                        });
                        requireActivity().runOnUiThread(SearchFragment.this::setupSearchUI);
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("token error")
                                    .setMessage(error)
                                    .setPositiveButton("exit", null)
                                    .show();
                        });
                    }
                });
            }
        }).start();
    }


    private void lockAndSearchKeyword(){
        //hide keyboard
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        finalKeyword = searchEditText.getText().toString();
        if(finalKeyword.length() >= 1 && accessToken != null) {
            searchTracks(finalKeyword, accessToken);
            searchLocked = true;
        }
    }

    private void setupSearchUI() {
        //search button click event
        ImageButton searchButton = requireView().findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            lockAndSearchKeyword();  // 아이콘 눌렀을 때 검색 실행
        });

        //delete keyword button click event
        ImageButton deletekeywordButton = requireView().findViewById(R.id.deleteKeywordButton);
        deletekeywordButton.setOnClickListener(v -> {
            searchEditText.setText("");
        });

        // search(enter in keyboard) click event
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                lockAndSearchKeyword();
                return true;
            }
            return false;
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            private final Handler searchHandler = new Handler();
            private Runnable searchRunnable;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchLocked && !s.toString().equals(finalKeyword)) {
                    searchLocked = false;
                }

                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                searchRunnable = () -> {
                    if (accessToken != null && !searchLocked && s.length() >= 1) {
                        searchTracks(s.toString(), accessToken);
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void searchTracks(String keyword, String accessToken) {
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

                List<Track> tracks = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject obj = items.getJSONObject(i);
                    JSONObject album = obj.getJSONObject("album");
                    JSONArray images = album.getJSONArray("images");
                    JSONArray artist = obj.getJSONObject("album").getJSONArray("artists");
                    String imageUrl = images.length() > 0 ? images.getJSONObject(0).getString("url") : "";

                    tracks.add(new Track(
                            obj.getString("id"),
                            album.getString("id"),
                            artist.getJSONObject(0).getString("id"),
                            obj.getString("name"),
                            album.optString("name", "정보없음"),
                            obj.getJSONArray("artists").getJSONObject(0).getString("name"),
                            imageUrl,
                            album.optString("release_date", ""),
                            obj.getString("duration_ms")
                    ));
                }

                //searchTracks()결과를 ViewModel 에 저장
                viewModel.searchResults.clear();
                viewModel.searchResults.addAll(tracks);

                requireActivity().runOnUiThread(() -> {
                    TrackAdapter adapter = new TrackAdapter(tracks, getContext(), accessToken, track -> showDetails(track));
                    RecyclerView recyclerView = requireView().findViewById(R.id.resultRecyclerView);
                    recyclerView.setAdapter(adapter);
                });

            } catch (Exception e) {
                Log.d("exception occurred searching track", "accessToken: " + accessToken);
                Log.d("exception occurred searching track", "url: " + e.getMessage());
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        new AlertDialog.Builder(getContext())
                                .setTitle("error")
                                .setMessage("노래 검색 중 오류 발생:\n" + e.getMessage())
                                .setPositiveButton("닫기", null)
                                .show()
                );
            }
        }).start();
    }

    private void showDetails(Track track) {
        new AlertDialog.Builder(getContext())
                .setMessage("제목:\t\t\t" + track.trackName +
                        "\n아티스트:\t" + track.artistName +
                        "\n앨범:\t\t\t" + track.albumName +
                        "\n발매일:\t\t" + track.releaseDate.substring(0, 10))
                .setPositiveButton("닫기", null)
                .show();
    }
}

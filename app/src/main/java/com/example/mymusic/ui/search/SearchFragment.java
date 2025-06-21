package com.example.mymusic.ui.search;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
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
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistAdapter;
import com.example.mymusic.model.TrackAdapter;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.TokenHelper;
import com.example.mymusic.ui.favorites.FavoritesViewModel;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    private static final long SEARCH_DELAY = 500;
    private String accessToken = null;
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private boolean searchLocked = false;
    private String finalKeyword = null;
    private SearchViewModel searchViewModel;
    private TokenRepository tokenRepository;

    private FavoritesViewModel favoritesViewModel;
    private static final int MAX_RETRY_REFRESH_TOKEN_COUNT = 3;
    private static int retryRefreshTokenCount;


    //ViewModel 연결, repository 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);

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

        if(!(searchViewModel.searchTrackResults.isEmpty() || searchViewModel.searchArtistResults.isEmpty() )){
            RecyclerView.Adapter adapter;
            if (searchViewModel.selectedOption == 0)
                adapter = new TrackAdapter(searchViewModel.searchTrackResults, getContext(), this::showTrackDetails, this::addFavoriteSong);
            else
                adapter = new ArtistAdapter(searchViewModel.searchArtistResults, getContext(), artist -> this.showArtistDetails(artist));
            recyclerView.setAdapter(adapter);
        }
        retryRefreshTokenCount = 0;
        getTokenAndEnableSearch();
    }

    //토큰 재발급
    private void refreshToken(){
        TokenHelper.getAccessToken(requireContext(), new TokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                accessToken = token;
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

    private void getTokenAndEnableSearch() {
        new Thread(() -> {
            Token token = tokenRepository.getAccessTokenSync();

            if (token != null && token.getAccessToken() != null) {
                accessToken = token.getAccessToken();
                requireActivity().runOnUiThread(this::setupSearchUI);
            } else {
                refreshToken();
            }
        }).start();
    }


    private void lockAndSearchKeyword(){
        //hide keyboard
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        finalKeyword = searchEditText.getText().toString();
        if(finalKeyword.length() >= 1 && accessToken != null) {
            search(finalKeyword, accessToken, searchViewModel.selectedOption);
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


        //searchModeButton
        ImageButton searchModeImageButton = requireView().findViewById(R.id.searchModeButton);
        searchModeImageButton.setOnClickListener(v -> {
            showOptionDialog();
        });

        if (searchViewModel.selectedOption == 0)
            searchEditText.setHint("노래 제목을 입력하세요");
        else
            searchEditText.setHint("아티스트 이름을 입력하세요");

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
                    if (accessToken != null && !searchLocked && s.length() >= 1)
                        search(s.toString(), accessToken, searchViewModel.selectedOption);

                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void search(String keyword, String accessToken, int searchTypeInt) {
        if (keyword.trim().isEmpty()) return;
        new Thread(() -> {
            try {
                String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
                String searchType = (searchTypeInt == 0) ? "track" : "artist";
                String urlStr = "https://api.spotify.com/v1/search?q=" + encodedKeyword + "&type=" + searchType + "&market=KR&limit=20";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){ //401 accessToken expired
                    if(retryRefreshTokenCount++ >= MAX_RETRY_REFRESH_TOKEN_COUNT){
                        retryRefreshTokenCount = 0;
                        requireActivity().runOnUiThread(() -> {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("ERROR")
                                    .setMessage("만료된 토큰입니다.")
                                    .setPositiveButton("확인", null)
                                    .show();
                        });
                        return;
                    }
                    refreshToken();
                    search(keyword, accessToken, searchViewModel.selectedOption);
                    return;
                } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) { //403
                    requireActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(getContext())
                                .setTitle("접근 제한됨")
                                .setMessage("이 요청에 대한 권한이 없습니다.")
                                .setPositiveButton("확인", null)
                                .show();
                    });
                } else {
                    retryRefreshTokenCount = 0;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())); //conn.getInputStream() 여기서 401, 403 등 exception throw 할 수 있음
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());

                if (searchTypeInt == 0) { //search track
                    JSONArray items = json.getJSONObject("tracks").getJSONArray("items");
                    List<Track> tracks = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject obj = items.getJSONObject(i);
                        JSONObject album = obj.getJSONObject("album");
                        JSONArray images = album.getJSONArray("images");
                        JSONArray artist = obj.getJSONArray("artists");
                        String imageUrl = images.length() > 0 ? images.getJSONObject(0).getString("url") : "";

                        tracks.add(new Track(
                                obj.getString("id"),
                                album.getString("id"),
                                artist.getJSONObject(0).getString("id"),
                                obj.getString("name"),
                                album.optString("name", "정보없음"),
                                artist.getJSONObject(0).getString("name"),
                                imageUrl,
                                album.optString("release_date", ""),
                                obj.getString("duration_ms")
                        ));
                    }

                    //searchTracks()결과를 ViewModel 에 저장
                    searchViewModel.searchTrackResults.clear();
                    searchViewModel.searchTrackResults.addAll(tracks);

                    //new Thread 백그라운드 작업이므로 requireActivity().runOnUiThread() 로 Fragment가 붙어있는 Activity를 반환
                    requireActivity().runOnUiThread(() -> {
                        TrackAdapter adapter = new TrackAdapter(tracks, getContext(), track -> showTrackDetails(track), track -> addFavoriteSong(track));
                        RecyclerView recyclerView = requireView().findViewById(R.id.resultRecyclerView);
                        recyclerView.setAdapter(adapter);
                    });
                }
                else if(searchTypeInt == 1){ //search artist
                    JSONArray items = json.getJSONObject("artists").getJSONArray("items");

                    List<Artist> artists = new ArrayList<>();
                    for (int i=0; i < items.length(); i++){
                        JSONObject obj = items.getJSONObject(i);
                        JSONArray genresJSONArray = obj.getJSONArray("genres");
                        JSONArray images = obj.getJSONArray("images");
                        String imageUrl = images.length() > 0 ? images.getJSONObject(0).getString("url") : "";
                        JSONObject followers = obj.getJSONObject("followers");
                        List<String> genres = new ArrayList<>();
                        for (int j=0; j<genresJSONArray.length(); j++){
                            genres.add(genresJSONArray.getString(j));
                        }
                        artists.add(new Artist(
                                obj.getString("id"),
                                obj.getString("name"),
                                imageUrl,
                                genres,
                                (int) Double.parseDouble(followers.getString("total")),
                                (int) Double.parseDouble(obj.getString("popularity"))
                        ));
                    }
                    //searchTracks()결과를 ViewModel 에 저장
                    searchViewModel.searchArtistResults.clear();
                    searchViewModel.searchArtistResults.addAll(artists);

                    //new Thread 백그라운드 작업이므로 requireActivity().runOnUiThread() 로 Fragment가 붙어있는 Activity를 반환
                    requireActivity().runOnUiThread(() -> {
                        ArtistAdapter adapter = new ArtistAdapter(artists, getContext(), artist -> this.showArtistDetails(artist));
                        RecyclerView recyclerView = requireView().findViewById(R.id.resultRecyclerView);
                        recyclerView.setAdapter(adapter);
                    });
                }

            } catch (Exception e) {
                Log.d("exception occurred searching track", "accessToken: " + accessToken);
                Log.d("exception occurred searching track", "url: " + e.getMessage());
                e.printStackTrace();
                String message = (searchViewModel.selectedOption == 0) ? "노래 검색 중 오류 발생:\n" : "아티스트 검색 중 오류 발생:\n";
                requireActivity().runOnUiThread(() ->
                        new AlertDialog.Builder(getContext())
                                .setTitle("error")
                                .setMessage(message + e.getMessage() + "\naccess token: " + accessToken)
                                .setPositiveButton("닫기", null)
                                .show()
                );
            }
        }).start();
    }


    private void showTrackDetails(Track track) {
        new AlertDialog.Builder(getContext())
                .setTitle("세부사항")
                .setMessage("제목: " + track.trackName +
                        "\n아티스트: " + track.artistName +
                        "\n앨범: " + track.albumName +
                        "\n발매일: " + track.releaseDate.substring(0, 10))
                .setPositiveButton("닫기", null)
                .show();
    }


    private void showArtistDetails(Artist artist){
        StringBuilder genres = new StringBuilder();
        genres.append("[");
        if (artist.genres.size() == 0)
            genres.append("]");
        else
            for (int i=0; i<artist.genres.size(); i++) {
                genres.append(artist.genres.get(i));
                if (i < artist.genres.size() - 1)
                    genres.append(", ");
                else
                    genres.append("]");
            }
        DecimalFormat formatter = new DecimalFormat("#,###");
        String followers = formatter.format(artist.followers);

        new AlertDialog.Builder(getContext())
                .setTitle("세부사항")
                .setMessage("아티스트: " + artist.artistName +
                        "\n장르: " + genres.toString() +
                        "\nfollowers: " + followers)
                .setPositiveButton("닫기", null)
                .show();
    }

    //addButton click시 db에 노래 저장
    private void addFavoriteSong(Track track){
        new AlertDialog.Builder(getContext())
                .setTitle("관심목록에 추가")
                .setMessage(track.trackName + " - " + track.artistName + " 을 Favorites List 에 추가할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        new Thread(() -> {
                            try {
                                favoritesViewModel.insert(track, today);
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                });
                            } catch (SQLiteConstraintException e) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }).start();

                    }
                })
        .show();
    }

    private void showOptionDialog(){
        String[] options = {"노래 검색", "아티스트 검색"};
        new AlertDialog.Builder(requireContext())
                .setTitle("검색 모드")
                .setSingleChoiceItems(options, searchViewModel.selectedOption, (dialog, which) -> {
                    searchViewModel.selectedOption = which;
                })
                .setPositiveButton("확인", (dialog, which) -> {
                    String selectedText = options[searchViewModel.selectedOption];
                    if (searchViewModel.selectedOption == 0)
                        searchEditText.setHint("노래 제목을 입력하세요");
                    else
                        searchEditText.setHint("아티스트 이름을 입력하세요");
                })
                .setNegativeButton("취소", null)
                .show();
    }


}

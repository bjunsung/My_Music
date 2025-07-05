package com.example.mymusic.ui.search;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.data.local.Token;
import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.data.repository.TokenRepository;
import com.example.mymusic.model.Artist;
import com.example.mymusic.adapter.ArtistAdapter;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.TokenHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
    private FavoriteArtistViewModel favoriteArtistViewModel;
    private static final int MAX_RETRY_REFRESH_TOKEN_COUNT = 3;
    private static int retryRefreshTokenCount;
    private TrackAdapter trackAdapter;


    //ViewModel 연결, repository 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);

        tokenRepository = new TokenRepository(requireContext().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        retryRefreshTokenCount = 0;
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchEditText = view.findViewById(R.id.searchEditText);

        recyclerView = view.findViewById(R.id.result_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //TrackViewModel 에 리스트 있으면 그거 가져옴
        //track search mode 일 때 music info 로 bundle, shared view 넘김
        if (searchViewModel.selectedOption == 0 && !searchViewModel.searchTrackResults.isEmpty()) {
            trackAdapter= new TrackAdapter(searchViewModel.searchTrackResults,
                    getContext(),
                    this::showTrackDetails,
                    this::addFavoriteSong,
                    this::onTrackClick);

            recyclerView.setAdapter(trackAdapter);
        } else if (searchViewModel.selectedOption == 1 && !searchViewModel.searchArtistResults.isEmpty()) {
            ArtistAdapter adapter = new ArtistAdapter(searchViewModel.searchArtistResults, getContext(), this::showArtistDetails, this::addFavoriteArtist);
            recyclerView.setAdapter(adapter);
        }
        getTokenAndEnableSearch();
    }

    //토큰 재발급
    private void refreshToken(Consumer<String> callback){
        retryRefreshTokenCount++;
        TokenHelper.refreshTokenWithUI(requireContext(), this, refreshed -> {
            if (refreshed != null){
                callback.accept(refreshed);
            }
            else{
                Log.e("SearchFragment", "토큰 get 실패");
                callback.accept(null);
            }
                },
                error -> Log.e("SearchFragment", "토큰 실패: " + error));
    }

    private void getTokenAndEnableSearch() {
        new Thread(() -> {
            Token token = tokenRepository.getAccessTokenSync();

            if (token != null && token.getAccessToken() != null) {
                accessToken = token.getAccessToken();
                requireActivity().runOnUiThread(this::setupSearchUI);
            } else {
                refreshToken(refreshed -> {
                    accessToken = refreshed;
                    requireActivity().runOnUiThread(this::setupSearchUI);
                });
            }
        }).start();
    }


    private void keyboardUp(){
        searchEditText.requestFocus();
        searchEditText.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    private void lockAndSearchKeyword(){
        //hide keyboard
        hideKeyboard();
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

                SettingRepository settingRepository = new SettingRepository(requireContext());
                int limit;
                if (searchType.equals("track"))
                    limit = settingRepository.getMaxSearchedTracks();
                else limit = settingRepository.getMaxSearchedArtists();
                String finalLimit = String.valueOf(limit);
                String urlStr = "https://api.spotify.com/v1/search?q=" + encodedKeyword + "&type=" + searchType + "&market=KR&limit=" + finalLimit;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){ //401 accessToken expired
                    if(retryRefreshTokenCount >= MAX_RETRY_REFRESH_TOKEN_COUNT){
                        retryRefreshTokenCount = 0;
                        requireActivity().runOnUiThread(() -> {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("ERROR")
                                    .setMessage("만료된 토큰입니다.")
                                    .setPositiveButton("확인", null)
                                    .show();
                        });
                        return;
                    }else {
                        refreshToken(refreshed -> {
                            if (refreshed != null){
                                search(keyword, refreshed, searchTypeInt);
                            }
                        });
                        return;
                    }
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
                        TrackAdapter adapter = new TrackAdapter(tracks,
                                getContext(),
                                this::showTrackDetails,
                                this::addFavoriteSong,
                                this::onTrackClick);
                        RecyclerView recyclerView = requireView().findViewById(R.id.result_recycler_view);
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
                        ArtistAdapter adapter = new ArtistAdapter(artists, getContext(), this::showArtistDetails, this::addFavoriteArtist);
                        RecyclerView recyclerView = requireView().findViewById(R.id.result_recycler_view);
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
        DecimalFormat formatter = new DecimalFormat("#,###");
        String followers = formatter.format(artist.followers);

        new AlertDialog.Builder(getContext())
                .setTitle("세부사항")
                .setMessage("아티스트: " + artist.artistName +
                        "\n장르: " + artist.getJoinedGenres() +
                        "\nfollowers: " + followers)
                .setPositiveButton("닫기", null)
                .show();
    }

    //addButton click시 db에 노래 저장
    private void addFavoriteSong(Track track){
        new AlertDialog.Builder(getContext())
                .setTitle("관심목록에 추가")
                .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", (dialog, which) -> favoritesViewModel.loadFavoriteItem(track.trackId, duplicates -> {
                    if (duplicates == null){
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
                    else{
                        Toast.makeText(getContext(), track.trackName + " - " + track.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
                    }
                }))
        .show();
    }


    private void addFavoriteArtist(Artist artist){
        new AlertDialog.Builder(getContext())
                .setTitle("관심목록에 추가")
                .setMessage(artist.artistName  + " 을(를) Favorites List 에 추가할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        new Thread(() -> {
                            try {
                                favoriteArtistViewModel.insert(artist, today);
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), artist.artistName + " 이(가) Favorites List에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                });
                            } catch (SQLiteConstraintException e) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), artist.artistName + " 이(가) 이미 Favorites List에 있습니다.", Toast.LENGTH_SHORT).show();
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

    public void onTrackClick(Track track, ImageView sharedImageView, int position){
        Bundle bundle = new Bundle();
        Favorite favorite = new Favorite(track);
        bundle.putParcelable("favorite", favorite);
        String transitionName = "Transition_search_to_music" + track.artworkUrl + track.trackId;
        bundle.putString("transitionName", transitionName);

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(sharedImageView, transitionName)
                .build();


        NavController navController = NavHostFragment.findNavController(this);
        NavDestination currentDestination = navController.getCurrentDestination();
        assert currentDestination != null;
        if (currentDestination.getId() == R.id.navigation_searches)
            navController.navigate(R.id.musicInfoFragment, bundle, null, extras);

    }

}

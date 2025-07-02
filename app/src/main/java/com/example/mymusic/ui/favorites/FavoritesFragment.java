package com.example.mymusic.ui.favorites;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.adapter.FavoriteArtistAdapter;
import com.example.mymusic.adapter.FavoritesAdapter;
import com.example.mymusic.data.repository.SettingRepository;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.ArtistMetadata;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;
import com.example.mymusic.network.ArtistMetadataService;
import com.example.mymusic.network.ArtistVibeLinkService;
import com.example.mymusic.network.LyricsSearchService;

import java.util.ArrayList;
import java.util.List;

import android.widget.LinearLayout;


import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class FavoritesFragment extends Fragment {
    private final String TAG = "FavoriteFragment";
    private RecyclerView recyclerView;
    private FavoritesViewModel favoritesViewModel;
    private FavoriteArtistViewModel favoriteArtistViewModel;
    FavoritesAdapter favoriteTrackAdapter;
    FavoriteArtistAdapter favoriteArtistAdapter;
    TextView emptyFavoriteSongTextView, emptyFavoriteArtistTextView, favoritesLoadedCountTextView;
    public int favoriteOption = 0; // 기본값: track
    private TextView elementCountTextView;
    private List<String> selectedArtistIds = new ArrayList<>();
    private ImageButton filterButton;

    private WebView webView, webView2;
    private String focusedTrackId;
    TextView lyricsTextView, onLyricsTitleTextView, getOnLyricsArtistTextView;
    ScrollView scrollAreaView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoritesViewModel = new ViewModelProvider(requireActivity()).get(FavoritesViewModel.class);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        elementCountTextView = view.findViewById(R.id.element_count);
        elementCountTextView.setText("Songs");

        lyricsTextView = view.findViewById(R.id.metadata_lyrics);
        onLyricsTitleTextView = view.findViewById(R.id.on_lyrics_title);
        getOnLyricsArtistTextView = view.findViewById(R.id.on_lyrics_artist);
        scrollAreaView = view.findViewById(R.id.scroll_area);
        if (scrollAreaView != null)
            OverScrollDecoratorHelper.setUpOverScroll(scrollAreaView);

        //switch toggle event
        SwitchCompat favoriteOptionSwitch = view.findViewById(R.id.favorite_option_switch);
        favoriteOptionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked)
                favoriteOption = 0; //track
            else {
                favoriteOption = 1; //artist
                toggleLyricsVisibility(false);
            }
            loadFavoritesAndUpdateUI();
        });


        recyclerView = view.findViewById(R.id.result_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        emptyFavoriteSongTextView = view.findViewById(R.id.empty_favorite_song);
        emptyFavoriteArtistTextView = view.findViewById(R.id.empty_favorite_artist);
        favoritesLoadedCountTextView = view.findViewById(R.id.favorites_loaded_count);
        webView = view.findViewById(R.id.hidden_web_view);
        webView2 = view.findViewById(R.id.hidden_web_view_2);

        if(favoriteOption == 0) {//track
            emptyFavoriteArtistTextView.setVisibility(View.GONE);
            favoritesViewModel.loadAllFavorites(favoritesList -> {
                favoriteTrackAdapter = new FavoritesAdapter(favoritesList, this::deleteFavoriteSong, this::onLyricClick, this::onLyricLongClick);
                recyclerView.setAdapter(favoriteTrackAdapter);
                if (favoritesList.isEmpty()) {
                    emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyFavoriteSongTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    favoriteTrackAdapter.updateData(favoritesList);
                }
                favoritesLoadedCountTextView.setText(String.valueOf(favoritesList.size()));
            });
        }
        else if(favoriteOption == 1) {//artist
            emptyFavoriteSongTextView.setVisibility(View.GONE);
            favoriteArtistViewModel.loadFavorites(favoriteArtistList -> {
                favoriteArtistAdapter = new FavoriteArtistAdapter(favoriteArtistList, this::deleteFavoriteArtist, this::addArtistMetadata, favoriteArtistViewModel);
                recyclerView.setAdapter(favoriteArtistAdapter);
                if (favoriteArtistList.isEmpty()) {
                    emptyFavoriteArtistTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyFavoriteArtistTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    favoriteArtistAdapter.updateData(favoriteArtistList);
                }
                favoritesLoadedCountTextView.setText(String.valueOf(favoriteArtistList.size()));

            });
        }





    }

    // 화면 업데이트 함수
    private void loadFavoritesAndUpdateUI() {
        if (favoriteOption == 0) {
            favoritesViewModel.loadAllFavorites(favoritesList -> {
                favoriteTrackAdapter = new FavoritesAdapter(favoritesList, this::deleteFavoriteSong, this::onLyricClick, this::onLyricLongClick);
                recyclerView.setAdapter(favoriteTrackAdapter);
                updateEmptyState(favoritesList.isEmpty());
                favoriteTrackAdapter.updateData(favoritesList);
                favoritesLoadedCountTextView.setText(String.valueOf(favoritesList.size()));
                elementCountTextView.setText("Songs");
            });
        } else {
            favoriteArtistViewModel.loadFavorites(favoriteArtistList -> {
                favoriteArtistAdapter = new FavoriteArtistAdapter(favoriteArtistList, this::deleteFavoriteArtist, this::addArtistMetadata, favoriteArtistViewModel);
                recyclerView.setAdapter(favoriteArtistAdapter);
                updateEmptyState(favoriteArtistList.isEmpty());
                favoriteArtistAdapter.updateData(favoriteArtistList);
                favoritesLoadedCountTextView.setText(String.valueOf(favoriteArtistList.size()));
                elementCountTextView.setText("Artists");
            });
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        // 먼저 모두 GONE 처리해서 겹침 방지
        emptyFavoriteSongTextView.setVisibility(View.GONE);
        emptyFavoriteArtistTextView.setVisibility(View.GONE);

        if (isEmpty) {
            if (favoriteOption == 0) { //favorite song
                emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
            } else { //favorite artist
                emptyFavoriteArtistTextView.setVisibility(View.VISIBLE);
            }
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }




    void deleteFavoriteSong(Favorite favorite){
        Track track = favorite.track;
        String trackName = track.trackName;
        if (favorite.metadata != null && favorite.metadata.title != null){
            trackName = favorite.metadata.title;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("삭제")
                .setMessage("정말 " + trackName + " - " + track.artistName + " 을(를) 삭제하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        favoritesViewModel.deleteFavoriteSong(track);
                        Toast.makeText(getContext(),
                                track.trackName + " - " + track.artistName + " 이(가) Favorites List 에서 삭제되었습니다.",
                                Toast.LENGTH_SHORT).show();
                        favoritesViewModel.loadAllFavorites(updatedList -> {
                            if (updatedList.isEmpty()) {
                                emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                emptyFavoriteSongTextView.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                            favoriteTrackAdapter.updateData(updatedList); // RecyclerView 새로고침
                            updateEmptyState(updatedList.isEmpty());
                            favoritesLoadedCountTextView.setText(String.valueOf(updatedList.size()));
                        });
                    }
                })
                .show();
    }


    public void deleteFavoriteArtist(Artist artist) {
        // --- AlertDialog에 삽입할 커스텀 뷰를 Java 코드로 직접 생성 시작 ---
        // 최상위 레이아웃 (수직 방향)
        LinearLayout customLayout = new LinearLayout(getContext());
        customLayout.setOrientation(LinearLayout.VERTICAL);

        // 패딩 설정 (dp 값을 픽셀로 변환)
        float density = getContext().getResources().getDisplayMetrics().density;
        int paddingHorizontalPx = (int) (24 * density); // 좌우 24dp
        int paddingVerticalPx = (int) (12 * density);   // 상하 12dp
        customLayout.setPadding(paddingHorizontalPx, paddingVerticalPx, paddingHorizontalPx, paddingVerticalPx);

        // 메시지 TextView 생성
        TextView messageTextView = new TextView(getContext());
        messageTextView.setText("정말 " + artist.artistName + " 을(를) 삭제하시겠습니까?");
        // 텍스트 색상 설정 (예시: 기본 시스템 텍스트 색상)
        messageTextView.setTextColor(getContext().getResources().getColor(android.R.color.tab_indicator_text, getContext().getTheme()));
        messageTextView.setTextSize(16); // 텍스트 크기 16sp
        // 메시지 TextView의 하단 패딩 (16dp)
        int messageBottomPaddingPx = (int) (16 * density);
        messageTextView.setPadding(0, 0, 0, messageBottomPaddingPx);

        // 체크박스 생성
        CheckBox deleteMetadataCheckBox = new CheckBox(getContext());
        deleteMetadataCheckBox.setText("metadata와 함께 삭제");

        // 체크박스를 오른쪽에 정렬하기 위한 LayoutParams 설정
        LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        checkBoxParams.gravity = android.view.Gravity.END; // 오른쪽 정렬
        deleteMetadataCheckBox.setLayoutParams(checkBoxParams);

        // 생성된 뷰들을 레이아웃에 추가
        customLayout.addView(messageTextView);
        customLayout.addView(deleteMetadataCheckBox);
        // --- AlertDialog에 삽입할 커스텀 뷰 생성 끝 ---


        // AlertDialog 빌더를 사용하여 다이얼로그 생성
        new AlertDialog.Builder(getContext())
                .setTitle("삭제")
                .setView(customLayout) // Java 코드로 만든 커스텀 뷰를 다이얼로그에 설정
                .setNegativeButton("취소", null) // 취소 버튼
                .setPositiveButton("확인", (dialog, which) -> {
                    // 사용자가 체크박스를 체크했는지 확인
                    boolean shouldDeleteMetadata = deleteMetadataCheckBox.isChecked();

                    if (shouldDeleteMetadata) {
                        Log.d(TAG, "delete artist with metadata");
                        // "metadata와 함께 삭제"가 체크된 경우의 로직
                        // ViewModel에 해당 아티스트의 메타데이터도 함께 삭제하는 메서드를 호출 (이 메서드는 ViewModel에 새로 구현해야 함)
                        favoriteArtistViewModel.deleteArtistMetadataBySpotifyId(artist.artistId, message -> {
                            if (message.contains("Success")){
                                Log.d(TAG, "delete metadata Success");
                                Toast.makeText(getContext(),
                                        artist.artistName + " 및 메타데이터가 삭제되었습니다",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Log.d(TAG, "delete metadata Fail");
                                Toast.makeText(getContext(),
                                        artist.artistName + "의 메타데이터 삭제 실패",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                    // 체크되지 않은 경우 (기존 로직: 즐겨찾기 목록에서만 삭제)
                    favoriteArtistViewModel.deleteFavoriteArtist(artist);
                    Toast.makeText(getContext(),
                            artist.artistName + " 이(가) Favorites List 에서 삭제되었습니다.",
                            Toast.LENGTH_SHORT).show();


                    // 삭제 후 즐겨찾기 목록 새로고침 (공통 로직)
                    favoriteArtistViewModel.loadFavorites(updatedList -> {
                        // 목록 상태에 따라 UI 업데이트 (empty/visible)
                        if (updatedList.isEmpty()) {
                            emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyFavoriteSongTextView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                        favoriteArtistAdapter.updateData(updatedList); // RecyclerView 데이터 업데이트
                        // updateEmptyState(updatedList.isEmpty()); // 이 메서드가 별도로 있다면 호출
                        favoritesLoadedCountTextView.setText(String.valueOf(updatedList.size())); // 개수 업데이트
                    });
                })
                .show(); // 다이얼로그 표시
    }



    private void onLyricLongClick(String trackIdDb, String trackName){
        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null){
                addLyric(trackIdDb, trackName, true);
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("가사정보 없음")
                        .setMessage("링크를 입력해 가사를 추가하세요")
                        .setPositiveButton("닫기", null)
                        .show();
            }
        });

    }
    private void onLyricClick(String trackIdDb, String trackName){

        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null && !favorite.metadata.lyrics.isEmpty()){
                showLyricsByScreenMode(favorite);
            } else {
                addLyric(trackIdDb, trackName, false);
            }
        });
    }

    private void showLyricsByScreenMode(Favorite favorite){
        // 가로모드
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //가사 보이는 상태에서 클릭시 가리기
            if(scrollAreaView.getVisibility() == View.VISIBLE && favorite.track.trackId.equals(focusedTrackId)){
                toggleLyricsVisibility(false);
            }
            else {
                lyricsTextView.setText(favorite.metadata.lyrics);
                lyricsTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
                if (favorite.metadata != null && favorite.metadata.title != null && !favorite.metadata.title.isEmpty()) {
                    onLyricsTitleTextView.setText(favorite.metadata.title);
                }
                else{
                    onLyricsTitleTextView.setText(favorite.track.trackName);
                }
                getOnLyricsArtistTextView.setText(favorite.track.artistName);
                toggleLyricsVisibility(true);
                focusedTrackId = favorite.track.trackId;
            }

        }     // 세로모드
        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            new AlertDialog.Builder(requireContext())
                    .setTitle("상세정보")
                    .setMessage(favorite.metadata.lyrics)
                    .setPositiveButton("닫기", null)
                    .show();
        }
    }

    private void addArtistMetadata(String artistId){
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);


        final EditText input = new EditText(getContext());
        input.setHint("https://vibe.naver.com/artist/# (#에 해당하는 숫자만 입력해도 괜찮습니다)");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        int originalInputType = input.getInputType();

        //NumberPad 세팅값 load
        SettingRepository settingRepository = new SettingRepository(requireContext());
        new Thread(() -> {
            boolean numericPadMode = settingRepository.getNumericPreference();
            if (numericPadMode){
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            else{
                input.setInputType(originalInputType);
            }

        }).start();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);

        layout.addView(input);

        AlertDialog dialog1 = new AlertDialog.Builder(getContext())
                .setTitle("Artist Metadata 추가")
                .setView(layout)
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", ((dialog, which) -> {
                    ArtistMetadataService.fetchMetadata(webView, input.getText().toString().trim() , new ArtistMetadataService.MetadataCallback() {
                        @Override
                        public void onSuccess(ArtistMetadata metadata) {
                            if (getActivity() != null){
                                getActivity().runOnUiThread(() -> {
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("아래 정보를 저장하시겠습니까?")
                                            .setMessage(metadata.toString())
                                            .setPositiveButton("저장", (dialog2, which2) -> {
                                                //ArtistMetadata 수정
                                                metadata.spotifyArtistId = artistId;
                                                metadata.vibeArtistId = input.getText().toString().trim();

                                                if (metadata.images != null && !metadata.images.isEmpty()) {
                                                    List<String> cleanedImages = new ArrayList<>();
                                                    for (String url : metadata.images) {
                                                        if (url == null) {
                                                            cleanedImages.add(null); // null 방어
                                                            continue;
                                                        }
                                                        int idx = url.indexOf("?type=");
                                                        if (idx != -1) {
                                                            cleanedImages.add(url.substring(0, idx)); // 자름
                                                        } else {
                                                            cleanedImages.add(url); // 그대로 유지
                                                        }
                                                    }
                                                    metadata.images = cleanedImages; // 원래 리스트 교체
                                                }

                                                if (metadata.members != null && !metadata.members.isEmpty()) {
                                                    for (int i = 0 ; i < metadata.members.size(); i++){
                                                        List<String> pair = metadata.members.get(i);
                                                        if (pair.get(1) == null || pair.get(1).isEmpty())
                                                            continue;
                                                        int id = Integer.parseInt(pair.get(1));
                                                        String padded = String.format("%06d", id); // 최소 6자리 패딩
                                                        String key = String.format("%06d", id / 1000);
                                                        String folder1 = key.substring(0, 3);    // 앞 3자리
                                                        String folder2 = key.substring(3, 6);    // 뒤 3자리
                                                        String link = "https://musicmeta-phinf.pstatic.net/artist/" + folder1 + "/" + folder2 + "/" + id + ".jpg";
                                                        metadata.members.get(i).set(2, link);
                                                    }
                                                }

                                                Log.d("FavoritesFragment", "수정된 metadata : " + metadata.toString());
                                                favoriteArtistViewModel.addArtistMetadata(metadata, new FavoriteArtistViewModel.addMetadataCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
                                                    }

                                                    @Override
                                                    public void onFailure(ArtistMetadata metadata, String reason) {
                                                        if (reason.contains("Spotify")) {
                                                            requireActivity().runOnUiThread(() -> {
                                                                new AlertDialog.Builder(getContext())
                                                                        .setTitle("Artist 중복")
                                                                        .setMessage("Metadata 정보가 이미 있습니다.\n삭제 후 다시 시도하세요.")
                                                                        .setNegativeButton("확인", null)
                                                                        .show();

                                                            });
                                                        }
                                                        else if (reason.contains("Vibe")){
                                                            favoriteArtistViewModel.getArtistNameById(metadata.spotifyArtistId, artistName -> {
                                                                requireActivity().runOnUiThread(() -> {
                                                                    new AlertDialog.Builder(getContext())
                                                                            .setTitle("Metadata 중복")
                                                                            .setMessage("Metadata 정보가 " + artistName +  " 에 연결되어있습니다.\n삭제 후 다시 시도하세요.")
                                                                            .setNegativeButton("확인", null)
                                                                            .show();
                                                                });
                                                            });
                                                        }
                                                    }
                                                });

                                            })
                                            .setNegativeButton("닫기", null)
                                            .show();
                                });
                            }
                        }

                        @Override
                        public void onFailure(String reason) {

                        }
                    });
                }))
                .create();




        dialog1.show();

    }

    private void addLyric(String trackIdDb, String trackName, boolean editMode){
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);


        final EditText input = new EditText(getContext());
        input.setHint("https://vibe.naver.com/track/# (#에 해당하는 숫자만 입력해도 괜찮습니다)");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final EditText input_lyrics = new EditText(getContext());
        input_lyrics.setHint("가사 입력");
        input_lyrics.setInputType(InputType.TYPE_CLASS_TEXT);
        //input_lyrics.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final EditText input_titleKr = new EditText(getContext());
        input_titleKr.setHint("한국어 제목을 입력하세요.");
        input_lyrics.setInputType(InputType.TYPE_CLASS_TEXT);

        int originalInputType = input.getInputType();


        //NumberPad 세팅값 load
        SettingRepository settingRepository = new SettingRepository(requireContext());
        new Thread(() -> {
            boolean numericPadMode = settingRepository.getNumericPreference();
            if (numericPadMode){
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            else{
                input.setInputType(originalInputType);
            }

        }).start();


        TextView message = new TextView(getContext());
        message.setText("또는 '편집' 버튼을 눌러 직접 수정");
        message.setPadding(6, 30, 0, 0);
        message.setTextSize(16);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);
        input_lyrics.setLayoutParams(params);


        layout.addView(input);
        layout.addView(message);


        LinearLayout layout_directly = new LinearLayout(getContext());
        layout_directly.setOrientation(LinearLayout.VERTICAL);
        layout_directly.setPadding(50, 40, 50, 10);

        layout_directly.addView(input_lyrics);
        layout_directly.addView(input_titleKr);

        String dialogTitle;
        if (editMode){
            dialogTitle = "가사 편집";
        }
        else{
            dialogTitle = "가사 정보 등록";
        }


        AlertDialog dialog1 = new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(layout)
                // AlertDialog의 setPositiveButton 부분을 아래 코드로 완전히 교체하세요.

                .setPositiveButton("확인", (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {
                        Toast.makeText(getContext(), "VIBE 트랙 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long startFetchLyricsTime = System.currentTimeMillis();

                    String userInput = input.getText().toString().trim();
                    String trackIdNaverVibe = extractTrackId(userInput);
// 사용자 입력을 받을 첫 번째 다이얼로그
// 로딩 다이얼로그를 담을 배열 (final로 만들어 콜백에서 접근 가능하게 함)
                    final AlertDialog[] loadingDialog = new AlertDialog[1];
                    loadingDialog[0] = new AlertDialog.Builder(getContext())
                            .setTitle("전체 정보 로딩 중...")
                            .setMessage("노래 정보와 아티스트 링크를 모두 가져오고 있습니다.")
                            .setCancelable(false)
                            .create();
                    loadingDialog[0].show();

                    // --- 1단계: 노래 정보 가져오기 시작 ---
                    LyricsSearchService.fetchMetadata(webView, trackIdNaverVibe, new LyricsSearchService.MetadataCallback() {
                        @Override
                        public void onSuccess(TrackMetadata metadata) {
                            if (metadata.lyrics == null || metadata.getLyrics().trim().isEmpty()) {
                                Log.d("FavoritesFragment", "Fail to fetch lyrics");
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    if(loadingDialog[0] != null) loadingDialog[0].dismiss();
                                    Toast.makeText(getContext(), "가사 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }
                            Log.d("FavoritesFragment", "Success to fetch lyrics");
                            metadata.vibeTrackId = trackIdNaverVibe;

                            // --- 2단계: 아티스트 링크 가져오기 시작 ---
                            if (getActivity() == null) return;
                            if (metadata.artistLink != null && !metadata.artistLink.isEmpty() && metadata.vocalists != null && !metadata.vocalists.isEmpty()) {
                                getActivity().runOnUiThread(() -> {

                                    // artistLink가 전체 주소일 경우와 상대 경로일 경우를 모두 처리
                                    String finalArtistPageUrl;
                                    if (metadata.artistLink != null && metadata.artistLink.startsWith("http")) {
                                        finalArtistPageUrl = metadata.artistLink;
                                    } else {
                                        finalArtistPageUrl = "https://vibe.naver.com" + metadata.artistLink;
                                    }

                                    long startTime = System.currentTimeMillis();
                                    // [수정] 디버깅 코드를 삭제하고, 원래 로직의 주석을 해제합니다.
                                    Log.d("FavoritesFragment", "start to fetch members ids");
                                    ArtistVibeLinkService.fetchAllLinks(
                                            webView2, // 아티스트 링크 탐색에 사용할 두 번째 WebView
                                            metadata.vocalists,
                                            finalArtistPageUrl, // 방금 만든 올바른 URL 사용
                                            new ArtistVibeLinkService.ArtistLinksCallback() {
                                                @Override
                                                public void onComplete(List<List<String>> updatedVocalistList) {
                                                    long linkFetchedTime = System.currentTimeMillis();
                                                    // --- 최종 성공: 모든 정보가 준비됨 ---
                                                    if (getActivity() == null) return;
                                                    getActivity().runOnUiThread(() -> {
                                                        if (loadingDialog[0] != null)
                                                            loadingDialog[0].dismiss();
                                                        metadata.setVocalists(updatedVocalistList);

                                                        long updatedTime = System.currentTimeMillis();
                                                        Log.d("FavoritesFragment", "Lyrics: " + (startTime - startFetchLyricsTime) + "ms 소요됨");
                                                        Log.d("FavoritesFragment", "link: " + (linkFetchedTime - startTime) + "ms 소요됨");
                                                        Log.d("FavoritesFragment", "update metadata: " + (updatedTime - linkFetchedTime) + "ms 소요됨");

                                                        new AlertDialog.Builder(requireContext())
                                                                .setTitle("아래 정보를 저장하시겠습니까?")
                                                                .setMessage(metadata.toString())
                                                                .setPositiveButton("저장", (dialog2, which2) -> {
                                                                    if (metadata.title != null && metadata.title.equals(trackName)) {
                                                                        metadata.title = null;
                                                                    }
                                                                    favoritesViewModel.updateMetadata(trackIdDb, metadata, updated -> {
                                                                        if (updated > 0) {
                                                                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
                                                                            loadFavoritesAndUpdateUI();
                                                                        } else {
                                                                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                                        }
                                                                    });
                                                                })
                                                                .setNegativeButton("닫기", null)
                                                                .show();
                                                    });
                                                }

                                                @Override
                                                public void onError(String reason) {
                                                    // 2단계 실패 시 처리
                                                    if (getActivity() == null) return;
                                                    getActivity().runOnUiThread(() -> {
                                                        if (loadingDialog[0] != null)
                                                            loadingDialog[0].dismiss();
                                                        Toast.makeText(getContext(), "아티스트 링크 업데이트 실패: " + reason, Toast.LENGTH_SHORT).show();
                                                    });
                                                }
                                            }
                                    );
                                });
                            }
                            else{ // vocalist 정보 없는 경우
                                if (loadingDialog[0] != null)
                                    loadingDialog[0].dismiss();
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("아래 정보를 저장하시겠습니까?")
                                        .setMessage(metadata.toString())
                                        .setPositiveButton("저장", (dialog, which) -> {
                                            if (metadata.title != null && metadata.title.equals(trackName)) {
                                                metadata.title = null;
                                            }
                                            favoritesViewModel.updateMetadata(trackIdDb, metadata, updated -> {
                                                if (updated > 0) {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());
                                                    loadFavoritesAndUpdateUI();
                                                } else {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                }
                                            });
                                        })
                                        .setNegativeButton("닫기", null)
                                        .show();
                            }
                        }

                        @Override
                        public void onFailure(String reason) {
                            // 1단계 실패 시 처리
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if(loadingDialog[0] != null) loadingDialog[0].dismiss();
                                if(reason.contains("JavaScript 오류")){
                                    Toast.makeText(getContext(), "JavaScript 오류: 유효하지 않은 주소입니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "ERROR: " + reason, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
// .setNeutralButton... 등 나머지 체인은 그대로 이어집니다.
                .setNeutralButton("편집", null)
                .setNegativeButton("취소", (dialog, which)  ->{})
                .show();


        dialog1.setOnShowListener(d -> {
            Button neutralBtn = dialog1.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralBtn.setOnClickListener(v -> {
                dialog1.dismiss();

                new AlertDialog.Builder(getContext())
                        .setTitle("Metadata 입력")
                        .setView(layout_directly)
                        .setPositiveButton("확인", (dialog, which) -> {
                            if (!(input_lyrics.getText().toString().isEmpty() && input_titleKr.getText().toString().isEmpty())){

                            TrackMetadata newMetadata = new TrackMetadata();
                            favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
                                        if (favorite != null) {
                                            if (!(favorite.metadata.title == null && favorite.metadata.lyrics == null && favorite.metadata.composers == null && favorite.metadata.lyricists == null)) {
                                                Log.d("metadata is not null", favorite.metadata.title + favorite.metadata.lyrics + favorite.metadata.composers + favorite.metadata.lyricists);
                                                newMetadata.vibeTrackId = favorite.metadata.vibeTrackId;
                                                newMetadata.title = favorite.metadata.title;
                                                newMetadata.lyrics = favorite.metadata.lyrics;
                                                newMetadata.lyricists = favorite.metadata.lyricists;
                                                newMetadata.composers = favorite.metadata.composers;
                                            }

                                            //사용자가 가사 직접 입력
                                            if (!input_lyrics.getText().toString().isEmpty()) {
                                                newMetadata.lyrics = input_lyrics.getText().toString();
                                            }

                                            // 사용자가 노래 제목(kr) 직접 입력
                                            if(!input_titleKr.getText().toString().isEmpty()) {
                                                newMetadata.title = input_titleKr.getText().toString();

                                            }

                                            favoritesViewModel.updateMetadata(trackIdDb, newMetadata, updated -> {
                                                if (updated > 0) {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());

                                                } else {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                }
                                            });
                                        }
                                    }
                            );
                            }
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        });


        dialog1.show();


    }




    private String extractTrackId(String input) {
        if (input.startsWith("http") && input.contains("/track/")) {
            return input.substring(input.lastIndexOf("/") + 1);
        } else {
            return input;
        }
    }


    private void toggleLyricsVisibility(boolean show) {
        try {
            View metadataLayout = requireView().findViewById(R.id.metadata_layout);

            if (show) {
                Animation slideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_bottom);
                metadataLayout.setVisibility(View.VISIBLE);
                scrollAreaView.setVisibility(View.VISIBLE);
                metadataLayout.startAnimation(slideIn);
                scrollAreaView.startAnimation(slideIn);
            } else {
                Animation slideOut = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_bottom);
                metadataLayout.startAnimation(slideOut);
                scrollAreaView.startAnimation(slideOut);

                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        metadataLayout.setVisibility(View.GONE);
                        scrollAreaView.setVisibility(View.GONE);
                    }
                });
            }
        }catch (NullPointerException e){}
    }




}

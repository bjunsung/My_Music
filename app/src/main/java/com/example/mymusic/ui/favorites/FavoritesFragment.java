package com.example.mymusic.ui.favorites;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.adapter.FavoriteArtistAdapter;
import com.example.mymusic.adapter.FavoritesAdapter;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;
import com.example.mymusic.model.TrackMetadata;
import com.example.mymusic.network.LyricsSearchService;

import java.util.ArrayList;
import java.util.List;

import android.widget.LinearLayout;



public class FavoritesFragment extends Fragment {
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

    private WebView webView;
    private String focusedTrackId;
    TextView lyricsTextView;


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
    public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        elementCountTextView = view.findViewById(R.id.element_count);
        elementCountTextView.setText("Songs");

        lyricsTextView = getView().findViewById(R.id.metadata_lyrics);

        //switch toggle event
        SwitchCompat favoriteOptionSwitch = view.findViewById(R.id.favorite_option_switch);
        favoriteOptionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked)
                favoriteOption = 0; //track
            else {
                favoriteOption = 1; //artist
                lyricsTextView.setVisibility(TextView.INVISIBLE);
            }
            loadFavoritesAndUpdateUI();
        });


        recyclerView = view.findViewById(R.id.result_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        emptyFavoriteSongTextView = view.findViewById(R.id.empty_favorite_song);
        emptyFavoriteArtistTextView = view.findViewById(R.id.empty_favorite_artist);
        favoritesLoadedCountTextView = view.findViewById(R.id.favorites_loaded_count);
        webView = view.findViewById(R.id.hidden_web_view);

        if(favoriteOption == 0) {//track
            emptyFavoriteArtistTextView.setVisibility(View.GONE);
            favoritesViewModel.loadFavorites(favoritesList -> {
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
                favoriteArtistAdapter = new FavoriteArtistAdapter(favoriteArtistList, this::deleteFavoriteArtist, favoriteArtistViewModel);
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
            favoritesViewModel.loadFavorites(favoritesList -> {
                favoriteTrackAdapter = new FavoritesAdapter(favoritesList, this::deleteFavoriteSong, this::onLyricClick, this::onLyricLongClick);
                recyclerView.setAdapter(favoriteTrackAdapter);
                updateEmptyState(favoritesList.isEmpty());
                favoriteTrackAdapter.updateData(favoritesList);
                favoritesLoadedCountTextView.setText(String.valueOf(favoritesList.size()));
                elementCountTextView.setText("Songs");
            });
        } else {
            favoriteArtistViewModel.loadFavorites(favoriteArtistList -> {
                favoriteArtistAdapter = new FavoriteArtistAdapter(favoriteArtistList, this::deleteFavoriteArtist, favoriteArtistViewModel);
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




    void deleteFavoriteSong(Track track){
        new AlertDialog.Builder(getContext())
                .setTitle("삭제")
                .setMessage("정말 " + track.trackName + " - " + track.artistName + " 을(를) 삭제하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        favoritesViewModel.deleteFavoriteSong(track);
                        Toast.makeText(getContext(),
                                track.trackName + " - " + track.artistName + " 이(가) Favorites List 에서 삭제되었습니다.",
                                Toast.LENGTH_SHORT).show();
                        favoritesViewModel.loadFavorites(updatedList -> {
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


    void deleteFavoriteArtist(Artist artist){
        new AlertDialog.Builder(getContext())
                .setTitle("삭제")
                .setMessage("정말 " + artist.artistName + " 을(를) 삭제하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        favoriteArtistViewModel.deleteFavoriteArtist(artist);
                        Toast.makeText(getContext(),
                                artist.artistName + " 이(가) Favorites List 에서 삭제되었습니다.",
                                Toast.LENGTH_SHORT).show();
                        favoriteArtistViewModel.loadFavorites(updatedList -> {
                            if (updatedList.isEmpty()) {
                                emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                emptyFavoriteSongTextView.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                            favoriteArtistAdapter.updateData(updatedList); // RecyclerView 새로고침
                            updateEmptyState(updatedList.isEmpty());
                            favoritesLoadedCountTextView.setText(String.valueOf(updatedList.size()));
                        });
                    }
                })
                .show();
    }



    private void onLyricLongClick(String trackIdDb){
        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null){
                addLyric(trackIdDb, true);
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("가사정보 없음")
                        .setMessage("링크를 입력해 가사를 추가하세요")
                        .setPositiveButton("닫기", null)
                        .show();
            }
        });

    }
    private void onLyricClick(String trackIdDb){

        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
            if (favorite != null && favorite.metadata != null && favorite.metadata.lyrics != null){
                showLyricsByScreenMode(favorite);
            } else {
                addLyric(trackIdDb, false);
            }
        });
    }

    private void showLyricsByScreenMode(Favorite favorite){
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 가로모드
            if(lyricsTextView.getVisibility() == TextView.VISIBLE && favorite.track.trackId.equals(focusedTrackId)){
                lyricsTextView.setVisibility(TextView.INVISIBLE);
            }
            else {
                lyricsTextView.setText(favorite.metadata.lyrics);
                lyricsTextView.setVisibility(TextView.VISIBLE);
                focusedTrackId = favorite.track.trackId;
            }

        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 세로모드
            new AlertDialog.Builder(requireContext())
                    .setTitle("상세정보")
                    .setMessage(favorite.metadata.lyrics)
                    .setPositiveButton("닫기", null)
                    .show();
        }
    }
    private void addLyric(String trackIdDb, boolean editMode){
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(getContext());
        input.setHint("https://vibe.naver.com/track/3861527");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        final EditText input_lyrics = new EditText(getContext());
        input_lyrics.setHint("여기에 가사 직접 입력");
        input_lyrics.setInputType(InputType.TYPE_CLASS_TEXT);
        input_lyrics.setImeOptions(EditorInfo.IME_ACTION_DONE);

        TextView message = new TextView(getContext());
        message.setText("NAVER VIBE 주소를 입력 또는 직접 가사 추가");
        message.setTextSize(17);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(params);

        layout.addView(message);
        layout.addView(input);
        layout.addView(input_lyrics);

        String dialogTitle;
        if (editMode){
            dialogTitle = "가사 편집";
        }
        else{
            dialogTitle = "가사 정보 등록";
        }

        new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(layout)
                .setPositiveButton("확인", (dialog, which) -> {
                    if (!input.getText().toString().isEmpty()) {
                        String userInput = input.getText().toString().trim();
                        String trackIdNaverVibe = extractTrackId(userInput);


                        LyricsSearchService.fetchMetadata(webView, trackIdNaverVibe, new LyricsSearchService.MetadataCallback() {
                            @Override
                            public void onSuccess(TrackMetadata metadata) {
                                if (metadata.getLyrics() == null || metadata.getLyrics().trim().isEmpty())
                                    return;
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("아래 정보를 저장하시겠습니까?")
                                        .setMessage(metadata.toString())
                                        .setPositiveButton("저장", (dialog, which) -> {
                                            favoritesViewModel.addMetadata(trackIdDb, metadata, updated -> {
                                                if (updated > 0) {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show());

                                                } else {
                                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "저장되지 않았습니다.", Toast.LENGTH_SHORT).show());
                                                }
                                            });

                                        })
                                        .setNegativeButton("닫기", null)
                                        .show();
                            }

                            @Override
                            public void onFailure(String reason) {
                                Toast.makeText(getContext(), "실패: " + reason, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else{
                        Log.d("Metadata input", "metadata inuput");
                        favoritesViewModel.loadFavoriteItem(trackIdDb, favorite -> {
                                    if (favorite != null) {
                                        Log.d("favorite != null", "favorite != null");
                                        TrackMetadata newMetadata = new TrackMetadata();
                                        if (!(favorite.metadata.title == null && favorite.metadata.lyrics == null && favorite.metadata.composers == null && favorite.metadata.lyricists == null)) {
                                            Log.d("metadata is not null", favorite.metadata.title + favorite.metadata.lyrics + favorite.metadata.composers + favorite.metadata.lyricists);
                                            newMetadata.title = favorite.metadata.title;
                                            newMetadata.lyrics = favorite.metadata.lyrics;
                                            newMetadata.lyricists = favorite.metadata.lyricists;
                                            newMetadata.composers = favorite.metadata.composers;
                                        }

                                        if (!input_lyrics.getText().toString().isEmpty()) {
                                            newMetadata.lyrics = input_lyrics.getText().toString();
                                            Log.d("added new lyrics to newMetadata", "added new lyrics to newMetadata");
                                        }

                                        favoritesViewModel.addMetadata(trackIdDb, newMetadata, updated -> {
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

    }


    private String extractTrackId(String input) {
        if (input.startsWith("http") && input.contains("/track/")) {
            return input.substring(input.lastIndexOf("/") + 1);
        } else {
            return input;
        }
    }



}

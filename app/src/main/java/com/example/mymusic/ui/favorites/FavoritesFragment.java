package com.example.mymusic.ui.favorites;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {
    private List<String> selectedArtistIds = new ArrayList<>();
    private RecyclerView recyclerView;
    private ImageButton filterButton;
    private FavoritesViewModel favoritesViewModel;
    private FavoriteArtistViewModel favoriteArtistViewModel;
    FavoritesAdapter favoriteTrackAdapter;
    FavoriteArtistAdapter favoriteArtistAdapter;
    TextView emptyFavoriteSongTextView, emptyFavoriteArtistTextView, favoritesLoadedCountTextView;
    public int favoriteOption = 0; // 기본값: track

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_favotrites, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        //switch toggle event
        SwitchCompat favoriteOptionSwitch = view.findViewById(R.id.favoriteOptionSwitch);
        favoriteOptionSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (!isChecked)
                favoriteOption = 0; //track
            else
                favoriteOption = 1; //artist
            loadFavoritesAndUpdateUI();
        }));


        recyclerView = view.findViewById(R.id.resultRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        emptyFavoriteSongTextView = view.findViewById(R.id.emptyFavoriteSongTextView);
        emptyFavoriteArtistTextView = view.findViewById(R.id.emptyFavoriteArtistTextView);
        favoritesLoadedCountTextView = view.findViewById(R.id.favoritesLoadedCountTextView);

        if(favoriteOption == 0) {//track
            favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
            emptyFavoriteArtistTextView.setVisibility(View.GONE);
            favoritesViewModel.loadFavorites(favoritesList -> {
                favoriteTrackAdapter = new FavoritesAdapter(favoritesList, this::deleteFavoriteSong);
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
            favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
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

    // ✅ 화면 업데이트 함수
    private void loadFavoritesAndUpdateUI() {
        if (favoriteOption == 0) {
            favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
            favoritesViewModel.loadFavorites(favoritesList -> {
                favoriteTrackAdapter = new FavoritesAdapter(favoritesList, this::deleteFavoriteSong);
                recyclerView.setAdapter(favoriteTrackAdapter);
                updateEmptyState(favoritesList.isEmpty());
                favoriteTrackAdapter.updateData(favoritesList);
                favoritesLoadedCountTextView.setText(String.valueOf(favoritesList.size()));
            });
        } else {
            favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
            favoriteArtistViewModel.loadFavorites(favoriteArtistList -> {
                favoriteArtistAdapter = new FavoriteArtistAdapter(favoriteArtistList, this::deleteFavoriteArtist, favoriteArtistViewModel);
                recyclerView.setAdapter(favoriteArtistAdapter);
                updateEmptyState(favoriteArtistList.isEmpty());
                favoriteArtistAdapter.updateData(favoriteArtistList);
                favoritesLoadedCountTextView.setText(String.valueOf(favoriteArtistList.size()));
            });
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        // ✅ 먼저 모두 GONE 처리해서 겹침 방지
        emptyFavoriteSongTextView.setVisibility(View.GONE);
        emptyFavoriteArtistTextView.setVisibility(View.GONE);

        if (isEmpty) {
            if (favoriteOption == 0) {
                emptyFavoriteSongTextView.setVisibility(View.VISIBLE);
            } else {
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
                            favoritesLoadedCountTextView.setText(String.valueOf(updatedList.size()));
                        });
                    }
                })
                .show();
    }

}

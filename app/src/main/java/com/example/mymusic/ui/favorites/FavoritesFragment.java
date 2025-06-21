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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {
    private List<String> selectedArtistIds = new ArrayList<>();
    private RecyclerView recyclerView;
    private ImageButton filterButton;
    private FavoritesViewModel favoritesViewModel;
    FavoritesAdapter adapter;

    TextView emptyFavoritesTextView, favoritesLoadedCountTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_favotrites, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.resultRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        emptyFavoritesTextView = view.findViewById(R.id.emptyMessageTextView);
        favoritesLoadedCountTextView = view.findViewById(R.id.favoritesLoadedCountTextView);

        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);

        favoritesViewModel.loadFavorites(favoritesList -> {
            adapter = new FavoritesAdapter(favoritesList, this::deleteFavoriteSong);
            recyclerView.setAdapter(adapter);
            if (favoritesList.isEmpty()) {
                emptyFavoritesTextView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyFavoritesTextView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.updateData(favoritesList);
            }
            favoritesLoadedCountTextView.setText(String.valueOf(favoritesList.size()));
        });



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
                                emptyFavoritesTextView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                emptyFavoritesTextView.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                            adapter.updateData(updatedList); // RecyclerView 새로고침
                            favoritesLoadedCountTextView.setText(String.valueOf(updatedList.size()));
                        });
                    }
                })
                .show();
    }




}

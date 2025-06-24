package com.example.mymusic.ui.artistInfo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.adapter.SimpleAlbumAdapter;
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.data.util.NumberUtils;
import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoriteArtistViewModel;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ArtistInfoFragment extends Fragment {

    private FavoriteArtistViewModel favoriteArtistViewModel;
    private FavoritesViewModel favoritesViewModel;

    private Artist artist;
    private ImageView artistImageView;
    private TextView artistNameTextView, genresTextView, followersTextView;
    private RecyclerView albumRecyclerView, trackRecyclerView;
    private ImageButton addArtistButton;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoriteArtistViewModel = new ViewModelProvider(this).get(FavoriteArtistViewModel.class);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_artist_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        artistImageView = view.findViewById(R.id.artworkImage);
        artistNameTextView = view.findViewById(R.id.artist_name);
        genresTextView = view.findViewById(R.id.genres);
        followersTextView = view.findViewById(R.id.followers);
        albumRecyclerView = view.findViewById(R.id.album_result_recycler_view);
        albumRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        trackRecyclerView = view.findViewById(R.id.track_result_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        addArtistButton = view.findViewById(R.id.add_button);


        artist = getArguments().getParcelable("artist");

        if (artist != null){
            if (artist.artworkUrl != null && artist.artworkUrl != "") {
                Picasso.get()
                        .load(artist.artworkUrl)
                        .error(R.drawable.ic_image_not_found_foreground)
                        .into(artistImageView);
            }
            artistNameTextView.setText(artist.artistName);
            genresTextView.setText(artist.getJoinedGenres());
            followersTextView.setText(NumberUtils.formatWithComma(artist.followers));
            ArtistApiHelper apiHelper = new ArtistApiHelper(this.getContext(), requireActivity());
            apiHelper.searchAlbumsByArtist(artist.artistId, albumList -> {
                SimpleAlbumAdapter albumAdapter = new SimpleAlbumAdapter(albumList);
                albumRecyclerView.setAdapter(albumAdapter);
                albumRecyclerView.setNestedScrollingEnabled(false);


            }, 0);
            apiHelper.searchTrackByArtist(artist.artistId, tracks -> {
                TrackAdapter trackAdapter = new TrackAdapter(tracks, getContext(), this::showTrackDetails, this::addFavoriteSong);
                trackRecyclerView.setAdapter(trackAdapter);
                trackRecyclerView.setNestedScrollingEnabled(false);
            }, 0);

            addArtistButton.setOnClickListener(v -> {
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
            });

        }else{
            Log.e("ArtistInfoFragment", "artist(Artist) is null!");
        }



    }


    private void addFavoriteSong(Track track){
        new AlertDialog.Builder(getContext())
                .setTitle("관심목록에 추가")
                .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
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


}

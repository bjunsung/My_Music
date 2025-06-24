package com.example.mymusic.ui.albumInfo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.mymusic.adapter.TrackAdapter;
import com.example.mymusic.model.Album;
import com.example.mymusic.model.Track;
import com.example.mymusic.network.ArtistApiHelper;
import com.example.mymusic.ui.favorites.FavoritesViewModel;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AlbumInfoFragment extends Fragment {

    private Album album;

    private ArtistApiHelper apiHelper;
    private TrackAdapter trackAdapter;
    private FavoritesViewModel favoritesViewModel;



    //View
    private ImageView albumImageView;
    private TextView albumNameTextView, artistNameTextView, releaseDateTextView, totalTracksTextView;
    private RecyclerView trackRecyclerView;


    @Override
    public void onCreate(@Nullable Bundle savedInstatceState){
        super.onCreate(savedInstatceState);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState){
        return  inflater.inflate(R.layout.fragment_album_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        album = getArguments().getParcelable("album");

        //View connection
        viewConnect(view);

        apiHelper = new ArtistApiHelper(getContext(), requireActivity());

        apiHelper.searchTrackByAlbum(album, trackList -> {
            trackAdapter = new TrackAdapter(trackList, getContext(), this::showTrackDetails, this::addFavoriteSong);
            trackAdapter.setShowImage(false);
            trackAdapter.setShowPosition(true);
            trackRecyclerView.setAdapter(trackAdapter);

        }, 0);


        setView();

    }

    private void viewConnect(View view){
        albumImageView = view.findViewById(R.id.artwork_image);
        albumNameTextView = view.findViewById(R.id.album_name);
        artistNameTextView = view.findViewById(R.id.artist_name);
        releaseDateTextView = view.findViewById(R.id.release_date);
        totalTracksTextView = view.findViewById(R.id.total_tracks);
        trackRecyclerView = view.findViewById(R.id.track_result_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setView(){
        if (album.artworkUrl != null){
            Picasso.get()
                    .load(album.artworkUrl)
                    .error(R.drawable.ic_image_not_found_foreground)
                    .into(albumImageView);
        }
        albumNameTextView.setText(album.albumName);
        artistNameTextView.setText(album.artistName);
        releaseDateTextView.setText(album.releaseDate);
        totalTracksTextView.setText(String.valueOf(album.totalTracks));
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

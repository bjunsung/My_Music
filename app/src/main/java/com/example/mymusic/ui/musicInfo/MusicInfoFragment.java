package com.example.mymusic.ui.musicInfo;

import android.app.AlertDialog;
import android.app.Dialog;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymusic.R;
import com.example.mymusic.data.local.Favorites;
import com.example.mymusic.model.Track;
import com.example.mymusic.ui.favorites.FavoritesAdapter;
import com.example.mymusic.ui.favorites.FavoritesViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MusicInfoFragment extends Fragment {
    private Track track;
    FavoritesViewModel favoritesViewModel;

    //ViewModel 연결
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        track = getArguments().getParcelable("track");

        if (track != null) {
            ImageView artworkImage = view.findViewById(R.id.artworkImage);
            TextView trackTitle = view.findViewById(R.id.trackTitle);
            TextView artistName = view.findViewById(R.id.artistName);
            TextView albumName = view.findViewById(R.id.albumName);
            TextView releaseDate = view.findViewById(R.id.releaseDate);
            TextView durationMs = view.findViewById(R.id.durationMs);

            // 이미지 표시: Picasso 등 사용
            if (track.artworkUrl != null && !track.artworkUrl.isEmpty()) {
                com.squareup.picasso.Picasso.get().load(track.artworkUrl).into(artworkImage);
            }
            trackTitle.setText(track.trackName);
            artistName.setText(track.artistName);
            albumName.setText(track.albumName);
            releaseDate.setText(track.releaseDate);
            int durationSec = (int) Double.parseDouble(track.durationMs)/1000;
            durationMs.setText(durationSec/60 + "분 " + durationSec%60 + "초");
            Log.d("TraceId - artist ID", track.artistId);
            Log.d("TraceId - album ID", track.albumId);
            Log.d("TraceId - track ID", track.trackId);
        }
        else {
            Log.e("MusicInfoFragment", "track(Track) is null!");
        }

        ImageButton addButton = view.findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("관심목록에 추가")
                    .setMessage(track.trackName + " - " + track.artistName + " 을(를) Favorites List 에 추가할까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
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
        });
    }

}

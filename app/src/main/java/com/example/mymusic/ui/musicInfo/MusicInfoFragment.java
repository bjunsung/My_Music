package com.example.mymusic.ui.musicInfo;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mymusic.R;
import com.example.mymusic.model.Track;

public class MusicInfoFragment extends Fragment {
    private static final String ARG_TRACK = "track";
    public static MusicInfoFragment newInstance(Track track, String accessToken) {
        MusicInfoFragment fragment = new MusicInfoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRACK, track);  // TrackItem implements Serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Track track = getArguments().getParcelable("track");
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
            Log.e("MusicInfoFragment", "track is null!");
        }
    }

}

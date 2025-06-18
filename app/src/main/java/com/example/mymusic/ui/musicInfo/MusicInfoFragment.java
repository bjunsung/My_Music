package com.example.mymusic.ui.musicInfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mymusic.R;
import com.example.mymusic.model.SongItem;

public class MusicInfoFragment extends Fragment {
    private static final String ARG_SONG = "song";

    public static MusicInfoFragment newInstance(SongItem song) {
        MusicInfoFragment fragment = new MusicInfoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);  // SongItem implements Serializable
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

        SongItem song = (SongItem) getArguments().getSerializable("song");

        // 예: TextView에 song 데이터 표시
    }
}

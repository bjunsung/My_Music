package com.example.mymusic.ui.search;

import androidx.lifecycle.ViewModel;

import com.example.mymusic.model.Artist;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    public final List<Track> searchTrackResults = new ArrayList<>();
    public final List<Artist> searchArtistResults = new ArrayList<>();
    public int selectedOption = 0; // 기본값: search Track
}

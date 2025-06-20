package com.example.mymusic.ui.search;

import androidx.lifecycle.ViewModel;

import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    public final List<Track> searchResults = new ArrayList<>();
}

package com.example.mymusic.model;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    public final List<Track> searchResults = new ArrayList<>();
}

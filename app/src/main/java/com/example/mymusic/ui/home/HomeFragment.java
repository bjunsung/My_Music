package com.example.mymusic.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mymusic.cache.ImagePreloader;
import com.example.mymusic.databinding.FragmentHomeBinding;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;
import com.example.mymusic.ui.favorites.FavoritesFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    Context viewGroupContext;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        viewGroupContext = container.getContext();
        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        //ImagePreloader.preloadRepresentativeFavoriteArtistImage(viewGroupContext, 160, 160);
        //new Handler(Looper.getMainLooper()).postDelayed(() -> ImagePreloader.preloadRepresentativeFavoriteArtistImage(viewGroupContext, 480, 480), 1);

        List<List<Integer>> preloadSizeList = new ArrayList<>();
        preloadSizeList.add(new ArrayList<>(Arrays.asList(FavoritesFragment.FAVORITE_ARTIST_REPRESENTATIVE_ARTWORK_SIZE, FavoritesFragment.FAVORITE_ARTIST_REPRESENTATIVE_ARTWORK_SIZE)));
        preloadSizeList.add(new ArrayList<>(Arrays.asList(ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE)));
        ImagePreloader.preloadRepresentativeFavoriteArtistImageWithDataLoad(viewGroupContext, preloadSizeList);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
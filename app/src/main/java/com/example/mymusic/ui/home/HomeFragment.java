package com.example.mymusic.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mymusic.cache.reader.FavoriteArtistReader;
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter;
import com.example.mymusic.databinding.FragmentHomeBinding;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment;
import com.example.mymusic.util.SortFilterArtistUtil;

import java.util.List;

public class HomeFragment extends Fragment {
    private final static String TAG = "HomeFragment";

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
        /**
         * Custom L1 L3 캐시 데이터 삭제 메소드 (테스트용, 기본 주석처리)
         */
        //CustomFavoriteArtistImageCacheL1.getInstance().clear();
        //CustomFavoriteArtistImageDiskCacheL3.getInstance(viewGroupContext).clear();

        boolean successToFetchDiskCache = CustomFavoriteArtistImageWriter.saveRepresentativeImageFromL3DiskCacheToL1Cache(viewGroupContext);

        if (successToFetchDiskCache) {
            Log.d(TAG, "success to load L3 disk cache and store to L1 memory cache");
        } else{
            Log.d(TAG, "fail to load L3 disk cache, start to load FavoriteArtist List from room db and store to L1, L3 cache");
            FavoriteArtistReader.loadFavoritesOriginalForm(viewGroupContext, favoriteArtistList -> {
                List<FavoriteArtist> filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(viewGroupContext, favoriteArtistList);
                CustomFavoriteArtistImageWriter.saveRepresentativeImagesByFavoriteArtistList(viewGroupContext, filtered, ArtistInfoFragment.ARTIST_ARTWORK_SIZE, ArtistInfoFragment.ARTIST_ARTWORK_SIZE);
            });
        }


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
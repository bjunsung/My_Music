package com.example.mymusic.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL1
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageDiskCacheL3
import com.example.mymusic.cache.reader.FavoriteArtistReader
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter
import com.example.mymusic.databinding.FragmentHomeBinding
import com.example.mymusic.model.FavoriteArtist
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment
import com.example.mymusic.util.SortFilterArtistUtil

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    var viewGroupContext: Context? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val homeViewModel: HomeViewModel by viewModels()

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding!!.root

        if (container != null) {
            viewGroupContext = container.context
        }
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * Custom L1 L3 캐시 데이터 삭제 메소드 (테스트용, 기본 주석처리)
         */
        //CustomFavoriteArtistImageCacheL1.getInstance().clear()
        //CustomFavoriteArtistImageDiskCacheL3.getInstance(viewGroupContext).clear()

        val successToFetchDiskCache =
            CustomFavoriteArtistImageWriter.saveRepresentativeImageFromL3DiskCacheToL1Cache(
                viewGroupContext
            )

        if (successToFetchDiskCache) {
            Log.d(TAG, "success to load L3 disk cache and store to L1 memory cache")
        } else {
            Log.d(
                TAG,
                "fail to load L3 disk cache, start to load FavoriteArtist List from room db and store to L1, L3 cache"
            )
            FavoriteArtistReader.loadFavoritesOriginalForm(
                viewGroupContext
            ) { favoriteArtistList: List<FavoriteArtist?>? ->
                val filtered = SortFilterArtistUtil.sortAndFilterFavoritesList(
                    viewGroupContext,
                    favoriteArtistList
                )
                CustomFavoriteArtistImageWriter.saveRepresentativeImagesByFavoriteArtistList(
                    viewGroupContext,
                    filtered,
                    ArtistInfoFragment.ARTIST_ARTWORK_SIZE,
                    ArtistInfoFragment.ARTIST_ARTWORK_SIZE
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}
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
import androidx.navigation.fragment.findNavController
import com.example.mymusic.R
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageCacheL1
import com.example.mymusic.cache.customCache.CustomFavoriteArtistImageDiskCacheL3
import com.example.mymusic.cache.reader.FavoriteArtistReader
import com.example.mymusic.cache.writer.CustomFavoriteArtistImageWriter
import com.example.mymusic.databinding.FragmentHomeBinding
import com.example.mymusic.model.FavoriteArtist
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment
import com.example.mymusic.ui.search.SearchFragment
import com.example.mymusic.util.SortFilterArtistUtil

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    var viewGroupContext: Context? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val homeViewModel: HomeViewModel by viewModels()

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (container != null) {
            viewGroupContext = container.context
        }
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()


    }

    private fun bind() {
        val searchBar = binding.searchBar
        searchBar.setOnClickListener {
            val navController = findNavController()
            val args = Bundle()
            args.putBoolean(SearchFragment.ARG_REQUEST_FOCUS, true)
            if (navController.currentDestination?.id == R.id.navigation_home) {
                navController.navigate(R.id.action_home_to_searchFragment,  args)
            } else{
                Log.d(TAG, "navigation departure mismatch")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}
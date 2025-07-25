package com.example.mymusic.ui.home

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentHomeBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.ui.search.SearchFragment
import com.example.mymusic.util.ViewPagerAutoScroller
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@UnstableApi
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    var viewGroupContext: Context? = null
    val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    val homeViewModel: HomeViewModel by viewModels()
    var onThisDaysPageAdapter: OnThisDaysPageAdapter  = OnThisDaysPageAdapter(emptyList(),
        object : OnThisDaysPageAdapter.OnClickListener {
            override fun onItemClick(holder: OnThisDaysPageAdapter.ViewHolder, favorite: Favorite) {
                val args = Bundle().apply {
                    putString(MusicInfoFragment.TRANSITION_NAME_KEY, holder.artworkImage.transitionName)
                    putParcelable(MusicInfoFragment.ARGUMENTS_KEY, favorite)
                }
                val extras = FragmentNavigatorExtras(
                    holder.artworkImage to holder.artworkImage.transitionName
                )
                findNavController().navigate(R.id.musicInfoFragment, args, null, extras)
            }

            override fun onPlayButtonClick(list: List<Favorite>, position: Int) {
                val ordered =  list.subList(position, list.size) + list.subList(0, position)
                val filtered = ordered.filter { !it.audioUri.isNullOrEmpty() }
                mainActivityViewModel.setPlaylist(filtered, 0)
            }
        })
    var onThisMonthPageAdapter: OnThisMonthPageAdapter  = OnThisMonthPageAdapter(emptyList(),
        object : OnThisMonthPageAdapter.OnClickListener {
            override fun onItemClick(holder: OnThisMonthPageAdapter.ViewHolder, favorite: Favorite) {
                val args = Bundle().apply {
                    putString(MusicInfoFragment.TRANSITION_NAME_KEY, holder.artworkImage.transitionName)
                    putParcelable(MusicInfoFragment.ARGUMENTS_KEY, favorite)
                }
                val extras = FragmentNavigatorExtras(
                    holder.artworkImage to holder.artworkImage.transitionName,
                    holder.titleTextView to holder.titleTextView.transitionName,
                    holder.artistNameTextView to holder.artistNameTextView.transitionName,
                    holder.releaseDateTextView to holder.releaseDateTextView.transitionName
                )
                findNavController().navigate(R.id.musicInfoFragment, args, null, extras)
            }

            override fun onPlayButtonClick(list: List<Favorite>, position: Int) {
                val ordered =  list.subList(position, list.size) + list.subList(0, position)
                val filtered = ordered.filter { !it.audioUri.isNullOrEmpty() }
                mainActivityViewModel.setPlaylist(filtered, 0)
            }
        })
    var onThisDayReleasesScroller: ViewPagerAutoScroller? = null
    var onThisMonthReleasesScroller: ViewPagerAutoScroller ?= null

    val today = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (container != null) {
            viewGroupContext = container.context
        }
        return root
    }

    //marginPx -> 사진 떨어뜨리기, position 별 스케일
    private val pageTransformer by lazy {
        val marginPx = getResources().getDimensionPixelOffset(R.dimen.artwork_side_margin_v2)
        val marginPageTransformer = MarginPageTransformer(marginPx)
        object : ViewPager2.PageTransformer {
            override fun transformPage(page: View, position: Float) {
                marginPageTransformer.transformPage(page, position)
                val scale = 0.65f + (1 - Math.abs(position)) * 0.35f
                page.scaleY = scale
                page.alpha = 0.5f + (1 - Math.abs(position)) * 0.5f
            }
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()

        mainActivityViewModel.favoriteList.observe(viewLifecycleOwner) { newList ->
            homeViewModel.checkOnThisDayReleases(newList)
        }

        setOnThisDayReleasesViewPager()




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

    private fun setOnThisMonthReleasesViewPager() {
        val onThisMonthsReleases = homeViewModel.onThisMonthReleases.value
        if (onThisMonthsReleases.isNullOrEmpty()) {
            binding.seeOnThisMonthReleasesFrame.visibility = View.GONE
        } else {
            binding.seeOnThisMonthReleasesFrame.visibility = View.VISIBLE
        }

        binding.seeOnThisMonthReleasesText.text = today.month.toString() + " 발매"
        binding.onThisMonthReleasesCountText.text = "(${onThisMonthsReleases?.size ?: 0}곡)"
        binding.onThisMonthReleasesViewPager.adapter = onThisMonthPageAdapter
        onThisMonthPageAdapter.updateList(onThisMonthsReleases!!)

        binding.onThisMonthReleasesViewPager.visibility = View.VISIBLE
        if (onThisMonthsReleases.size == 1) {
            binding.onThisMonthReleasesViewPager.scaleX = 0.925f
            binding.onThisMonthReleasesViewPager.scaleY = 0.925f
        } else {
            binding.onThisMonthReleasesViewPager.scaleX = 0.8f
            binding.onThisMonthReleasesViewPager.scaleY = 0.8f
        }
        onThisMonthReleasesScroller?.detach()

        onThisMonthReleasesScroller = ViewPagerAutoScroller(
            pager = binding.onThisMonthReleasesViewPager,
            autoDelayMs = 5_000L,
            afterUserDelayMs = 7_500L
        ).also {
            it.attach()
            it.start()
        }

        binding.onThisMonthReleasesViewPager.setPageTransformer(pageTransformer)
        binding.onThisMonthReleasesViewPager.offscreenPageLimit = 1

    }

    private fun setOnThisDayReleasesViewPager() {
        binding.onThisDaysReleasesViewPager.adapter = onThisDaysPageAdapter

        homeViewModel.onThisDayReleases.observe(viewLifecycleOwner) { onThisDayReleases ->
            onThisDayReleases?.let {
                if (it.size > 0) {
                    binding.onThisDaysReleasesFrame.visibility = View.VISIBLE
                    if (it.size == 1) {
                        binding.onThisDaysReleasesViewPager.scaleX = 0.925f
                        binding.onThisDaysReleasesViewPager.scaleY = 0.925f
                    } else {
                        binding.onThisDaysReleasesViewPager.scaleX = 0.8f
                        binding.onThisDaysReleasesViewPager.scaleY = 0.8f
                    }
                    onThisDaysPageAdapter.updateList(it)
                } else {
                    binding.onThisDaysReleasesFrame.visibility = View.GONE
                }
            }

            Handler(Looper.getMainLooper()).postDelayed({
                setOnThisMonthReleasesViewPager()
            }, 0)


        }

        //page 변경시 텍스트 변경 & viewModel 에 page 저장
        val myCallbacks = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val selected = onThisDaysPageAdapter.getItemByPosition(position)
                selected?.let {
                    val releaseDate = LocalDate.parse(it.track.releaseDate)
                    binding.yearDiff.text =
                        ChronoUnit.YEARS.between(releaseDate, LocalDate.now()).toString() + " 년전 오늘"
                    val titleAndArtistText = it.title + " - " + it.artistName
                    binding.titleAndArtist.text = "$titleAndArtistText (이)가 발매됐어요."
                }
            }
        }

        //auto scroller 연결
        onThisDayReleasesScroller?.detach()

        onThisDayReleasesScroller = ViewPagerAutoScroller(
            pager = binding.onThisDaysReleasesViewPager,
            autoDelayMs = 5_000L,
            afterUserDelayMs = 7_500L,
            extraCallbacks = listOf(myCallbacks)
        ).also {
            it.attach()
            it.start()
        }


        binding.onThisDaysReleasesViewPager.setPageTransformer(pageTransformer)
        binding.onThisDaysReleasesViewPager.offscreenPageLimit = 1

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, homeViewModel.onThisDayReleases.value?.size.toString())
        binding.onThisDaysReleasesViewPager.adapter = onThisDaysPageAdapter
        binding.onThisMonthReleasesViewPager.adapter = onThisMonthPageAdapter
        homeViewModel.onThisDayReleases.value?.let { onThisDaysPageAdapter.updateList(it) }
        homeViewModel.onThisMonthReleases.value?.let { onThisMonthPageAdapter.updateList(it) }
        onThisDayReleasesScroller?.stop()
        onThisMonthReleasesScroller?.stop()
        onThisDayReleasesScroller?.start()
        onThisMonthReleasesScroller?.start()
    }

    override fun onPause() {
        super.onPause()
        onThisDayReleasesScroller?.stop()
        onThisMonthReleasesScroller?.stop()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.onThisDaysReleasesViewPager.adapter = null
        binding.onThisMonthReleasesViewPager.adapter = null
        onThisDayReleasesScroller?.detach()
        onThisMonthReleasesScroller?.detach()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}
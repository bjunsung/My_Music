package com.example.mymusic.ui.chart

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.ReleaseDateChartAdapter
import com.example.mymusic.databinding.FragmentReleaseDateChartBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.myCalendar.ReleaseDateViewModel
import com.example.mymusic.util.DarkModeUtils
import com.example.mymusic.util.SortFilterUtil
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.LocalDate
import kotlin.properties.Delegates
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.data.repository.FavoriteArtistRepository
import com.example.mymusic.dataLoader.FavoriteArtistLoader
import com.example.mymusic.model.FavoriteArtist
import com.example.mymusic.network.ArtistApiHelper
import com.example.mymusic.ui.albumInfo.AlbumInfoFragment
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.util.DateUtils
import com.github.mikephil.charting.components.Legend
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

@UnstableApi
class ReleaseDateChart: Fragment() {
    private var _binding: FragmentReleaseDateChartBinding? = null
    private val binding: FragmentReleaseDateChartBinding get() = _binding!!
    private lateinit var lineChart: LineChart
    private val viewModel: ReleaseDateViewModel by viewModels()
    private lateinit var favList: List<Favorite>
    private lateinit var yearLabels: List<String>
    private var startYear by Delegates.notNull<Int>()
    private var endYear by Delegates.notNull<Int>()
    private lateinit var entries: List<Entry>
    private lateinit var favListForYearMap: MutableMap<Int, List<Favorite>>
    private lateinit var seeMoreButton: TextView
    private lateinit var foldButton: TextView
    private lateinit var yearSelectedTextView: TextView
    private lateinit var favoriteCountTextView: TextView
    private lateinit var popupWindow: PopupWindow
    private var preventUnfocus = false
    private lateinit var anchorView: View
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()



    private val adapter: ReleaseDateChartAdapter = ReleaseDateChartAdapter(listOf(), object: ReleaseDateChartAdapter.OnItemEventListener {
        override fun onItemClick(view: View, item: Favorite, position: Int) {

            if (viewModel.focusedTrack != null && item.equals(viewModel.focusedTrack)) {
                if (viewModel.itemClickRepeated++ % 2 == 1)
                    return
            }
            else {
                viewModel.itemClickRepeated = 1
            }
            viewModel.currentPosition = position
            viewModel.currentTrackId = item.track.trackId
            showPopupWindow(view, item, position)
        }

        override fun onImageReady(trackId: String) {
            if (viewModel.currentTrackId == null || viewModel.currentTrackId == trackId)
                startPostponedEnterTransition()
        }

        override fun onImageLoadFailed() {}

    })

    private fun showPopupWindow(anchorView: View, item: Favorite, position: Int) {
        if (viewModel.reenterStateFromMusicInfoFragment || viewModel.reenterStateFromAlbumInfoFragment || viewModel.reenterStateFromArtistInfoFragment)
            return

        this.anchorView = anchorView

        viewModel.onFocused = true
        viewModel.focusedPosition = position
        viewModel.focusedTrack = item

        //val popupView = layoutInflater.inflate(R.layout.page_favorite_detail_info, null)
        val popupView = layoutInflater.inflate(R.layout.popup_favorite_detail_pager, null)
        val viewPager = popupView.findViewById<ViewPager2>(R.id.popup_view_pager)
        viewPager.adapter = PopupPagerAdapter(item, object: PopupPagerAdapter.OnClickEventListener {
            override fun onTitleClick(item: Favorite) {
                this@ReleaseDateChart.onTitleClick(item)
            }

            override fun onAlbumNameClick(item: Favorite) {
                this@ReleaseDateChart.onAlbumNameClick(item)
            }

            override fun onArtistNameClick(item: Favorite) {
                this@ReleaseDateChart.onArtistNameClick(item)
            }

            @OptIn(UnstableApi::class)
            override fun onPlayButtonClick(item: Favorite) {
                if (item.audioUri == null){
                    Log.d(TAG, "audio not found, skipping play")
                    return
                }
                val playlist = viewModel.shuffledList.filter { !it.audioUri.isNullOrEmpty() }
                val currentPosition = viewModel.currentPosition

                val ordered = (playlist.subList(currentPosition, playlist.size) +
                        playlist.subList(0, currentPosition)).toMutableList()

                mainActivityViewModel.setPlaylist(ordered, 0)

            }
        })


        val indicator = popupView.findViewById<me.relex.circleindicator.CircleIndicator3>(R.id.page_indicator)
        indicator.setViewPager(viewPager)

        //val layoutWidth = (220 * context?.resources?.displayMetrics!!.density).toInt()



        val context: Context = requireContext()
        val layoutWidth = if (isTablet(context)) {
            dpToPx(context, 235)   // 태블릿
        } else {
            dpToPx(context, 195)   // 폰
        }

        popupWindow = PopupWindow(
            popupView,
            layoutWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }


        val minHeight = if (isTablet(context)) dpToPx(context, 389) else dpToPx(context, 280)
        popupView.minimumHeight = minHeight

// 먼저 내부 뷰 강제 측정
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val contentHeight = popupView.measuredHeight

// 최소 높이와 실제 내용 높이 중 큰 값으로 적용
        popupWindow.height = maxOf(minHeight, contentHeight)

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        Log.d(TAG, "x: " + x + " y: " + y + " screenWidth " + screenWidth)


        val adjustedDx = if (isTablet(context)) 60f else 100f

        val bubbleDrawable = if (screenWidth - x < layoutWidth) {
            BubbleDrawable(
                context,
                bubbleColor = Color.parseColor("#333333"),
                cornerRadius = 24f,
                tailPosition = BubbleDrawable.TailPosition.RIGHT,
                dx = (screenWidth - x - adjustedDx)
            )
        } else{
             BubbleDrawable(
                 context,
                 bubbleColor = Color.parseColor("#333333"),
                 cornerRadius = 24f,
                 tailPosition = BubbleDrawable.TailPosition.LEFT,
                 dx = adjustedDx
            )
        }


        popupView.background = bubbleDrawable

        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y + anchorView.height)


        preventUnfocus = false

        popupWindow.setOnDismissListener {
            if (!preventUnfocus) {
                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.onFocused = false
                    //viewModel.focusedPage = 0
            }, 0)
            }

        }
    }

    fun isTablet(context: Context): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    fun onTitleClick(item: Favorite) {
        preventUnfocus = true
        popupWindow.dismiss()
        val args = Bundle().apply {
            putParcelable(MusicInfoFragment.ARGUMENTS_KEY, item)
            putString(MusicInfoFragment.TRANSITION_NAME_KEY, anchorView.transitionName)
        }

        val extras = FragmentNavigatorExtras(
            anchorView to anchorView.transitionName
        )
        val navController = NavHostFragment.findNavController(this)
        val currentDestination = navController.currentDestination
        if (currentDestination?.id == R.id.fragment_release_date_chart) {
            viewModel.reenterStateFromMusicInfoFragment = true
            findNavController().navigate(R.id.musicInfoFragment, args, null, extras)
        } else {
            Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨")
        }
    }

    fun onAlbumNameClick(item: Favorite) {
        preventUnfocus = true
        popupWindow.dismiss()

        val track = item.track
        val apiHelper = ArtistApiHelper(requireContext())
        apiHelper.getAlbum(null, track.albumId) { album ->
            if (album != null) {
                val args = Bundle().apply {
                    putString(AlbumInfoFragment.TRANSITION_NAME_KEY, anchorView.transitionName)
                    putParcelable(AlbumInfoFragment.ARGUMENTS_KEY, album)
                }

                val extras = FragmentNavigatorExtras(
                    anchorView to anchorView.transitionName
                )

                val navController = NavHostFragment.findNavController(this)
                val currentDestination = navController.currentDestination
                if (currentDestination?.id == R.id.fragment_release_date_chart) {
                    viewModel.reenterStateFromAlbumInfoFragment = true
                    findNavController().navigate(R.id.action_releaseDateChart_to_albumInfoFragment, args, null, extras)
                } else {
                    Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨")
                }
            }
        }
    }

    fun onArtistNameClick(item: Favorite) {
        preventUnfocus = true
        popupWindow.dismiss()
        val args = Bundle()
        FavoriteArtistLoader.loadFavoriteArtistById(requireContext(), item.track.artistId, object : FavoriteArtistLoader.Companion.OnLoadListener {
            override fun onLoadSuccess(fav: FavoriteArtist) {
                args.putParcelable(ArtistInfoFragment.ARGUMENTS_KEY, fav)
                val navController = findNavController()
                if (navController.currentDestination!!.id == R.id.fragment_release_date_chart) {
                    viewModel.reenterStateFromArtistInfoFragment = true
                    navController.navigate(R.id.action_releaseDateChart_to_artistInfoFragment, args)
                } else {
                    Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨")
                }

            }

            override fun onLoadFailed() {
                Log.d(TAG, "favorite artist load failed")
                val apiHelper = ArtistApiHelper(requireContext())
                apiHelper.getArtist(null, item.track.artistId) { artist ->
                    val artist = FavoriteArtist(artist)
                    args.putParcelable(ArtistInfoFragment.ARGUMENTS_KEY, artist)
                    val navController = findNavController()
                    if (navController.currentDestination!!.id == R.id.fragment_release_date_chart) {
                        viewModel.reenterStateFromArtistInfoFragment = true
                        navController.navigate(R.id.action_releaseDateChart_to_artistInfoFragment, args)
                    } else {
                        Log.w(TAG, "현재 위치가 musicInfoFragment가 아님. Navigation 취소됨")
                    }
                }
            }

        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
    : View {
        _binding = FragmentReleaseDateChartBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        Handler(Looper.getMainLooper()).postDelayed(Runnable{ startPostponedEnterTransition() }, 50)
        setGradientColor()
        bind()
        prepareChart()


        //reenter from MusicInfoFragment
        parentFragmentManager.setFragmentResultListener(
            MusicInfoFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { key, bundle ->
            val transitionEnded = bundle.getBoolean(MusicInfoFragment.BUNDLE_KEY_TRANSITION_END, false)
            if (transitionEnded) {
                Log.d(TAG, "transition end callback, show popupwindow")
                viewModel.reenterStateFromMusicInfoFragment = false
                //showPopupWindowAgain()
            }
        }

        parentFragmentManager.setFragmentResultListener(
            AlbumInfoFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { key, bundle ->
            val transitionEnd = bundle.getBoolean(AlbumInfoFragment.BUNDLE_KEY_TRANSITION_END, false)
            if (transitionEnd){
                viewModel.reenterStateFromAlbumInfoFragment = false
                //showPopupWindowAgain()
            }
        }


    }

    private fun bind(){
        lineChart = binding.lineChart
        setRecyclerView()
    }


    private var gradientColor = intArrayOf()
    private var recyclerView: RecyclerView? = null

    override fun onResume() {
        super.onResume()
        setGradientColor()


    }


    private fun setGradientColor() {
        if (DarkModeUtils.isDarkMode(context)){
            gradientColor = intArrayOf(
                Color.parseColor("#FFA726"),
                Color.parseColor("#2B2B2B")
            )
        }
        else{
            gradientColor = intArrayOf(
                Color.parseColor("#FB8C00"),
                Color.parseColor("#FFF3E0")
            )
        }
    }

    private fun prepareChart() {
        Log.d(TAG, "preparing data for chart...")
        viewModel.loadFavoriteList(object : ReleaseDateViewModel.FavoriteLoadCallback {
            override fun onSuccess(favList: List<Favorite>) {
                viewModel.updateFavoriteList(favList)
                requireActivity().runOnUiThread {

                    this@ReleaseDateChart.favList = favList

                    if (favList.isNotEmpty()) {
                        val filtered = SortFilterUtil.sortAndFilterFavoritesList(
                            context,
                            favList,
                            SortFilterUtil.filter_ALL,
                            null,
                            SortFilterUtil.sort_RELEASE_DATE,
                            false
                        )
                        val minDate = filtered[0].releaseDate
                        val maxDate = filtered[filtered.size - 1].releaseDate
                        startYear = minDate.take(4).toIntOrNull() ?: 1980
                        endYear = maxDate.take(4).toIntOrNull() ?: LocalDate.now().year
                        viewModel.updateStartYear(startYear)
                        viewModel.updateEndYear(endYear)
                        yearLabels = (startYear..endYear).map { it.toString() } // X축 라벨
                        viewModel.chartLabels = yearLabels

                        favListForYearMap = mutableMapOf()

                        for (y in startYear..endYear) {
                            val listOfYear = viewModel.getListOfYear(filtered, y)
                            if (listOfYear.isNotEmpty())
                                favListForYearMap[y] = listOfYear
                        }

                        viewModel.favListForYearMap = favListForYearMap

                        entries = yearLabels.mapIndexed { index, yearStr ->
                            val year = yearStr.toInt()
                            val value = (favListForYearMap[year]?.size)?.toFloat() ?: 0f
                            Entry(index.toFloat(), value)
                        }
                        viewModel.chartEntries = entries

                        setupChart()

                    }
                }
            }

            override fun onDuplicated() {
                Log.d(TAG, "list duplicated")
                yearLabels = viewModel.chartLabels
                entries = viewModel.chartEntries
                favListForYearMap = viewModel.favListForYearMap
                Log.d(TAG, "map restore: " + favListForYearMap.toString())
                setupChart()
            }
            override fun onFailure(message: String?) {}
        })
    }

    private fun setupChart() {
        Log.d(TAG, "all data ready, set up chart...")
        lineChart.clear()

        // --- 여기부터 차트 연결 ---
        val dataSet = LineDataSet(entries, "연도별 노래")

        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet.color = "#FFB74D".toColorInt()

        val textColor = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        lineChart.xAxis.textColor = textColor



        val legend = lineChart.legend

        val isTablet = isTablet(requireContext())

        if (isTablet)
            lineChart.setExtraOffsets(30f, 20f, 30f, 24f)
        else
            lineChart.setExtraOffsets(5f, 0f, 0f, 5f)

        legend.textColor = textColor
        legend.verticalAlignment =
            if (isTablet) Legend.LegendVerticalAlignment.BOTTOM
            else Legend.LegendVerticalAlignment.CENTER
        legend.horizontalAlignment =
            if (isTablet) Legend.LegendHorizontalAlignment.CENTER
            else Legend.LegendHorizontalAlignment.RIGHT

        legend.textSize = if (isTablet(requireContext())) 18f else 12f
        legend.setDrawInside(false)


        dataSet.valueTextSize = 13.5f

        dataSet.lineWidth = 3f
        dataSet.circleRadius = 2f
        dataSet.setCircleColor(android.graphics.Color.GRAY)

        dataSet.setDrawCircles(false)   // 점 숨김 (원하는 경우)
        dataSet.setDrawValues(false)    // 값 숨김
        dataSet.setDrawFilled(true)     // 채우기 활성화
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, // 방향
            gradientColor

        )
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = gradientDrawable


        val lineData = com.github.mikephil.charting.data.LineData(dataSet)
        lineChart.data = lineData

        // X축 라벨 설정
        val xAxis = lineChart.xAxis

        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter =
            com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                yearLabels
            )
        xAxis.position =
            com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -60f // 라벨 회전
        xAxis.setLabelCount(yearLabels.size, true) // 모든 레이블 강제 표시
        //value 정수로
        lineChart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return value.toInt().toString()
            }
        }

        lineChart.isScaleYEnabled = true // 세로 줌 비활성화
        lineChart.setDragEnabled(true) //가로 줌 활성화
        lineChart.isScaleXEnabled = true

        //value 정수로
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.y?.toInt().toString()
            }
        }


        // 왼쪽 Y 축
        val axisLeft = lineChart.axisLeft
        axisLeft.setDrawGridLines(false)


        // 오른쪽 Y축 제거
        lineChart.axisRight.isEnabled = false

        // 설명 제거
        lineChart.description.isEnabled = false

        // 애니메이션
        lineChart.animateY(750)


        seeMoreButton = binding.seeMoreButton
        foldButton = binding.foldButton


        dataSet.isHighlightEnabled = true // 데이터셋 하이라이트 활성화
        lineChart.setTouchEnabled(true)   // 차트 터치 활성화
        lineChart.isHighlightPerTapEnabled = true // 탭당 하이라이트 활성화

        lineChart.post {
            lineChart.setOnChartValueSelectedListener(null)
            lineChart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    Log.d(TAG, "value selected")
                    if (h != null) {
                        viewModel.selectedHighlightYear = h
                    }

                    var x = e?.x!!.toInt()
                    var yearSelected = yearLabels.get(x)
                    var favoriteCount = e.y.toInt().toString()
                    yearSelectedTextView = binding.xValue
                    favoriteCountTextView = binding.yValue

                    yearSelectedTextView.text = yearSelected
                    favoriteCountTextView.text = favoriteCount


                    val favListForYear: List<Favorite> = favListForYearMap[yearSelected.toInt()] ?: emptyList()

                    val seeMoreState = viewModel.seeMoreState
                    val shuffled = favListForYear.shuffled()
                    viewModel.shuffledList = shuffled

                    adapter.updateList(shuffled)
                    if (seeMoreState){
                        adapter.updateVisibleCount(favListForYear.size)
                    }


                    setSeeMoreAndFoldButton(favListForYear)

                }

                override fun onNothingSelected() {}

            })
        }

        lineChart.invalidate()
        restoreData()
    }

    private fun setRecyclerView() {
        recyclerView = binding.favoriteRecyclerView
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView?.adapter = adapter
    }

    private fun restoreData() {
        yearSelectedTextView = binding.xValue
        favoriteCountTextView = binding.yValue
        recyclerView?.scrollToPosition(viewModel.focusedPosition)
        val shuffled = viewModel.shuffledList
        if (shuffled.isNotEmpty()){
            adapter.updateList(shuffled)
            setSeeMoreAndFoldButton(shuffled)
            yearSelectedTextView.text = shuffled.get(0).releaseDate.substring(0, 4)
            favoriteCountTextView.text = shuffled.size.toString()
            lineChart.postDelayed({
                lineChart.highlightValue(viewModel.selectedHighlightYear, false) }
                , 800)
        }

        if (viewModel.seeMoreState) {
            adapter.updateVisibleCount(shuffled.size)
        }

        if (viewModel.onFocused && !(
                    viewModel.reenterStateFromMusicInfoFragment
                    || viewModel.reenterStateFromAlbumInfoFragment
                    || viewModel.reenterStateFromArtistInfoFragment)) {
            recyclerView?.post { //showPopupWindowAgain()
             }
        }
        else if (viewModel.reenterStateFromArtistInfoFragment) {
            recyclerView?.postDelayed({
                viewModel.reenterStateFromArtistInfoFragment = false
                //showPopupWindowAgain()
                                      }, 280)
        }
        else{
            startPostponedEnterTransition()
        }
    }

    private fun showPopupWindowAgain() {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(viewModel.focusedPosition)
        if (viewHolder != null) {
            showPopupWindow(
                viewHolder.itemView,
                viewModel.focusedTrack!!,
                viewModel.focusedPosition
            )
        }
    }

    private fun setSeeMoreAndFoldButton(favList: List<Favorite>) {
        val seeMoreState = viewModel.seeMoreState
        seeMoreButton.visibility = if (favList.size > 3 && !seeMoreState) View.VISIBLE else View.GONE
        foldButton.visibility = if (favList.size > 3 && seeMoreState) View.VISIBLE else View.GONE


        seeMoreButton.setOnClickListener {
            Log.d(TAG, "see more button clicked")
            adapter.updateVisibleCount(favList.size)
            viewModel.seeMoreState = true
            seeMoreButton.visibility = View.GONE
            foldButton.visibility = View.VISIBLE
        }

        foldButton.setOnClickListener {
            adapter.updateVisibleCount(3)
            viewModel.seeMoreState = false
            seeMoreButton.visibility = View.VISIBLE
            foldButton.visibility = View.GONE
        }
    }

    companion object{
        private const val TAG = "ReleaseDateChart"
    }

}
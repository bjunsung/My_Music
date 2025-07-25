package com.example.mymusic.ui.chart

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.MainActivityViewModel
import com.example.mymusic.R
import com.example.mymusic.data.repository.FavoriteSongRepository
import com.example.mymusic.dataLoader.FavoriteArtistLoader
import com.example.mymusic.databinding.FragmentPlayCountChartBinding
import com.example.mymusic.databinding.PopupSelectPlaytimePeriodBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.model.FavoriteArtist
import com.example.mymusic.network.ArtistApiHelper
import com.example.mymusic.ui.albumInfo.AlbumInfoFragment
import com.example.mymusic.ui.artistInfo.ArtistInfoFragment
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar


class PlayTimeChart: Fragment() {


    private var _binding: FragmentPlayCountChartBinding? = null
    private val binding get() = _binding!!

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val playCountViewModel: PlayCountChartViewModel by viewModels()

    private val today = LocalDate.now()
    private var recyclerView: RecyclerView? = null
    private var container: ViewGroup? = null

    private lateinit var popupWindow: PopupWindow
    private var preventUnfocus = false
    private lateinit var anchorView: View
    private val apiHelper: ArtistApiHelper by lazy { ArtistApiHelper(viewGroupContext) }
    private var navController: NavController? = null

    private var viewGroupContext: Context? = null

    private var adapter: PlayTimeChartAdapter? = null

    private val modifySpecificYearButton: MaterialCardView by lazy { binding.modifySpecificYearCard }
    private val prefs: SharedPreferences by lazy { requireActivity().getSharedPreferences("last_saved_value", Context.MODE_PRIVATE) }

    override fun onResume() {
        super.onResume()
        updateChart(playCountViewModel.chartOption)


        playCountViewModel.getOrderedList()?.let {
            adapter?.updateData(getRank(it), playCountViewModel.chartOption)
            Log.d(TAG, "size" + it.size)
        }

        binding.chartOption.text = when(playCountViewModel.chartOption) {
            PlayCountChartViewModel.LAST_YEAR -> "Last Year"
            PlayCountChartViewModel.LAST_3_MONTH -> "Last 3 Month"
            else -> "Last Month"
        }


        Log.d(TAG, "item count: " + adapter?.itemCount + " getRank(it).size: "  + (getRank(playCountViewModel.getOrderedList() ?: emptyList()).size))


    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach")
        if (navController == null) {
            navController = findNavController()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        this.container = container
        this.viewGroupContext = container?.context
        _binding = FragmentPlayCountChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        //reenter from MusicInfoFragment
        parentFragmentManager.setFragmentResultListener(
            MusicInfoFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { key, bundle ->
            val transitionEnded = bundle.getBoolean(MusicInfoFragment.BUNDLE_KEY_TRANSITION_END, false)
            if (transitionEnded) {
                Log.d(TAG, "transition end callback, show popupwindow")
                playCountViewModel.reenterStateFromMusicInfoFragment = false
                showPopupWindowAgain()
            }
        }

        parentFragmentManager.setFragmentResultListener(
            AlbumInfoFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { key, bundle ->
            val transitionEnd = bundle.getBoolean(AlbumInfoFragment.BUNDLE_KEY_TRANSITION_END, false)
            if (transitionEnd){
                playCountViewModel.reenterStateFromAlbumInfoFragment = false
                showPopupWindowAgain()
            }
        }

        if (playCountViewModel.adapter == null) {
            playCountViewModel.adapter = PlayTimeChartAdapter(
                emptyList(),
                PlayCountChartViewModel.LAST_MONTH,
                object : PlayTimeChartAdapter.OnItemEventListener {
                    override fun onItemClick(anchorView: View, item: Favorite, position: Int) {
                        playCountViewModel.focusedPosition = position
                        playCountViewModel.focusedTrack = item
                        this@PlayTimeChart.showPopupDetails(anchorView, item, position)
                    }

                    override fun onImageReady(transitionName: String) {
                        if (transitionName.equals(playCountViewModel.transitionName)) {
                            startPostponedEnterTransition()
                        }
                    }
                })
        }
        this.adapter = playCountViewModel.adapter

        if (!playCountViewModel.reenterStateFromMusicInfoFragment && !playCountViewModel.reenterStateFromAlbumInfoFragment)
            startPostponedEnterTransition()
        else if (playCountViewModel.reenterStateFromArtistInfoFragment) {
            Handler(Looper.getMainLooper()).postDelayed({
                playCountViewModel.reenterStateFromArtistInfoFragment = false
                showPopupWindowAgain()
            }, 280)
        }
        else{
            Handler(Looper.getMainLooper()).postDelayed({startPostponedEnterTransition()}, 200)
        }

        Log.d(TAG, "onViewCreated")

        recyclerView = binding.recyclerView
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.dropdownButton.setOnClickListener { showCustomPopup() }
        if (playCountViewModel.rawList.isNullOrEmpty()) loadFavorites()

        val specificYearLastSaved =  prefs.getInt("specific_year", LocalDate.now().year)
        playCountViewModel.specificYear = specificYearLastSaved
        adapter?.setSpecificYear(specificYearLastSaved)

        modifySpecificYearButton.visibility = if (playCountViewModel.chartOption == PlayCountChartViewModel.SPECIFIC_YEAR) View.VISIBLE else View.GONE
        modifySpecificYearButton.setOnClickListener {
            showYearPickerDialog(
                viewGroupContext!!,
                initialYear = playCountViewModel.specificYear,
                minYear = 2000,
                maxYear = today.year)
            { year ->

                prefs.edit().putInt("specific_year", year).apply()

                adapter?.setSpecificYear(year)
                playCountViewModel.specificYear = year
                updateChart(PlayCountChartViewModel.SPECIFIC_YEAR)
                playCountViewModel.getOrderedList()?.let {
                    adapter?.updateData(getRank(it), PlayCountChartViewModel.SPECIFIC_YEAR)
                }
                binding.chartOption.text = year.toString()
            }
        }

    }
    /**
     * 커스텀 레이아웃(dialog_custom_year_picker.xml)을 이용해
     * 연도만 선택할 수 있는 다이얼로그를 띄웁니다.
     *
     * @param context        다이얼로그를 띄울 Context (프래그먼트라면 requireContext())
     * @param initialYear    초기 선택 연도 (기본값: 현재 연도)
     * @param minYear        최소 선택 연도 (기본값: 1900)
     * @param maxYear        최대 선택 연도 (기본값: 현재 연도)
     * @param onYearSelected 확인 버튼 클릭 시 선택된 연도를 받을 콜백
     */
    fun showYearPickerDialog(
        context: Context,
        initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        minYear: Int = 1900,
        maxYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        onYearSelected: (year: Int) -> Unit
    ) {
        val dialog = Dialog(context).apply {
            // 투명 배경, 모서리 둥글게 등은 XML의 MaterialCardView 속성으로 이미 처리됨
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(true)
            setContentView(R.layout.dialog_year_picker)
        }

        // 1) 뷰 바인딩
        val npYear = dialog.findViewById<NumberPicker>(R.id.npYear).apply {
            minValue = minYear
            maxValue = maxYear
            value    = initialYear
            wrapSelectorWheel = false
        }
        val titleView   = dialog.findViewById<TextView>(R.id.title)
        val subtextView = dialog.findViewById<TextView>(R.id.subtext)
        val cancelBtn   = dialog.findViewById<TextView>(R.id.cancel_button)
        val confirmBtn  = dialog.findViewById<TextView>(R.id.confirm_button)

        // 2) 텍스트 설정 (필요에 따라 바꿔 주세요)
        titleView.text   = "날짜 선택 (년)"
        subtextView.text = "원하는 연도를 선택하세요."

        // 3) 버튼 리스너
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        confirmBtn.setOnClickListener {
            onYearSelected(npYear.value)
            dialog.dismiss()
        }

        // 4) 다이얼로그 표시
        dialog.show()
    }




    private fun showCustomPopup() {
        val popupViewBinding = PopupSelectPlaytimePeriodBinding.inflate(layoutInflater)

        val popupView = popupViewBinding.root

        val popupWidth = 320
        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 16f

        when (playCountViewModel.chartOption){
            PlayCountChartViewModel.LAST_MONTH ->  popupViewBinding.lastMonthLayout.visibility = View.GONE
            PlayCountChartViewModel.LAST_3_MONTH -> popupViewBinding.last3MonthLayout.visibility = View.GONE
            PlayCountChartViewModel.LAST_YEAR -> popupViewBinding.lastYearLayout.visibility = View.GONE
            PlayCountChartViewModel.SPECIFIC_YEAR -> popupViewBinding.specificYearLayout.visibility = View.GONE
        }

        popupViewBinding.lastMonthLayout.setOnClickListener {
            playCountViewModel.chartOption = PlayCountChartViewModel.LAST_MONTH
            updateChart(PlayCountChartViewModel.LAST_MONTH)
            playCountViewModel.getOrderedList()?.let {
                adapter?.updateData(getRank(it), PlayCountChartViewModel.LAST_MONTH)
            }
            binding.chartOption.text = "Last Month"
            popupWindow.dismiss()
            modifySpecificYearButton.visibility = View.GONE
        }

        popupViewBinding.last3MonthLayout.setOnClickListener {
            playCountViewModel.chartOption = PlayCountChartViewModel.LAST_3_MONTH
            updateChart(PlayCountChartViewModel.LAST_3_MONTH)
            playCountViewModel.getOrderedList()?.let {
                adapter?.updateData(getRank(it), PlayCountChartViewModel.LAST_3_MONTH)
            }
            binding.chartOption.text = "Last 3 Month"
            popupWindow.dismiss()
            modifySpecificYearButton.visibility = View.GONE
        }

        popupViewBinding.lastYearLayout.setOnClickListener {
            playCountViewModel.chartOption = PlayCountChartViewModel.LAST_YEAR
            updateChart(PlayCountChartViewModel.LAST_YEAR)
            playCountViewModel.getOrderedList()?.let {
                adapter?.updateData(getRank(it), PlayCountChartViewModel.LAST_YEAR)
            }
            binding.chartOption.text = "Last Year"
            popupWindow.dismiss()
            modifySpecificYearButton.visibility =  View.GONE
        }

        popupViewBinding.specificYearLayout.setOnClickListener {
            playCountViewModel.chartOption = PlayCountChartViewModel.SPECIFIC_YEAR
            updateChart(PlayCountChartViewModel.SPECIFIC_YEAR)
            binding.chartOption.text = playCountViewModel.specificYear.toString()
            playCountViewModel.getOrderedList()?.let {
                adapter?.updateData(getRank(it), PlayCountChartViewModel.SPECIFIC_YEAR)
            }
            popupWindow.dismiss()
            modifySpecificYearButton.visibility = View.VISIBLE
        }

        popupViewBinding.specificYearText.text = playCountViewModel.specificYear.toString()


        binding.periodSelectionTab.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val location = IntArray(2)
        binding.periodSelectionTab.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]

        popupWindow.showAtLocation(
            binding.periodSelectionTab,
            Gravity.NO_GRAVITY,
            anchorX,
            anchorY + 64
        )




    }

    private fun loadFavorites() {
        val repository = FavoriteSongRepository(context)
        playCountViewModel.viewModelScope.launch(Dispatchers.IO) {
            val rawList = repository.allFavoriteTracksWithPlayCount
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                if (!isAdded) return@launch


                playCountViewModel.rawList = rawList
                val lastMonthRanking = filterAndSortByOption(today.minusMonths(1))
                playCountViewModel.top10LastMonth = lastMonthRanking
                lastMonthRanking?.let {
                    adapter?.updateData(getRank(it), PlayCountChartViewModel.LAST_MONTH)
                    updateChart(PlayCountChartViewModel.LAST_MONTH)
                }
                val last3monthsRanking = filterAndSortByOption(today.minusMonths(3))
                val lastYearRanking = filterAndSortByOption(today.minusYears(1))
                playCountViewModel.top10Last3Month = last3monthsRanking
                playCountViewModel.top10LastYear = lastYearRanking
            }
        }
    }



    private fun getRank(orderedList: List<Favorite>): List<Pair<Favorite, Int>> {
        val startDate = playCountViewModel.getStartDate()
        val endDate = playCountViewModel.getEndDate()

        // 1. 총 재생 수 계산
        val totalPlayCounts = orderedList.map { favorite ->
            val total = favorite.playCountByDay
                .filterKeys { it in startDate..endDate }
                .values
                .sum()
            favorite to total
        }

        // 2. 내림차순 정렬
        val sorted = totalPlayCounts
            .withIndex()
            .sortedByDescending { it.value.second }

        // 3. 경쟁 순위 부여: 1, 2, 2, 4, 4, 6 ...
        val ranks = MutableList(orderedList.size) { 0 }
        var currentRank = 1
        var lastScore: Int? = null
        var actualPosition = 1

        for ((i, indexed) in sorted.withIndex()) {
            val currentScore = indexed.value.second
            if (currentScore != lastScore) {
                currentRank = actualPosition
                lastScore = currentScore
            }
            ranks[indexed.index] = currentRank
            actualPosition += 1
        }

        // 4. 정렬되기 전 원래 순서로 매핑
        return totalPlayCounts.mapIndexed { i, (favorite, _) ->
            favorite to ranks[i]
        }
    }





    private fun filterAndSortByOption(startDate: LocalDate): List<Favorite>? =
        playCountViewModel.rawList
            ?.asSequence()
            // 1) startDate 이전 재생 기록은 제외, total 계산
            ?.mapNotNull { item ->
                item.lastPlayedDate
                    ?.takeIf { it >= startDate }
                    ?.let {
                        val total = item.playCountByDay
                            .filterKeys { it >= startDate }
                            .values
                            .sum()
                        item to total
                    }
            }
            // 2) total==0 항목 제외
            ?.filter { it.second > 0 }
            // 3) 1차 total 내림, 2차 lastPlayedDate 내림
            ?.sortedWith(
                compareByDescending<Pair<Favorite, Int>> { it.second }
                    .thenByDescending { it.first.lastPlayedDate }
            )
            // 4) Favorite 객체만 꺼내고
            ?.map { it.first }
            // 5) 최대 10개만
            ?.take(10)
            // 6) 결과물 로깅 & 리스트로
            ?.toList()
            ?.also { Log.d(TAG, "filtered/sorted(top10) = $it") }



    val customColors = listOf(
        Color.parseColor("#446CCF"), // 진한 파랑
        Color.parseColor("#4ECDC4"), //청록
        Color.parseColor("#E0CFB3"), // 짙은 파랑
        Color.parseColor("#FF9F1C"),  // 오렌지
        Color.parseColor("#FF6B6B") // 빨강
    )


    private fun updateChart(rangeType: Int) {
        val chart = binding.lineChart
        val topFavorites = playCountViewModel.getOrderedList()?.take(5) ?: return

        val formatter = when (rangeType) {
            PlayCountChartViewModel.LAST_YEAR -> DateTimeFormatter.ofPattern("yyyy-MM")
            PlayCountChartViewModel.SPECIFIC_YEAR -> DateTimeFormatter.ofPattern("yyyy-MM")
            PlayCountChartViewModel.LAST_3_MONTH -> DateTimeFormatter.ofPattern("MM/dd")
            else -> DateTimeFormatter.ofPattern("MM/dd")
        }

        val startDate = when (rangeType) {
            PlayCountChartViewModel.LAST_YEAR -> today.minusYears(1).withDayOfMonth(1)
            PlayCountChartViewModel.SPECIFIC_YEAR -> LocalDate.of(playCountViewModel.specificYear, 1, 1)
            PlayCountChartViewModel.LAST_3_MONTH -> today.minusMonths(3).with(java.time.DayOfWeek.MONDAY)
            else -> today.minusMonths(1)
        }

        // ✅ 1. X축 키 생성
        val groupKeys: List<LocalDate> = when (rangeType) {
            PlayCountChartViewModel.LAST_YEAR -> (0..12).map { startDate.plusMonths(it.toLong()) }
            PlayCountChartViewModel.SPECIFIC_YEAR -> (0..11).map { startDate.plusMonths(it.toLong()) }
            PlayCountChartViewModel.LAST_3_MONTH -> {
                val days = if (startDate.plusWeeks(12)  >= today) 12 else 13
                (0..days).map { startDate.plusWeeks(it.toLong()) }
            }
            else -> {
                val days = if (startDate.plusDays(30) == today) 30 else 31
                (0..days).map { startDate.plusDays(it.toLong()) }
            }

        }

        //val keyToIndex = groupKeys.mapIndexed { idx, key -> key to idx }.toMap()

        val dataSets = mutableListOf<ILineDataSet>()

        topFavorites.forEachIndexed { i, favorite ->
            // 2. 각 Favorite의 날짜를 그룹으로 집계
            val groupedMap = mutableMapOf<LocalDate, Int>()

            favorite.playCountByDay
                .filterKeys { it >= startDate }
                .forEach { (date, count) ->
                    val groupKey = when (rangeType) {
                        PlayCountChartViewModel.LAST_YEAR -> date.withDayOfMonth(1)
                        PlayCountChartViewModel.SPECIFIC_YEAR -> date.withDayOfMonth(1)
                        PlayCountChartViewModel.LAST_3_MONTH -> date.with(DayOfWeek.MONDAY) // ✅ 깔끔하게 주 시작일로 정렬
                        else -> date
                    }
                    groupedMap[groupKey] = groupedMap.getOrDefault(groupKey, 0) + count
                }

            // 3. Entry 생성 (누락된 그룹은 0으로)
            val entries = groupKeys.mapIndexed { idx, key ->
                val y = groupedMap.getOrDefault(key, 0).toFloat()
                Entry(idx.toFloat(), y)
            }

            val dataSet = LineDataSet(entries, favorite.title).apply {
                color = customColors[i % customColors.size]
                lineWidth = 2.5f
                circleRadius = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                setDrawCircles(false)
            }

            dataSet.setCircleColor(dataSet.color)




            dataSets.add(dataSet)
        }

        val lineData = LineData(dataSets)
        chart.data = lineData

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return groupKeys.getOrNull(index)?.format(formatter) ?: ""
            }
        }

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.labelRotationAngle = -45f

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.invalidate()

        val legend = chart.legend
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        legend.textColor = textPrimary
        chart.axisLeft.apply {
            textColor = textPrimary
        }
        chart.xAxis.textColor = textPrimary
        chart.setExtraOffsets(0f, 72f, 0f, 20f)

    }

    private fun showPopupDetails(anchorView: View, item: Favorite, position: Int) {
        //if (playCountViewModel.reenterStateFromMusicInfoFragment || playCountViewModel.reenterStateFromAlbumInfoFragment || playCountViewModel.reenterStateFromArtistInfoFragment)
        // return

        this.anchorView = anchorView

        playCountViewModel.onFocused = true
        playCountViewModel.focusedPosition = position
        playCountViewModel.focusedTrack = item

        val ctx = anchorView.context

        val popupView =
            LayoutInflater.from(ctx).inflate(R.layout.popup_favorite_detail_pager, null)

        // 왼쪽에 tailHeight만큼 padding 추가
        popupView.setPadding(dpToPx(ctx, 4), 0, 0, 0)

        val viewPager = popupView.findViewById<ViewPager2>(R.id.popup_view_pager)
        viewPager.adapter =
            PopupPagerAdapter(item, object : PopupPagerAdapter.OnClickEventListener {
                override fun onTitleClick(item: Favorite) {
                    this@PlayTimeChart.onTitleClick(item)
                }

                override fun onAlbumNameClick(item: Favorite) {
                    this@PlayTimeChart.onAlbumNameClick(item)
                }

                override fun onArtistNameClick(item: Favorite) {
                    this@PlayTimeChart.onArtistNameClick(item)
                }

                override fun onPlayButtonClick(item: Favorite) {
                    if (item.audioUri == null) {
                        Log.d(TAG, "audio not found, skipping play")
                        return
                    }
                    val playlist = playCountViewModel.getOrderedList()?.filter { !it.audioUri.isNullOrEmpty() }

                    playlist?.let {
                        val ordered = (it.subList(position, it.size) +
                                it.subList(0, position)).toMutableList()
                        mainActivityViewModel.setPlaylist(ordered, 0)
                    }


                }
            })

        val indicator =
            popupView.findViewById<me.relex.circleindicator.CircleIndicator3>(R.id.page_indicator)
        indicator.setViewPager(viewPager)

        //val layoutWidth = (220 * context?.resources?.displayMetrics!!.density).toInt()

        val layoutWidth = if (isTablet(ctx)) {
            dpToPx(ctx, 235)   // 태블릿
        } else {
            dpToPx(ctx, 195)   // 폰
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


        val minHeight = if (isTablet(ctx)) dpToPx(ctx, 389) else dpToPx(ctx, 280)
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
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        Log.d(TAG, "x: " + x + " y: " + y + " screen height " + screenHeight)

        val alphaDx = if (screenHeight - y < maxOf(minHeight, contentHeight)) (y + maxOf(
            minHeight,
            contentHeight
        ) - screenHeight).toFloat() else 0f
        val adjustedDx = if (isTablet(ctx)) 53.5f + alphaDx else 40f + alphaDx

        Log.d(TAG, "alpha dx: " + alphaDx)


        val bubbleDrawable = BubbleDrawable(
            ctx,
            bubbleColor = Color.parseColor("#333333"),
            cornerRadius = 24f,
            tailPosition = BubbleDrawable.TailPosition.LEFT_TOP,
            dx = adjustedDx
        )


        popupView.background = bubbleDrawable

        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            x + anchorView.width,
            y + anchorView.height * 0
        )


        preventUnfocus = false

        popupWindow.setOnDismissListener {
            if (!preventUnfocus) {
                Handler(Looper.getMainLooper()).postDelayed({
                    playCountViewModel.onFocused = false
                    //viewModel.focusedPage = 0
                }, 0)
            }

        }



    }

    private fun onArtistNameClick(item: Favorite) {
        popupWindow.dismiss()
        viewGroupContext?.let {
            FavoriteArtistLoader.loadFavoriteArtistById(it, item.track.artistId, object: FavoriteArtistLoader.Companion.OnLoadListener {
                override fun onLoadSuccess(fav: FavoriteArtist) {
                    val args = Bundle().apply {
                        putParcelable(ArtistInfoFragment.ARGUMENTS_KEY, fav)
                    }
                    playCountViewModel.reenterStateFromArtistInfoFragment = true
                    navController?.navigate(R.id.artist_info, args)
                }

                override fun onLoadFailed() {
                    apiHelper.getArtist(null, item.track.artistId) { artist ->
                        val artist = FavoriteArtist(artist)
                        val args = Bundle().apply {
                            putParcelable(ArtistInfoFragment.ARGUMENTS_KEY, artist)
                        }
                        playCountViewModel.reenterStateFromArtistInfoFragment = true
                        navController?.navigate(R.id.artist_info, args)
                    }
                }

            })
        }


    }

    private fun onAlbumNameClick(item: Favorite) {
        popupWindow.dismiss()
        val track = item.track
        apiHelper.getAlbum(null, track.albumId) {album ->
            if (album != null) {
                val args = Bundle().apply {
                    putString(AlbumInfoFragment.TRANSITION_NAME_KEY, anchorView.transitionName)
                    putParcelable(AlbumInfoFragment.ARGUMENTS_KEY, album)
                }
                playCountViewModel.transitionName = anchorView.transitionName
                val extras = FragmentNavigatorExtras(
                    anchorView to anchorView.transitionName
                )

                val currentDestination = navController?.currentDestination
                if (currentDestination?.id == R.id.fragment_play_count_chart) {
                    playCountViewModel.reenterStateFromAlbumInfoFragment = true
                    navController?.navigate(R.id.album_info, args, null, extras)
                }
            }
        }
    }

    private fun onTitleClick(item: Favorite) {
        playCountViewModel.reenterStateFromMusicInfoFragment = true
        popupWindow.dismiss()
        val args = Bundle()
            .apply {
                putString(MusicInfoFragment.TRANSITION_NAME_KEY, anchorView.transitionName)
                putParcelable(MusicInfoFragment.ARGUMENTS_KEY, item)
            }
        playCountViewModel.transitionName = anchorView.transitionName
        val extras = FragmentNavigatorExtras(
            anchorView to anchorView.transitionName
        )
        navController?.navigate(R.id.musicInfoFragment, args, null, extras)
    }

    private fun showPopupWindowAgain() {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(playCountViewModel.focusedPosition)
        if (viewHolder != null) {
            showPopupDetails(
                viewHolder.itemView,
                playCountViewModel.focusedTrack!!,
                playCountViewModel.focusedPosition
            )
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
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        const val TAG = "PlayTimeChart"
    }
}
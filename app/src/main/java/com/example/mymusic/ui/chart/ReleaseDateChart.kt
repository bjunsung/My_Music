package com.example.mymusic.ui.chart

import android.content.res.Resources
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.example.mymusic.ui.musicInfo.MusicInfoFragment
import com.example.mymusic.util.DateUtils
import com.github.mikephil.charting.components.Legend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private val adapter: ReleaseDateChartAdapter = ReleaseDateChartAdapter(listOf(), object: ReleaseDateChartAdapter.OnItemEventListener {
        override fun onItemClick(view: View, item: Favorite, position: Int) {
            showPopupWindow(view, item, position)
        }

        override fun onImageReady() {
            startPostponedEnterTransition()
        }

        override fun onImageLoadFailed() {}

    })

    private fun showPopupWindow(anchorView: View, item: Favorite, position: Int) {
        viewModel.onFocused = true
        viewModel.focusedPosition = position
        viewModel.focusedTrack = item

        val popupView = layoutInflater.inflate(R.layout.popup_favorite_detail, null)
        val titleView = popupView.findViewById<TextView>(R.id.track_title)

        if (item.metadata!= null && item.metadata.title != null && item.metadata.title.isNotEmpty()){
            titleView.text = item.metadata.title
        }
        else{
            titleView.text = item.title
        }

        val artistNameTextView = popupView.findViewById<TextView>(R.id.artist_name)
        artistNameTextView.text = item.artistName
        val albumNameTextView = popupView.findViewById<TextView>(R.id.album_name)
        albumNameTextView.text = item.track.albumName
        val releaseDateTextView = popupView.findViewById<TextView>(R.id.release_date)
        releaseDateTextView.text = item.releaseDate
        val durationTextView = popupView.findViewById<TextView>(R.id.duration_ms)
        val durationSec = (item.duration.toDouble() / 1000).toInt()
        durationTextView.text = "${durationSec / 60}분 ${durationSec % 60}초"



        val daysBetween = popupView.findViewById<TextView>(R.id.days_between)
        val vocalistsLayout = popupView.findViewById<LinearLayout>(R.id.vocalists_layout)
        val lyricistsLayout = popupView.findViewById<LinearLayout>(R.id.lyricists_layout)
        val composersLayout = popupView.findViewById<LinearLayout>(R.id.composers_layout)

        val vocalistsTextView = popupView.findViewById<TextView>(R.id.vocalists)
        val lyricistsTextView = popupView.findViewById<TextView>(R.id.lyricists)
        val composersTextView = popupView.findViewById<TextView>(R.id.composers)
        val addedDateTextView = popupView.findViewById<TextView>(R.id.added_date)

        daysBetween.text = DateUtils.calculateDateDiffrence(item.releaseDate, LocalDate.now().toString()).toString()
        addedDateTextView.text = item.addedDate

        if (item.metadata != null){
            val metadata = item.metadata
            if (!metadata.vocalists.isNullOrEmpty()) {
                vocalistsLayout.visibility = View.VISIBLE
                vocalistsTextView.text = metadata.vocalistsToString()
            }
            if (!metadata.lyricists.isNullOrEmpty()) {
                lyricistsLayout.visibility = View.VISIBLE
                lyricistsTextView.text = metadata.lyricists.joinToString(", ")
            }
            if (!metadata.composers.isNullOrEmpty()) {
                composersLayout.visibility = View.VISIBLE
                composersTextView.text = metadata.composers.joinToString(", ")
            }
        }

        val layoutWidth = 480
        val popupWindow = PopupWindow(
            popupView,
            layoutWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        Log.d(TAG, "x: " + x + " y: " + y + " screenWidth " + screenWidth)



        val bubbleDrawable = if (screenWidth - x < layoutWidth) {
            BubbleDrawable(
                bubbleColor = Color.parseColor("#333333"),
                cornerRadius = 24f,
                tailPosition = BubbleDrawable.TailPosition.RIGHT,
                dx = (screenWidth - x - 60f)
            )
        } else{
             BubbleDrawable(
                 bubbleColor = Color.parseColor("#333333"),
                 cornerRadius = 24f,
                 tailPosition = BubbleDrawable.TailPosition.LEFT,
                 dx = 60f
            )
        }


        popupView.background = bubbleDrawable




        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y + anchorView.height)
        popupWindow.isOutsideTouchable = true

        var preventUnfocus = false

        titleView.setOnClickListener {
            preventUnfocus = true
            popupWindow.dismiss()
            val args = Bundle()
            args.putString(MusicInfoFragment.TRANSITION_NAME_KEY, anchorView.transitionName)
            args.putParcelable(MusicInfoFragment.ARGUMENTS_KEY, item)
            val extras = FragmentNavigatorExtras(
                anchorView to anchorView.transitionName
            )

            findNavController().navigate(R.id.action_releaseDateChart_to_musicInfoFragment, args, null, extras)
        }

        popupWindow.setOnDismissListener {
            if (!preventUnfocus)
                viewModel.onFocused = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
    : View {
        _binding = FragmentReleaseDateChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setGradientColor()
        bind()
        prepareChart()
    }

    private fun bind(){
        lineChart = binding.lineChart

    }


    private var gradientColor = intArrayOf()
    private lateinit var recyclerView: RecyclerView

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

        lineChart.setExtraOffsets(0f, 0f, 0f, 24f)

        val legend = lineChart.legend
        legend.textColor = textColor
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.textSize = 18f
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
        lineChart.animateY(800)

        recyclerView = binding.favoriteRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

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
                    var x = e?.x!!.toInt()
                    var yearSelected = yearLabels.get(x)
                    var favoriteCount = e.y.toInt().toString()
                    val yearSelectedTextView = binding.xValue
                    val favoriteCountTextView = binding.yValue
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

    private fun restoreData() {
        recyclerView.scrollToPosition(viewModel.focusedPosition)
        val shuffled = viewModel.shuffledList
        if (shuffled.isNotEmpty()){
            adapter.updateList(shuffled)
            setSeeMoreAndFoldButton(shuffled)
        }

        if (viewModel.seeMoreState) {
            adapter.updateVisibleCount(shuffled.size)
        }

        if (viewModel.onFocused) {
            recyclerView.post {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(viewModel.focusedPosition)
                if (viewHolder != null) {
                    showPopupWindow(
                        viewHolder.itemView,
                        viewModel.focusedTrack,
                        viewModel.focusedPosition
                    )
                }
            }
        }
        else{
            startPostponedEnterTransition()
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
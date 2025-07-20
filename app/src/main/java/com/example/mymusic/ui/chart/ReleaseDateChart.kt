package com.example.mymusic.ui.chart

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.mymusic.R
import com.example.mymusic.databinding.FragmentReleaseDateChartBinding
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.myCalendar.ReleaseDateViewModel
import com.example.mymusic.util.SortFilterUtil
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import kotlin.properties.Delegates

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
    : View {
        _binding = FragmentReleaseDateChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        bind()
        prepareChart()
    }

    private fun bind(){
        lineChart = binding.lineChart
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
        val dataSet =
            com.github.mikephil.charting.data.LineDataSet(entries, "노래")

        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.color = Color.parseColor("#1565C0")
        dataSet.valueTextColor = android.graphics.Color.BLACK
        dataSet.valueTextSize = 13.5f

        dataSet.lineWidth = 3f
        dataSet.circleRadius = 2f
        dataSet.setCircleColor(android.graphics.Color.GRAY)

        dataSet.setDrawCircles(false)   // 점 숨김 (원하는 경우)
        dataSet.setDrawValues(false)    // 값 숨김
        dataSet.setDrawFilled(true)     // 채우기 활성화
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, // 방향
            intArrayOf(
                Color.parseColor("#1565C0"), // 시작 색상
                Color.parseColor("#E3F2FD")  // 끝 색상 (투명)
            )
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

        lineChart.setScaleYEnabled(false) // 세로 줌 비활성화
        lineChart.setDragEnabled(true) //가로 줌 활성화
        lineChart.setScaleXEnabled(true)

        //value 정수로
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.y?.toInt().toString()
            }
        }


        val axisLeft = lineChart.axisLeft
        axisLeft.setDrawGridLines(false)


        // 오른쪽 Y축 제거
        lineChart.axisRight.isEnabled = false

        // 설명 제거
        lineChart.description.isEnabled = false

        // 애니메이션
        lineChart.animateY(800)
        val artworkCardView1: MaterialCardView = binding.artworkCardview1
        val artworkCardView2: MaterialCardView = binding.artworkCardview2
        val artworkCardView3: MaterialCardView = binding.artworkCardview3

        val artwork1: ImageView = binding.artworkImage1
        val artwork2: ImageView = binding.artworkImage2
        val artwork3: ImageView = binding.artworkImage3

        val seeMoreButton: TextView? = binding.seeMoreButton

        artworkCardView1.visibility = View.GONE
        artworkCardView2.visibility = View.GONE
        artworkCardView3.visibility = View.GONE
        seeMoreButton?.visibility = View.GONE


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
                    var favoriteCount = e.y?.toInt().toString()
                    val yearSelectedTextView = binding.xValue
                    val favoriteCountTextView = binding.yValue
                    yearSelectedTextView.text = yearSelected
                    favoriteCountTextView.text = favoriteCount

                    val favListForYear: List<Favorite> = favListForYearMap[yearSelected.toInt()] ?: emptyList()

                    val selectedList: List<Favorite> = if (favListForYear.size > 3) {
                        seeMoreButton?.visibility = View.VISIBLE
                        favListForYear.shuffled().take(3) // 랜덤 3개
                    } else {
                        seeMoreButton?.visibility = View.GONE
                        favListForYear // 3개 이하이면 그대로
                    }

// 이미지와 카드뷰 배열로 관리
                    val artworkCardViews = listOf(artworkCardView1, artworkCardView2, artworkCardView3)
                    val artworkImages = listOf(artwork1, artwork2, artwork3)

                    for (i in artworkCardViews.indices) {
                        val fav = selectedList.getOrNull(i)
                        if (fav != null && !fav.track.artworkUrl.isNullOrEmpty()) {

                            Log.d(TAG, "url: " + fav.track.artworkUrl)

                            artworkCardViews[i].visibility = View.VISIBLE
                            Glide.with(artworkImages[i].context)
                                .load(fav.track.artworkUrl)
                                .override(144, 144)
                                .into(artworkImages[i])
                        } else {
                            artworkCardViews[i].visibility = View.GONE
                        }
                    }
                }

                override fun onNothingSelected() {
                    artworkCardView1.visibility = View.GONE
                    artworkCardView2.visibility = View.GONE
                    artworkCardView3.visibility = View.GONE
                    seeMoreButton?.visibility = View.GONE
                }

            })
        }


        lineChart.invalidate()
    }

    companion object{
        private const val TAG = "ReleaseDateChart"
    }

}
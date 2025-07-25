package com.example.mymusic.ui.home.recommend

import android.util.Log
import com.example.mymusic.model.Favorite
import com.example.mymusic.ui.home.HomeViewModel
import java.time.LocalDate
import kotlin.math.max

data class RecommendationResult(
    val hotNowTop5: List<Favorite>,   // 최근 1개월 최다
    val comebackTop5: List<Favorite>  // 감소 기준 최종 5곡
)
val TAG = "Recommendation"

private fun Int.coerceNonNegative(): Int = if (this < 0) 0 else this

private fun countInRange(item: Favorite, startInclusive: LocalDate, endExclusive: LocalDate): Int {
    return item.playCountByDay
        .asSequence()
        .filter { (d, _) -> d >= (startInclusive) && d.isBefore(endExclusive)   }
        .sumOf { it.value }
}

fun recommendFavorites(
    rawList: List<Favorite>?,
    today: LocalDate = LocalDate.now(),
    minDropThreshold: Int = 5,   // 절대 감소량 기준 최소치
    homeViewModel: HomeViewModel
): RecommendationResult {
    if (rawList.isNullOrEmpty()) return RecommendationResult(emptyList(), emptyList())

    // 공통: 최근 1개월 Top5
    val endExclusive = today.plusDays(1)
    val lastMonthStart = today.minusMonths(1)
    val hotNowTop5 = rawList.asSequence()
        .map { it to countInRange(it, lastMonthStart, endExclusive) }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(5)
        .toList()

    homeViewModel.maxCountDiff = 0
    // 감소 지표
    data class DropMetrics(
        val item: Favorite,
        val prev: Int,
        val recent: Int,
        val absDrop: Int
    )

    fun computeDrops(
        prevStart: LocalDate,
        prevEndExclusive: LocalDate,
        recentStart: LocalDate,
        recentEndExclusive: LocalDate,
        minDrop: Int
    ): Pair<List<Favorite>, Map<Favorite, DropMetrics>> {
        val drops = rawList.asSequence()
            .map { item ->
                val prev = countInRange(item, prevStart, prevEndExclusive)
                val recent = countInRange(item, recentStart, recentEndExclusive)
                val absDrop = (prev - recent).coerceNonNegative()
                if (absDrop > 3) {
                    Log.d(TAG, "title: ${item.title} prev: $prev recent: $recent abs: $absDrop")
                }
                DropMetrics(item, prev, recent, absDrop)
            }
            .toList()

        val absTop = drops.asSequence()
            .filter { it.absDrop >= minDrop }
            .sortedByDescending { it.absDrop }
            .map { it.item }
            .distinct()
            .take(20)
            .toList()

        val metricsByItem = drops.associateBy { it.item }
        return Pair(absTop, metricsByItem)
    }

    // 시작은 항상 1년 전 고정
    val prevStart = today.minusYears(1)

    // 3개월 고정 대신 주 단위로 split을 점점 지금에 가깝게 당김
    val splitWeeksSeq = listOf(12, 10, 8, 6, 4, 3, 2, 1)

    var unionCandidates: List<Favorite> = emptyList()
    var metricsByItem: Map<Favorite, DropMetrics> = emptyMap()

    for (w in splitWeeksSeq) {
        val split = today.minusWeeks(w.toLong()).plusDays(1)  // 기준일: w주 전
        Log.d(TAG, "split: $split")
        val (absTop, metrics) = computeDrops(
            prevStart = prevStart,
            prevEndExclusive = split,      // [1Y, split)
            recentStart = split,           // [split, now]
            recentEndExclusive = endExclusive,
            minDrop = minDropThreshold
        )

        // HomeViewModel에 단계 기록
        homeViewModel.dropWindowPresetWeek = w

        if (absTop.isNotEmpty()) {
            unionCandidates = absTop
            metricsByItem = metrics
            break
        }
    }

    if (unionCandidates.isEmpty()) {
        return RecommendationResult(hotNowTop5 = hotNowTop5, comebackTop5 = emptyList())
    }

    // 스코어링 (absDrop 기준만 사용)
    val maxAbs = unionCandidates.maxOfOrNull { metricsByItem[it]?.absDrop ?: 0 }?.takeIf { it > 0 } ?: 1
    homeViewModel.maxCountDiff = maxAbs

    val comebackTop5 = unionCandidates.asSequence()
        .map { item ->
            val m = metricsByItem[item]!!
            val normAbs = m.absDrop.toDouble() / maxAbs
            val score = normAbs
            Triple(item, score, m)
        }
        .sortedWith(
            compareByDescending<Triple<Favorite, Double, DropMetrics>> { it.second }
                .thenBy { it.third.recent }                // 최근 적을수록 복귀 여지 ↑
                .thenByDescending { it.first.lastPlayedDate }
        )
        .map { it.first }
        .take(5)
        .toList()



    return RecommendationResult(
        hotNowTop5 = hotNowTop5,
        comebackTop5 = comebackTop5
    )

}

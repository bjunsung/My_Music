// MyMicrobenchmark.java (수정된 예시)

package com.example.benchmark; // build.gradle.kts의 namespace에 맞춰 수정하세요.

import android.util.Log; // Log 사용 시 필요
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.benchmark.BenchmarkState; // BenchmarkState 클래스 임포트
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Benchmark, which will execute on an Android device.
 * <p>
 * The while loop will measure the contents of the loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4.class)
public class MyMicrobenchmark {

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule(); // mBenchmarkRule 대신 benchmarkRule로 통일

    @Test
    public void benchmarkListProcessing() {
        // 벤치마크 대상이 아닌 초기화 코드는 루프 밖에 둡니다.
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            data.add(i);
        }

        // BenchmarkState를 사용하여 측정 루프를 직접 제어합니다.
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            // 이 루프 내의 코드가 벤치마크 측정 대상이 됩니다.
            // 리스트의 모든 요소를 2배로 만들고 새로운 리스트를 생성합니다.
            data.stream()
                    .map(n -> n * 2)
                    .collect(Collectors.toList());
        }
    }

    @Test
    public void benchmarkSimpleLoop() {
        // BenchmarkState를 사용하여 측정 루프를 직접 제어합니다.
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            // 이 루프 내의 코드가 벤치마크 측정 대상이 됩니다.
            long sum = 0;
            for (int i = 0; i < 100000; i++) {
                sum += i;
            }
        }
    }

    @Test
    public void benchmarkLogMethod() {
        // BenchmarkState를 사용하여 측정 루프를 직접 제어합니다.
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            // Log.d 메서드 호출 비용을 측정합니다.
            Log.d("LogBenchmark", "the cost of writing this log method will be measured");
        }
    }
}
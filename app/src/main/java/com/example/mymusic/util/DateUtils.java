package com.example.mymusic.util;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    public static String today(){
        // 오늘 날짜 구하기
        LocalDate today = LocalDate.now();

        // 원하는 포맷 정의
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return today.format(formatter);

    }
    public static long calculateDateDiffrence(String dateStr1, String dateStr2){
        if (dateStr1.length() < 10)
            throw new DateFormatMismatchException("format requires: yyyy-MM-dd, but received: " + dateStr1);
        // 1. 날짜 포맷 정의 (yyyy-MM-dd 형식)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 2. 문자열을 LocalDate로 파싱
        LocalDate date1 = LocalDate.parse(dateStr1, formatter);
        LocalDate date2 = LocalDate.parse(dateStr2, formatter);

        // 3. 일수 차이 계산
        return ChronoUnit.DAYS.between(date1, date2);
    }


}

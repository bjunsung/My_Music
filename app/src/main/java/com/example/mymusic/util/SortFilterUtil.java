package com.example.mymusic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SortFilterUtil {
    private final static String TAG = "SortFilterUtil";
    private static Context context;
    public static final String sort_RELEASE_DATE = "RELEASE_DATE";
    public static final String sort_ARTIST_NAME =  "ARTIST_NAME";
    public static final String filter_ALL = "ALL";

    public static List<Favorite> sortAndFilterFavoritesList(Context context, List<Favorite> originalList, Track track) {
        SortFilterUtil.context = context;
        if (context == null || originalList == null) return originalList;

        SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
        String sort = prefs.getString("sort_option", "ADDED_DATE");
        String filter = prefs.getString("filter_option", "ALL");
        boolean isDescending = prefs.getBoolean("isDescending", false);

        List<Favorite> filteredList = filterList(originalList, filter, track);
        return sortList(filteredList, sort, isDescending);
    }

    public static List<Favorite> sortAndFilterFavoritesList(Context context, List<Favorite> originalList) {
        SortFilterUtil.context = context;
        if (context == null || originalList == null) return originalList;

        SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
        String sort = prefs.getString("sort_option", "ADDED_DATE");
        String filter = prefs.getString("filter_option", "ALL");
        boolean isDescending = prefs.getBoolean("isDescending", false);

        List<Favorite> filteredList = filterList(originalList, filter, null);
        return sortList(filteredList, sort, isDescending);
    }
    public static List<Favorite> sortAndFilterFavoritesList(Context context, List<Favorite> originalList, String filterOption, Track track, String sortOption, boolean isDescending) {
        SortFilterUtil.context = context;
        if (context == null || originalList == null) return originalList;
        if (filterOption == null) filterOption = "ALL";
        List<Favorite> filteredList = filterList(originalList, filterOption, track);
        if (sortOption == null) sortOption = "ADDED_DATE";
        return sortList(filteredList, sortOption, isDescending);
    }

    private static List<Favorite> filterList(List<Favorite> list, String filter, Track queryTrack) {
        List<Favorite> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
        LocalDate tenYearsAgo = LocalDate.now().minusYears(10);
        if (context != null) {
            LocalDate today = LocalDate.now();

            SharedPreferences prefs = context.getSharedPreferences("filter_prefs", Context.MODE_PRIVATE);
            String startDateStr = prefs.getString("start_date", today.minusYears(1).toString());
            LocalDate startDate = LocalDate.parse(startDateStr);
            String endDateStr = prefs.getString("end_date", today.toString());
            LocalDate endDate;
            if (endDateStr.equals("TODAY")){
                endDate = LocalDate.now();
            }else{
                endDate = LocalDate.parse(endDateStr);
            }
            Log.d(TAG, "received start date: " + startDateStr);
            Log.d(TAG, "received end date: " + endDateStr);


            for (Favorite item : list) {
                switch (filter) {
                    case "ALL":
                        result.add(item);
                        break;
                    case "LAST_3_MONTHS":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.track.releaseDate, formatter);
                                if (!releaseDate.isBefore(threeMonthsAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "LAST_YEAR":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.track.releaseDate, formatter);
                                if (!releaseDate.isBefore(oneYearAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "LAST_5_YEARS":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.track.releaseDate, formatter);
                                if (!releaseDate.isBefore(fiveYearsAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "LAST_10_YEARS":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.track.releaseDate, formatter);
                                if (!releaseDate.isBefore(tenYearsAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case "DECADE_2020S":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                String yearStr = item.track.releaseDate.substring(0, 4);
                                int year = Integer.parseInt(yearStr);
                                if (year >= 2020 && year <= 2029) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "DECADE_2010S":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                String yearStr = item.track.releaseDate.substring(0, 4);
                                int year = Integer.parseInt(yearStr);
                                if (year >= 2010 && year <= 2019) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "DECADE_2000S":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                String yearStr = item.track.releaseDate.substring(0, 4);
                                int year = Integer.parseInt(yearStr);
                                if (year >= 2000 && year <= 2009) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "BEFORE_2000S":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                String yearStr = item.track.releaseDate.substring(0, 4);
                                int year = Integer.parseInt(yearStr);
                                if (year <= 1999) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "SEASON_SPRING":
                        try {
                            if (item.track != null && item.track.releaseDate != null && item.track.releaseDate.length() >= 7) {
                                String monthStr = item.track.releaseDate.substring(5, 7);
                                int month = Integer.parseInt(monthStr);
                                if (month == 3 || month == 4 || month == 5) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "SEASON_SUMMER":
                        try {
                            if (item.track != null && item.track.releaseDate != null && item.track.releaseDate.length() >= 7) {
                                String monthStr = item.track.releaseDate.substring(5, 7);
                                int month = Integer.parseInt(monthStr);
                                if (month == 6 || month == 7 || month == 8) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "SEASON_AUTUMN":
                        try {
                            if (item.track != null && item.track.releaseDate != null && item.track.releaseDate.length() >= 7) {
                                String monthStr = item.track.releaseDate.substring(5, 7);
                                int month = Integer.parseInt(monthStr);
                                if (month == 9 || month == 10 || month == 11) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "SEASON_WINTER":
                        try {
                            if (item.track != null && item.track.releaseDate != null && item.track.releaseDate.length() >= 7) {
                                String monthStr = item.track.releaseDate.substring(5, 7);
                                int month = Integer.parseInt(monthStr);
                                if (month == 12 || month == 1 || month == 2) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "ON_THIS_DAY":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.track.releaseDate);
                                if (releaseDate.getMonth().equals(today.getMonth()) && releaseDate.getDayOfMonth() == today.getDayOfMonth()) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case "CUSTOM_INPUT":
                        try {
                            if (item.track != null && item.track.releaseDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.track.releaseDate);
                                if (releaseDate.isAfter(startDate.minusDays(1)) && releaseDate.isBefore(endDate.plusDays(1))) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "ARTIST":
                        if (queryTrack != null) {
                            try {
                                if (item.track != null && item.track.artistId != null && !item.track.artistId.isEmpty()) {
                                    if (item.track.artistId.equals(queryTrack.artistId) && !item.track.trackId.equals(queryTrack.trackId)) {
                                        result.add(item);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    default:
                        result.add(item);
                        break;
                }
            }
        }
        return result;
    }
    private static List<Favorite> sortList(List<Favorite> list, String sort, boolean isDescending) {
        switch (sort) {
            case "TITLE":
                Collections.sort(list, Comparator.comparing(Favorite::getTitle, String.CASE_INSENSITIVE_ORDER));
                break;
            case "ARTIST_NAME":
                Collections.sort(list, Comparator.comparing(Favorite::getArtistName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "ADDED_DATE":
                Collections.sort(list, Comparator.comparing(Favorite::getAddedDate));
                break;
            case "RELEASE_DATE":
                Collections.sort(list, Comparator.comparing(Favorite::getReleaseDate));
                break;
            case "DURATION":
                Collections.sort(list, Comparator.comparing(Favorite::getDuration));
                break;
        }
        if (isDescending){
            Log.d(TAG, "is Descending: " + isDescending);
            Collections.reverse(list);
        }else{
            Log.d(TAG, "is Descending: " + isDescending);
        }
        return list;
    }
}

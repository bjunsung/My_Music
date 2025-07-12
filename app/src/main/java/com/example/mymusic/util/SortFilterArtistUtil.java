package com.example.mymusic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.mymusic.model.Favorite;
import com.example.mymusic.model.FavoriteArtist;
import com.example.mymusic.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SortFilterArtistUtil {
    private final static String TAG = "SortFilterArtistUtil";
    private static Context context;

    public static List<FavoriteArtist> sortAndFilterFavoritesList(Context context, List<FavoriteArtist> originalList) {
        SortFilterArtistUtil.context = context;
        if (context == null || originalList == null) return originalList;

        SharedPreferences prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
        String sort = prefs.getString("sort_option", "ADDED_DATE");
        String filter = prefs.getString("filter_option", "ALL");
        boolean isDescending = prefs.getBoolean("isDescending", false);

        List<FavoriteArtist> filteredList = filterList(originalList, filter);
        return sortList(filteredList, sort, isDescending);
    }
    public static List<FavoriteArtist> sortAndFilterFavoritesList(Context context, List<FavoriteArtist> originalList, String filterOption, String sortOption, boolean isDescending) {
        SortFilterArtistUtil.context = context;
        if (context == null || originalList == null) return originalList;
        if (filterOption == null) filterOption = "ALL";
        List<FavoriteArtist> filteredList = filterList(originalList, filterOption);
        if (sortOption == null) sortOption = "ADDED_DATE";
        return sortList(filteredList, sortOption, isDescending);
    }

    private static List<FavoriteArtist> filterList(List<FavoriteArtist> list, String filter) {
        Log.d(TAG, "received artist list, size: " +  list.size());
        Log.d(TAG, "received filter method: " + filter);
        List<FavoriteArtist> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
        LocalDate tenYearsAgo = LocalDate.now().minusYears(10);
        if (context != null) {
            LocalDate today = LocalDate.now();

            SharedPreferences prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
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


            for (FavoriteArtist item : list) {
                switch (filter) {
                    case "ALL":
                        result.add(item);
                        break;
                    case "LAST_5_YEARS":
                        try {
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                LocalDate debutDate = LocalDate.parse(item.getDebutDate(), formatter);
                                if (debutDate.isAfter(fiveYearsAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "LAST_10_YEARS":
                        try {
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                LocalDate debutDate = LocalDate.parse(item.getDebutDate(), formatter);
                                if (debutDate.isAfter(tenYearsAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "OVER_10_YEARS":
                        try {
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                LocalDate debutDate = LocalDate.parse(item.getDebutDate(), formatter);
                                if (debutDate.isBefore(tenYearsAgo)) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case "DECADE_2020S":
                        try {
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String yearStr = item.getDebutDate().substring(0, 4);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String yearStr = item.getDebutDate().substring(0, 4);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String yearStr = item.getDebutDate().substring(0, 4);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String yearStr = item.getDebutDate().substring(0, 4);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String monthStr = item.getDebutDate().substring(5, 7);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String monthStr = item.getDebutDate().substring(5, 7);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String monthStr = item.getDebutDate().substring(5, 7);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                String monthStr = item.getDebutDate().substring(5, 7);
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
                            if (item.metadata != null && item.metadata.debutDate != null) {
                                LocalDate releaseDate = LocalDate.parse(item.getDebutDate());
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
                            if (item.metadata != null && item.getDebutDate() != null) {
                                LocalDate releaseDate = LocalDate.parse(item.getDebutDate());
                                if (releaseDate.isAfter(startDate.minusDays(1)) && releaseDate.isBefore(endDate.plusDays(1))) {
                                    result.add(item);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "MEMBER_COUNTS":
                        try{
                            if (item.metadata != null && item.metadata.members != null && !item.metadata.members.isEmpty()){
                                result.add(item);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        result.add(item);
                        break;
                }
            }
        }
        return result;
    }
    private static List<FavoriteArtist> sortList(List<FavoriteArtist> list, String sort, boolean isDescending) {
        Log.d(TAG, "received artist list, size: " +  list.size());
        Log.d(TAG, "received sort method: " + sort);
        switch (sort) {
            case "ADDED_DATE":
                Collections.sort(list, Comparator.comparing(FavoriteArtist::getAddedDate, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "DEBUT_DATE":
                Collections.sort(list, Comparator.comparing(FavoriteArtist::getDebutDate, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "FOLLOWERS":
                Collections.sort(list, Comparator.comparing(FavoriteArtist::getFollowers));
                break;
            case "MEMBER_COUNTS":
                list = filterList(list, "MEMBER_COUNTS");
                Collections.sort(list, Comparator.comparing(FavoriteArtist::getMemberCount));
                break;
            case "ARTIST_NAME":
                Collections.sort(list, Comparator.comparing(FavoriteArtist::getArtistName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "IMAGE_COUNTS":
                Collections.sort(list, Comparator.comparing(FavoriteArtist::getImageCount));
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
